package com.portfolio.auth.transport.tcp;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

final class BoundedLineReader {
    private BoundedLineReader() {
    }

    static String readLine(Reader reader, int maxMessageBytes) throws IOException {
        StringBuilder line = new StringBuilder();
        int current;
        while ((current = reader.read()) != -1) {
            if (current == '\n') {
                break;
            }
            if (current != '\r') {
                line.append((char) current);
            }
            if (line.length() > maxMessageBytes) {
                throw new IOException("Mensaje JSON excede el limite configurado");
            }
        }

        if (current == -1 && line.isEmpty()) {
            return null;
        }
        return line.toString();
    }

    static void requireWithinLimit(String message, int maxMessageBytes) throws IOException {
        if (message.getBytes(StandardCharsets.UTF_8).length > maxMessageBytes) {
            throw new IOException("Mensaje JSON excede el limite configurado");
        }
    }
}
