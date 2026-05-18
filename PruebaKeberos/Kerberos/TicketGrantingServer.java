package Kerberos;

import Seguridad.Comunicacion;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.TgsResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.core.replay.ReplayCache;
import com.portfolio.auth.transport.legacy.LegacyTgsResponseMapper;

import javax.crypto.SealedObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.UUID;

import static Kerberos.AESUtils.encriptarObjeto;

public class TicketGrantingServer {
    private static final AuthConfig CONFIG = AuthConfig.fromEnvironment();
    private static final ReplayCache REPLAY_CACHE = new InMemoryReplayCache();

    private String id_servidor;
    private String id_cliente;
    private InetAddress address_cliente;
    private Client.ClientAuthentication autificacionCliente;
    private AuthenticationServer.Ticket_TGS ticket_tgs;
    private String currentRequestId = "legacy-tgs-" + UUID.randomUUID();
    private String currentVersion = ProtocolDefaults.CURRENT_VERSION;

    public static void main(String[] args) throws Exception {
        System.out.println(
                "         -----------------------------------\n" +
                        "         --   IMPLEMENTACION KERBEROS 4   --\n" +
                        "         --------------   TGT  -------------\n" +
                        "         -----------------------------------\n");

        System.out.println("\n" +
                "--------------------------------------------------\n" +
                "- INTERCAMBIO DE TGS: PARA OBTENER UN TICKET  -\n" +
                "-    QUE CONCEDE UN SERVICIO                     -\n" +
                "--------------------------------------------------");

        final int puertoServer = CONFIG.ticketGrantingServerPort();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);

        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(puertoServer)) {
            System.out.println("[TGS] Escuchando en " + puertoServer + " (concurrencia habilitada)");
            while (true) {
                java.net.Socket s = serverSocket.accept();
                pool.submit(() -> {
                    try (java.net.Socket sc = s) {
                        TicketGrantingServer TGS = new TicketGrantingServer();

                        java.io.InputStream in = sc.getInputStream();
                        java.io.OutputStream out = sc.getOutputStream();

                        TGS.recibirPeticionTicketDesdeCliente(in);

                        boolean ok = TGS.validarClienteConTicketYReplay();
                        System.out.printf("\n¿Peticion TGS valida y no repetida? %s\n", ok ? "SI" : "NO");

                        if (ok) {
                            TGS.enviarRespuestaTicketAlCliente(out);
                        } else {
                            System.out.println("[TGS] Peticion rechazada por identidad, expiracion, skew o replay.");
                        }

                    } catch (Exception e) {
                        System.err.println("[TGS] Error en handler: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public Client.ClientAuthentication getAutificacionCliente() {
        return autificacionCliente;
    }

    public void setAutificacionCliente(Client.ClientAuthentication autificacionCliente) {
        this.autificacionCliente = autificacionCliente;
    }

    public AuthenticationServer.Ticket_TGS getTicket_tgs() {
        return ticket_tgs;
    }

    public void setTicket_tgs(AuthenticationServer.Ticket_TGS ticket_tgs) {
        this.ticket_tgs = ticket_tgs;
    }

    public void recibirPeticionTicketDesdeCliente(InputStream inputStream) throws Exception {

        HashMap<String, Object> peticion = (HashMap<String, Object>) Comunicacion.recibirObjeto(inputStream);

        System.out.printf("Solicitud TGS recibida. Campos: %s \n\n", peticion.keySet());
        this.currentRequestId = stringOrDefault(
                peticion.get("[Request-Id]"),
                "legacy-tgs-" + UUID.randomUUID());
        this.currentVersion = stringOrDefault(
                peticion.get("[Protocol-Version]"),
                ProtocolDefaults.CURRENT_VERSION);

        Client.ClientAuthentication autenticacionCliente = (Client.ClientAuthentication) AESUtils
                .desencriptarObjeto((SealedObject) peticion.get("[Autentificador-c]"),
                        CONFIG.legacyClientTgsSessionKey());

        this.setAutificacionCliente(autenticacionCliente);

        AuthenticationServer.Ticket_TGS ticket_tgs = (AuthenticationServer.Ticket_TGS) AESUtils
                .desencriptarObjeto((SealedObject) peticion.get("[Ticket-tgs]"),
                        CONFIG.legacyTicketGrantingServerSecret());

        this.setTicket_tgs(ticket_tgs);

        System.out.printf("Ticket TGS validable para cliente %s, expira %s \n\n",
                ticket_tgs.getId_cliente(),
                ticket_tgs.getTiempo_vida_ticket());
        System.out.printf("Autenticador cliente recibido: id=%s, timestamp=%s \n\n",
                autenticacionCliente.getId_cliente(),
                autenticacionCliente.getTimeStamp_ClientAuthentication());

        this.setId_cliente(autenticacionCliente.getId_cliente());
        this.setAddress_cliente(autenticacionCliente.getAddress_cliente());
        this.setId_servidor((String) peticion.get("[Id-v]"));

    }

    public void enviarRespuestaTicketAlCliente(OutputStream outputStream) throws Exception {
        HashMap<String, Object> respuestaTicket = this.crearRespuestaTicket();

        SealedObject respuestaCifrada = encriptarObjeto(respuestaTicket,
                ticket_tgs.getClave_Cliente_TicketGrantingServer());

        Comunicacion.enviarObjeto(outputStream, respuestaCifrada);

        System.out.printf("\nRespuesta TGS enviada. Campos: %s", respuestaTicket.keySet());
    }

    public void setId_servidor(String id_servidor) {
        this.id_servidor = id_servidor;
    }

    public void setId_cliente(String id_cliente) {
        this.id_cliente = id_cliente;
    }

    public void setAddress_cliente(InetAddress address_cliente) {
        this.address_cliente = address_cliente;
    }

    public HashMap<String, Object> crearRespuestaTicket() throws Exception {
        Ticket_servicio ticket_servidor = new Ticket_servicio(CONFIG.legacyClientServiceSessionKey(), id_cliente,
                address_cliente, id_servidor, Math.max(1, CONFIG.ticketLifetime().toMinutes()));

        SealedObject ticket_servidor_cifrado = encriptarObjeto(ticket_servidor, CONFIG.legacyServiceSecret());

        Instant issuedAt = toInstant(ticket_servidor.getCreacion_ticket());
        Instant expiresAt = toInstant(ticket_servidor.getTiempo_vida_ticket());
        TicketService ticketDto = new TicketService(
                currentVersion,
                "legacy-ticket-service-" + currentRequestId,
                issuedAt,
                expiresAt,
                ticket_servidor.getId_cliente(),
                ticket_servidor.getIp_cliente(),
                ticket_servidor.getId_servidor(),
                ticket_servidor.getClave_cliente_servidor());
        TgsResponse response = new TgsResponse(
                currentVersion,
                currentRequestId,
                issuedAt,
                expiresAt,
                ticket_servidor.getId_cliente(),
                ticket_servidor.getId_servidor(),
                ticket_servidor.getClave_cliente_servidor(),
                ticketDto);

        System.out.printf("\n[Ticket-v] emitido para servicio %s y cliente %s\n", id_servidor, id_cliente);

        return LegacyTgsResponseMapper.toLegacyMap(response, ticket_servidor_cifrado);
    }

    private boolean validarClienteConTicketYReplay() {
        if (autificacionCliente == null || ticket_tgs == null) {
            return false;
        }

        boolean identidadValida = autificacionCliente.getIp_cliente().equals(ticket_tgs.getIp_cliente())
                && autificacionCliente.getId_cliente().equals(ticket_tgs.getId_cliente());
        boolean ticketVigente = ticket_tgs.getTiempo_vida_ticket().isAfter(LocalDateTime.now());
        boolean timestampAceptable = dentroDeSkewPermitido(autificacionCliente.getTimeStamp_ClientAuthentication());

        if (!identidadValida || !ticketVigente || !timestampAceptable) {
            return false;
        }

        return registrarAutenticadorSiNoFueUsado();
    }

    private boolean registrarAutenticadorSiNoFueUsado() {
        Instant autenticadorEmitido = toInstant(autificacionCliente.getTimeStamp_ClientAuthentication());
        Instant ticketExpira = toInstant(ticket_tgs.getTiempo_vida_ticket());
        Instant replayExpira = min(ticketExpira, autenticadorEmitido.plus(CONFIG.replayWindow()));

        String replayKey = "tgs:"
                + ticket_tgs.getId_TicketGrantingServer()
                + ":"
                + autificacionCliente.getId_cliente()
                + ":"
                + autificacionCliente.getIp_cliente()
                + ":"
                + autificacionCliente.getTimeStamp_ClientAuthentication();

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

    public static class Ticket_servicio implements Serializable {
        final String clave_cliente_servidor;
        final String id_cliente;
        final InetAddress address_cliente;
        final LocalDateTime creacion_ticket;
        final LocalDateTime tiempo_vida_ticket;
        final String id_servidor;

        public Ticket_servicio(String clave_cliente_servidor, String id_cliente, InetAddress address_cliente,
                String id_servidor, long tiempoVida) {
            this.clave_cliente_servidor = clave_cliente_servidor;
            this.id_cliente = id_cliente;
            this.address_cliente = address_cliente;
            this.id_servidor = id_servidor;
            this.creacion_ticket = LocalDateTime.now();
            this.tiempo_vida_ticket = creacion_ticket.plusMinutes(tiempoVida);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Ticket_servicio{");
            sb.append("clave_cliente_servidor=<redacted>");
            sb.append(", id_cliente='").append(id_cliente).append('\'');
            sb.append(", address_cliente=").append(address_cliente);
            sb.append(", creacion_ticket=").append(creacion_ticket);
            sb.append(", tiempo_vida_ticket=").append(tiempo_vida_ticket);
            sb.append(", id_servidor='").append(id_servidor).append('\'');
            sb.append('}');
            return sb.toString();
        }

        public String getClave_cliente_servidor() {
            return clave_cliente_servidor;
        }

        public String getId_cliente() {
            return id_cliente;
        }

        public InetAddress getAddress_cliente() {
            return address_cliente;
        }

        public LocalDateTime getCreacion_ticket() {
            return creacion_ticket;
        }

        public LocalDateTime getTiempo_vida_ticket() {
            return tiempo_vida_ticket;
        }

        public String getId_servidor() {
            return id_servidor;
        }

        public String getIp_cliente() {
            return address_cliente.getHostAddress();
        }
    }

}
