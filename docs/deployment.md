# Deployment

Production setup chosen by the owner (Q-06): **free of charge**, one small server.

```
Phone (app) ──HTTPS──▶ Oracle VM: Caddy :443 ──▶ Spring Boot 127.0.0.1:8080 ──▶ PostgreSQL 16 (localhost:5432)
                                                        └──▶ Firebase Cloud Messaging
```

| Part | Choice | Cost |
|---|---|---|
| Server | Oracle Cloud *Always Free*, VM.Standard.A1.Flex (Arm), Ubuntu 24.04, region Frankfurt | 0 |
| Database | PostgreSQL 16 on the same VM, reachable only locally | 0 |
| Domain + HTTPS | DuckDNS subdomain `bookman-artur.duckdns.org`, Caddy with Let's Encrypt | 0 |
| Push | Firebase Cloud Messaging | 0 |
| App distribution | Google Play, internal testing track | USD 25 once |

Why not a "scale to zero" free tier (Render, Cloud Run, Neon, Supabase …): the backend runs schedulers
(reservation import every 30 min, reminders every minute), so it has to run all the time.

Template files are in [`/deploy`](../deploy). Secrets (database password, JWT secret, Firebase key, keystore)
never go into the repository.

## 1. Server (one-time)

### 1.1 Oracle VM
1. Compute → Instances → Create: image **Ubuntu 24.04**, shape **VM.Standard.A1.Flex** (e.g. 2 OCPU / 12 GB).
2. SSH keys: *Generate a key pair for me* → **save the private key** (it cannot be downloaded again), keep it
   outside the repository, e.g. `C:\Users\<you>\.ssh\bookman-oracle.key`.
3. Note the **public IPv4 address** of the instance (not the private `10.x` one).
4. Optional: switch the account to *Pay As You Go* so an idle Always Free VM is not reclaimed; usage within the
   Always Free limits stays free.

Connect: `ssh -i C:\Users\<you>\.ssh\bookman-oracle.key ubuntu@<public IP>`
(on "UNPROTECTED PRIVATE KEY FILE": `icacls <key> /inheritance:r` and `icacls <key> /grant:r "$($env:USERNAME):(R)"`).

### 1.2 Domain
DuckDNS: create `bookman-artur`, set **current ip** to the public IPv4 address, leave IPv6 empty.
Check: `nslookup bookman-artur.duckdns.org`.

### 1.3 Open ports 80 and 443 – in two places
1. **Oracle:** instance → Primary VNIC → Subnet → Security → *Default Security List* → Add Ingress Rules:
   source `0.0.0.0/0`, TCP, destination ports `80,443`. Do **not** open 5432 or 8080.
2. **Ubuntu iptables** (Oracle images reject everything except SSH):
   ```bash
   sudo iptables -L INPUT --line-numbers -n          # find the line number N of the REJECT rule
   sudo iptables -I INPUT N -m state --state NEW -p tcp --dport 443 -j ACCEPT
   sudo iptables -I INPUT N -m state --state NEW -p tcp --dport 80 -j ACCEPT
   sudo netfilter-persistent save
   ```
   The new rules must be **above** REJECT. Never run `iptables -F` or enable `ufw` on Oracle images.

### 1.4 Software
```bash
sudo apt update && sudo apt -y upgrade
sudo apt install -y openjdk-21-jre-headless postgresql debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install -y caddy
```
Check: `java -version` (21), `psql --version` (16), `http://bookman-artur.duckdns.org` shows the Caddy welcome page.

### 1.5 Database
```bash
openssl rand -hex 24                                   # database password – store it in a password manager
sudo -u postgres createuser --pwprompt bookman
sudo -u postgres createdb --owner=bookman bookman
psql -h localhost -U bookman -d bookman -c "select version();"
```
PostgreSQL listens on `localhost` only – keep it that way. Tables are created by Flyway on the first backend start.

### 1.6 Backend service
```bash
sudo useradd --system --home /opt/bookman --shell /usr/sbin/nologin bookman
sudo mkdir -p /opt/bookman /etc/bookman
sudo nano /etc/bookman/bookman.env        # content: deploy/bookman.env.example, filled in
```
From Windows, copy the Firebase **service-account key** (not `google-services.json`) and the unit file:
```
scp -i <key> C:\Users\<you>\secrets\bookman-fcm.json ubuntu@bookman-artur.duckdns.org:/tmp/fcm.json
scp -i <key> deploy\bookman.service deploy\Caddyfile deploy\backup.sh ubuntu@bookman-artur.duckdns.org:/tmp/
```
On the server:
```bash
sudo mv /tmp/fcm.json /etc/bookman/fcm.json
sudo mv /tmp/bookman.service /etc/systemd/system/bookman.service
sudo mv /tmp/Caddyfile /etc/caddy/Caddyfile
sudo mv /tmp/backup.sh /opt/bookman/backup.sh && sudo chmod 755 /opt/bookman/backup.sh
sudo chown -R bookman:bookman /opt/bookman /etc/bookman
sudo chmod 600 /etc/bookman/bookman.env /etc/bookman/fcm.json
sudo systemctl daemon-reload && sudo systemctl enable bookman
```
Then install the backend with [section 2](#2-backend-updates) and reload Caddy: `sudo systemctl reload caddy`.

Check: `https://bookman-artur.duckdns.org/api/users/me` returns `{"code":"UNAUTHORIZED",…}` with a valid certificate.

### 1.7 Backups
```bash
sudo mkdir -p /var/backups/bookman && sudo chown postgres:postgres /var/backups/bookman
sudo crontab -u postgres -e      # add:  15 3 * * * /opt/bookman/backup.sh
```
Daily dump at 03:15 UTC, 14 days kept. Copy them off the server from time to time:
`scp -i <key> "ubuntu@bookman-artur.duckdns.org:/var/backups/bookman/*.dump" C:\Users\<you>\secrets\`
Restore: `sudo -u postgres pg_restore --clean -d bookman <file>.dump` (stop the backend first).

## 2. Backend updates
From the repository root on Windows (latest `main`):
```
.\deploy\deploy-backend.ps1 -Key C:\Users\<you>\.ssh\bookman-oracle.key      # PowerShell
deploy\deploy-backend.cmd -Key C:\Users\<you>\.ssh\bookman-oracle.key        # cmd
```
(Started from cmd, a `.ps1` file only opens in Notepad – use the `.cmd` wrapper there.)
The script runs 5 steps and reports each one (`[n/5] … OK`):
1. checks prerequisites (repository root, SSH key, `ssh`/`scp`) and shows the deployed branch and commit, with a
   warning for uncommitted changes or a branch other than `main`,
2. builds the jar (`gradlew bootJar`),
3. uploads it,
4. installs it and restarts `bookman.service`,
5. waits (up to 120 s, `-StartupTimeoutSeconds`) until `https://bookman-artur.duckdns.org/api/users/me` answers
   `401` – the backend is up and HTTPS works.

It ends with a green `DEPLOYMENT SUCCEEDED` or a red `DEPLOYMENT FAILED: <reason>` (exit code 1); if the backend does
not come up it prints the last 30 log lines. Migrations run on start. If Windows refuses to run scripts
("running scripts is disabled"), run once `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

Useful commands on the server:

| Command | Purpose |
|---|---|
| `sudo systemctl status bookman` | running? since when? |
| `journalctl -u bookman -f` | live log (Ctrl+C stops viewing, not the service) |
| `journalctl -u bookman -n 200` | last 200 lines |
| `sudo systemctl restart bookman` | restart (also after editing `bookman.env`) |
| `journalctl -u caddy -n 50` | HTTPS / certificate problems |

A healthy start logs `Successfully applied … migrations` (or `Schema "public" is up to date`) and
`Started BookmanApplication`.

## 3. Android release

### 3.1 Configuration
- The **release** build talks to `https://bookman-artur.duckdns.org/` (default in `android/app/build.gradle.kts`,
  override with `bookmanApiUrl`). The **debug** build keeps using the local backend (`http://10.0.2.2:8080/`).
- `android/app/google-services.json` must be present, otherwise the app has no notifications.
- **Version:** before every Google Play upload raise `bookmanVersionCode` by 1 (and `bookmanVersionName` if you
  like) in `android/gradle.properties`.

### 3.2 Upload key (one-time)
Android Studio → *Build → Generate Signed App Bundle or APK* → *Create new…*, store it outside the repository,
e.g. `C:\Users\<you>\secrets\bookman-upload.jks`, alias `upload`, the **same** password for store and key (min. 6
characters). If the wizard fails, create it on the command line:
```
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore C:\Users\<you>\secrets\bookman-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
**Back up the keystore and its password** – without them no further updates can be uploaded.

Optional, for builds without the wizard: add to `C:\Users\<you>\.gradle\gradle.properties` (not the repository)
```
bookmanKeystore=C:/Users/<you>/secrets/bookman-upload.jks
bookmanKeystorePassword=<password>
```
then `cd android` and `.\gradlew bundleRelease` (Play) or `.\gradlew assembleRelease` (APK); output in
`app/build/outputs/bundle/release/` and `app/build/outputs/apk/release/`.

### 3.3 Google Play – internal testing
1. Create a developer account at https://play.google.com/console (personal, USD 25, ID and device verification –
   takes a few days).
2. Create app *Bookman* (app, free).
3. Build a signed **App Bundle** (`.aab`, release). Play App Signing stays on; your keystore is the upload key.
4. *Testing → Internal testing*: create a tester list with the cleaners' Google (Gmail) addresses → *Create new
   release* → upload the `.aab` → *Save → Review → Start rollout* → *Copy link* and send it to the testers.
5. Testers open the link on the phone → *Become a tester* → install from Google Play. Updates arrive automatically.

Uninstall a debug build before installing a release build on the same device (different signature).

**Before a public release** (not needed for internal testing, see Q-25): privacy-policy URL, in-app account
deletion with a web deletion link, store listing (icon 512×512, feature graphic 1024×500, screenshots,
descriptions), Data safety form, content rating, a demo account for the reviewers, and – for a new personal
account – a closed test with at least 12 testers for 14 days. The target SDK must be raised every year.
