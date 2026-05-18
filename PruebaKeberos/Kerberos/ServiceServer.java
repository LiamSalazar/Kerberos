package Kerberos;

import Seguridad.Comunicacion;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.core.replay.ReplayCache;
import com.portfolio.auth.transport.legacy.LegacyServiceResponseMapper;

import javax.crypto.SealedObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.UUID;

public class ServiceServer {
    private static final AuthConfig CONFIG = AuthConfig.fromEnvironment();
    private static final ReplayCache REPLAY_CACHE = new InMemoryReplayCache();

    public final String servicio = "--------- ACCESO CONCEDIDO A MELODYFINDER --------- KERBEROS SECURITY EXITOSO ---------";

    private LocalDateTime timestamp5;
    private String clave_cliente_servidor;
    private String clave_servidor;
    private TicketGrantingServer.Ticket_servicio ticketServicio;
    private Client.ClientAuthentication clientAuthentication;
    private String currentRequestId = "legacy-service-" + UUID.randomUUID();
    private String currentVersion = ProtocolDefaults.CURRENT_VERSION;

    public static void main(String[] args) throws Exception {
        System.out.println(
                "         -----------------------------------\n" +
                        "              KERBEROS 4            \n" +
                        " -----------------------------------\n");

        System.out.println("\n" +
                "--------------------------------------------------\n" +
                "-          INTERCAMBIO DE AUTENTIFICACION        -\n" +
                "-    CLIENTE/SERVIDOR: PARA OBTENER UN SERVICIO  -\n" +
                "--------------------------------------------------");

        final int puertoServer = CONFIG.serviceServerPort();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);

        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(puertoServer)) {
            System.out.println("[Service] Escuchando en " + puertoServer + " (concurrencia habilitada)");
            while (true) {
                java.net.Socket s = serverSocket.accept();
                pool.submit(() -> {
                    try (java.net.Socket sc = s) {
                        ServiceServer serviceServer = new ServiceServer();
                        serviceServer.setClave_servidor(CONFIG.legacyServiceSecret());

                        java.io.InputStream in = sc.getInputStream();
                        java.io.OutputStream out = sc.getOutputStream();

                        serviceServer.recibirPeticionServicioDesdeCliente(in);

                        var ticket_servicio = serviceServer.getTicketServicio();
                        var clientAuth = serviceServer.getClientAuthentication();

                        boolean esClienteValido = serviceServer.validarClienteConTicket(ticket_servicio, clientAuth);
                        System.out.printf(
                                "\n¿Coinciden los datos del cliente con los del ticket servidor? %s \n"
                                        + "Datos cliente -> address: %s, id: %s ",
                                esClienteValido ? "SI COINCIDEN" : "NO COINCIDEN",
                                ticket_servicio.getAddress_cliente(),
                                clientAuth.getId_cliente());

                        if (esClienteValido) {
                            serviceServer.responderPeticionServicioCliente(out);
                        } else {
                            System.out.println("[Service] Peticion rechazada por identidad, expiracion, skew o replay.");
                        }

                    } catch (Exception e) {
                        System.err.println("[Service] Error en handler: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public Client.ClientAuthentication getClientAuthentication() {
        return clientAuthentication;
    }

    public void setClientAuthentication(Client.ClientAuthentication clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
    }

    public TicketGrantingServer.Ticket_servicio getTicketServicio() {
        return ticketServicio;
    }

    public void setTicketServicio(TicketGrantingServer.Ticket_servicio ticketServicio) {
        this.ticketServicio = ticketServicio;
    }

    public void recibirPeticionServicioDesdeCliente(InputStream inputStream) throws Exception {

        HashMap<String, Object> peticionServicio = (HashMap<String, Object>) Comunicacion.recibirObjeto(inputStream);
        this.currentRequestId = stringOrDefault(
                peticionServicio.get("[Request-Id]"),
                "legacy-service-" + UUID.randomUUID());
        this.currentVersion = stringOrDefault(
                peticionServicio.get("[Protocol-Version]"),
                ProtocolDefaults.CURRENT_VERSION);

        SealedObject ticketServicio_cifrado = (SealedObject) peticionServicio.get("[Ticket-v]");

        TicketGrantingServer.Ticket_servicio ticket_servicio = (TicketGrantingServer.Ticket_servicio) AESUtils
                .desencriptarObjeto(ticketServicio_cifrado, this.getClave_servidor());

        this.setTicketServicio(ticket_servicio);
        this.setClave_cliente_servidor(ticket_servicio.getClave_cliente_servidor());

        SealedObject autentificadorCliente_cifrado = (SealedObject) peticionServicio.get("[Autentificador-c]");

        Client.ClientAuthentication autentificadorCliente = (Client.ClientAuthentication) AESUtils
                .desencriptarObjeto(autentificadorCliente_cifrado, this.getClave_cliente_servidor());

        this.setClientAuthentication(autentificadorCliente);
        this.setTimestamp5(autentificadorCliente.getTimeStamp_ClientAuthentication());

        System.out.printf("Peticion de servicio recibida. Campos: %s\n", peticionServicio.keySet());
        System.out.printf("Ticket de servicio validable para cliente %s, expira %s\n",
                ticket_servicio.getId_cliente(),
                ticket_servicio.getTiempo_vida_ticket());
        System.out.printf("Autenticador cliente recibido: id=%s, timestamp=%s\n",
                autentificadorCliente.getId_cliente(),
                autentificadorCliente.getTimeStamp_ClientAuthentication());

    }

    public void responderPeticionServicioCliente(OutputStream outputStream) throws Exception {
        HashMap<String, Object> respuestaServicio = this.responderServicio();

        SealedObject respuestaServicio_cifrada = AESUtils.encriptarObjeto(respuestaServicio,
                this.getClave_cliente_servidor());

        Comunicacion.enviarObjeto(outputStream, respuestaServicio_cifrada);

        System.out.printf("\nEl servicio ha sido otorgado al cliente");

        System.out.printf("Respuesta de servicio enviada. Campos: %s", respuestaServicio.keySet());
    }

    public HashMap<String, Object> responderServicio() throws Exception {
        Instant issuedAt = Instant.now();
        Instant ticketExpiresAt = toInstant(ticketServicio.getTiempo_vida_ticket());
        Instant authenticatorIssuedAt = toInstant(timestamp5);
        ServiceResponse response = new ServiceResponse(
                currentVersion,
                currentRequestId,
                issuedAt,
                ticketExpiresAt,
                ticketServicio.getId_cliente(),
                ticketServicio.getId_servidor(),
                authenticatorIssuedAt,
                issuedAt,
                servicio,
                true);

        return LegacyServiceResponseMapper.toLegacyMap(response);
    }

    private boolean validarClienteConTicket(TicketGrantingServer.Ticket_servicio ticket_servicio,
            Client.ClientAuthentication autentificadorCliente) {
        if (ticket_servicio == null || autentificadorCliente == null) {
            return false;
        }

        boolean esClienteValido = ticket_servicio.getId_cliente().equals(autentificadorCliente.getId_cliente())
                && ticket_servicio.getIp_cliente().equals(autentificadorCliente.getIp_cliente())
                && ticket_servicio.getTiempo_vida_ticket().isAfter(LocalDateTime.now())
                && dentroDeSkewPermitido(autentificadorCliente.getTimeStamp_ClientAuthentication());

        if (!esClienteValido) {
            return false;
        }

        return registrarAutenticadorSiNoFueUsado(ticket_servicio, autentificadorCliente);
    }

    private boolean registrarAutenticadorSiNoFueUsado(TicketGrantingServer.Ticket_servicio ticket_servicio,
            Client.ClientAuthentication autentificadorCliente) {
        Instant autenticadorEmitido = toInstant(autentificadorCliente.getTimeStamp_ClientAuthentication());
        Instant ticketExpira = toInstant(ticket_servicio.getTiempo_vida_ticket());
        Instant replayExpira = min(ticketExpira, autenticadorEmitido.plus(CONFIG.replayWindow()));

        String replayKey = "service:"
                + ticket_servicio.getId_servidor()
                + ":"
                + autentificadorCliente.getId_cliente()
                + ":"
                + autentificadorCliente.getIp_cliente()
                + ":"
                + autentificadorCliente.getTimeStamp_ClientAuthentication();

        return REPLAY_CACHE.registerIfAbsent(replayKey, replayExpira);
    }

    private static boolean dentroDeSkewPermitido(LocalDateTime timestamp) {
        Duration diferencia = Duration.between(toInstant(timestamp), Instant.now()).abs();
        return diferencia.compareTo(CONFIG.allowedClockSkew()) <= 0;
    }

    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    public String getClave_servidor() {
        return clave_servidor;
    }

    public void setClave_servidor(String clave_servidor) {
        this.clave_servidor = clave_servidor;
    }

    public String getClave_cliente_servidor() {
        return clave_cliente_servidor;
    }

    public void setClave_cliente_servidor(String clave_cliente_servidor) {
        this.clave_cliente_servidor = clave_cliente_servidor;
    }

    public LocalDateTime getTimestamp5() {
        return timestamp5;
    }

    public void setTimestamp5(LocalDateTime timestamp5) {
        this.timestamp5 = timestamp5;
    }

}
