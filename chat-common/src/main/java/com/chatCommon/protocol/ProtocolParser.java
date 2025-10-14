package com.chatCommon.protocol;

import java.util.*;

public class ProtocolParser {

    private final char delimiter;
    private final char escapeChar;

    public ProtocolParser(char delimiter, char escapeChar) {
        this.delimiter = delimiter;
        this.escapeChar = escapeChar;
    }

    public List<String> decode(String message) {
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean isEscaping = false;

        for (char c : message.toCharArray()) {
            if (isEscaping) {
                currentPart.append(c);
                isEscaping = false;
            } else if (c == escapeChar) {
                isEscaping = true;
            } else if (c == delimiter) {
                parts.add(currentPart.toString());
                currentPart.setLength(0);
            } else {
                currentPart.append(c);
            }
        }
        parts.add(currentPart.toString());
        return parts;
    }

    public Map<String, String> decodeMap(String message) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String part : decode(message)) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    public String encode(String... parts) {
        StringJoiner joiner = new StringJoiner(String.valueOf(delimiter));
        for (String part : parts) {
            joiner.add(escape(part));
        }
        return joiner.toString();
    }

    private String escape(String part) {
        StringBuilder escaped = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (c == delimiter || c == escapeChar) {
                escaped.append(escapeChar);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}