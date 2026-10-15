package com.bookman.apartment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The backend downloads the iCal URL, so it must not be able to point at internal hosts (SSRF, e.g. the
 * AWS metadata endpoint). REQ-04 / Q-17 (confirmed): only https URLs on Booking.com hosts are accepted.
 */
@ConfigurationProperties(prefix = "bookman.apartment")
public record ApartmentUrlPolicy(List<String> allowedHosts) {

    public ApartmentUrlPolicy {
        allowedHosts = allowedHosts == null || allowedHosts.isEmpty() ? List.of("booking.com") : List.copyOf(allowedHosts);
    }

    public Optional<URI> parseAllowed(String value) {
        try {
            URI uri = new URI(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return Optional.empty();
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            boolean allowed = allowedHosts.stream().anyMatch(h -> host.equals(h) || host.endsWith("." + h));
            return allowed ? Optional.of(uri) : Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
