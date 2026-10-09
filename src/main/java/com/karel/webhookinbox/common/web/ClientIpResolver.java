package com.karel.webhookinbox.common.web;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "x-forwarded-for";
    private static final String X_REAL_IP = "x-real-ip";
    private static final int MAX_LENGTH = 64;

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    public String resolve(Map<String, List<String>> headers, String remoteAddr) {
        String forwarded = firstHeaderValue(headers, X_FORWARDED_FOR);
        if (forwarded != null) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                String normalized = normalize(first);
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        String realIp = firstHeaderValue(headers, X_REAL_IP);
        if (realIp != null && !realIp.isBlank()) {
            String normalized = normalize(realIp.trim());
            if (normalized != null) {
                return normalized;
            }
        }
        return normalize(remoteAddr);
    }

    private String firstHeaderValue(Map<String, List<String>> headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                List<String> values = entry.getValue();
                return (values == null || values.isEmpty()) ? null : values.get(0);
            }
        }
        return null;
    }

    private String normalize(String ip) {
        if (ip == null || ip.isBlank() || !isIpAddress(ip.trim())) {
            return null;
        }
        String trimmed = ip.trim();
        return trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
    }

    private boolean isIpAddress(String value) {
        if (value.contains(":")) {
            try {
                return InetAddress.getByName(value) instanceof Inet6Address;
            } catch (UnknownHostException ex) {
                return false;
            }
        }
        Matcher matcher = IPV4.matcher(value);
        if (!matcher.matches()) {
            return false;
        }
        for (int i = 1; i <= 4; i++) {
            if (Integer.parseInt(matcher.group(i)) > 255) {
                return false;
            }
        }
        return true;
    }
}
