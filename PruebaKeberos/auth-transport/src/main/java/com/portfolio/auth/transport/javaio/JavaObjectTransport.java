package com.portfolio.auth.transport.javaio;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Envoltura minima sobre serializacion Java para migraciones graduales.
 *
 * Replica el comportamiento actual del proyecto: un solo objeto por direccion
 * y por socket, sin cerrar el stream recibido.
 */
public final class JavaObjectTransport {
    private JavaObjectTransport() {
    }

    public static void send(OutputStream outputStream, Serializable message) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(message);
        objectOutputStream.flush();
    }

    public static <T> T receive(InputStream inputStream, Class<T> expectedType)
            throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        Object message = objectInputStream.readObject();

        if (!expectedType.isInstance(message)) {
            throw new IOException(
                    "Tipo de mensaje inesperado. Se esperaba " + expectedType.getName() + " pero llego "
                            + message.getClass().getName());
        }

        return expectedType.cast(message);
    }
}
