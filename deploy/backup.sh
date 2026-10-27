#!/bin/sh
# Daily PostgreSQL backup, run by the postgres user's crontab (docs/deployment.md):
#   15 3 * * * /opt/bookman/backup.sh
# Keeps 14 days of dumps; copy them off the server from time to time.
set -eu
DIR=/var/backups/bookman
pg_dump -Fc bookman > "$DIR/bookman-$(date +%F).dump"
find "$DIR" -name 'bookman-*.dump' -mtime +14 -delete
