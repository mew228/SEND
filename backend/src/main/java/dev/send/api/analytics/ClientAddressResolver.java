package dev.send.api.analytics;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import javax.annotation.Nullable;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientAddressResolver {
    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (isTrustedProxy(remoteAddress) && forwardedFor != null && !forwardedFor.isBlank()) {
            int separatorIndex = forwardedFor.indexOf(',');
            return (separatorIndex >= 0 ? forwardedFor.substring(0, separatorIndex) : forwardedFor).trim();
        }

        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private boolean isTrustedProxy(@Nullable String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }

        String normalizedAddress = remoteAddress.trim();
        int ipv6ZoneSeparator = normalizedAddress.indexOf('%');
        if (ipv6ZoneSeparator >= 0) {
            normalizedAddress = normalizedAddress.substring(0, ipv6ZoneSeparator);
        }

        String lowerCaseAddress = normalizedAddress.toLowerCase(Locale.ROOT);
        if (lowerCaseAddress.startsWith("::ffff:")) {
            normalizedAddress = normalizedAddress.substring(7);
        }

        try {
            InetAddress address = InetAddress.getByName(normalizedAddress);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress()) {
                return true;
            }
            if (address instanceof Inet4Address ipv4Address) {
                byte[] octets = ipv4Address.getAddress();
                int firstOctet = octets[0] & 0xff;
                int secondOctet = octets[1] & 0xff;
                return firstOctet == 10
                        || firstOctet == 127
                        || (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31)
                        || (firstOctet == 192 && secondOctet == 168);
            }
            if (address instanceof Inet6Address ipv6Address) {
                byte[] octets = ipv6Address.getAddress();
                return octets.length > 0 && (octets[0] & 0xfe) == 0xfc;
            }
        } catch (UnknownHostException exception) {
            return false;
        }

        return false;
    }
}
