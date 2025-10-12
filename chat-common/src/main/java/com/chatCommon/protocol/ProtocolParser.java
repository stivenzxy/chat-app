package com.chatCommon.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ProtocolParser {

    private final char delimiter;
    private final char escapeChar;

    public ProtocolParser(char delimiter, char escapeChar) {
        this.delimiter = delimiter;
        this.escapeChar = escapeChar;
    }

    /**
     * Decodifica un mensaje de la red en sus partes componentes,
     * respetando las reglas de escape.
     */
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

    /**
     * Codifica una serie de strings en un único mensaje para la red,
     * aplicando las reglas de escape necesarias.
     */
    public String encode(String... parts) {
        StringJoiner joiner = new StringJoiner(String.valueOf(delimiter));
        for (String part : parts) {
            joiner.add(escape(part));
        }
        return joiner.toString();
    }

    /**
     * Método auxiliar para escapar un único string.
     */
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