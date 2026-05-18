package Kerberos;

import Seguridad.Comunicacion;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import com.portfolio.auth.core.protocol.dto.AsResponse;
import com.portfolio.auth.core.protocol.dto.TicketTgs;
import com.portfolio.auth.transport.javaio.JavaObjectTransport;
import com.portfolio.auth.transport.legacy.LegacyAsRequestMapper;
import com.portfolio.auth.transport.legacy.LegacyAsResponseMapper;

import javax.crypto.SealedObject;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static Kerberos.AESUtils.encriptarObjeto;

public class AuthenticationServer {
    private static final AuthConfig CONFIG = AuthConfig.fromEnvironment();

    static final Map<String, String> BDD_usuarios = new HashMap<String, String>() {
        {
            put(CONFIG.defaultClientId(), CONFIG.legacyClientSecret());
        }
    };
    static final Map<String, String> BDD_TGS = new HashMap<String, String>() {
        {
            put(CONFIG.defaultTicketGrantingServerId(), CONFIG.legacyTicketGrantingServerSecret());
        }
    };
    static final Map<String, String> BDD_Servidor = new HashMap<String, String>() {
        {
            put(CONFIG.defaultServiceId(), CONFIG.legacyServiceSecret());
        }
    };
    private String id_cliente;
    private InetAddress address_cliente;
    private String id_TicketGrantingServer;
    private String clave_Cliente_TicketGrantingServer = CONFIG.legacyClientTgsSessionKey();
    private String clave_TicketGrantingServer = CONFIG.legacyTicketGrantingServerSecret();
    private AsRequest currentRequest;

    public static void main(String[] args) throws Exception {
        System.out.println(
                "         -----------------------------------\n" +
                        "         --   IMPLEMENTACION KERBEROS 4   --\n" +
                        "         --------------   AS    ------------\n" +
                        "         -----------------------------------\n");

        System.out.println("\n" +
                "--------------------------------------------------\n" +
                "-    INTERCAMBIO DE SERVICIO DE AUNTENTIFICACION:-\n" +
                "-                  PARA OBTENER TGT              -\n" +
                "--------------------------------------------------");

        final int puertoServer = CONFIG.authenticationServerPort();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);

        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(puertoServer)) {
            System.out.println("[AS] Escuchando en " + puertoServer + " (concurrencia habilitada)");
            while (true) {
                java.net.Socket s = serverSocket.accept();
                pool.submit(() -> {
                    try (java.net.Socket sc = s) {
                        // Instancia nueva por conexion: estado aislado por cliente.
                        AuthenticationServer AS = new AuthenticationServer();

                        java.io.OutputStream out = sc.getOutputStream();

                        AS.recibirSolicitudTGTdesdeCliente(sc);
                        AS.responderSolicitudTGTalCliente(out);

                    } catch (Exception e) {
                        System.err.println("[AS] Error en handler: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public void recibirSolicitudTGTdesdeCliente(Socket conexionCliente) throws Exception {
        HashMap<String, Object> solicitudTGT = JavaObjectTransport.receive(conexionCliente.getInputStream(), HashMap.class);
        AsRequest request = LegacyAsRequestMapper.fromLegacyMap(solicitudTGT);

        System.out.printf("Solicitud recibida: %s \n DTO tipado: %s \n\n", solicitudTGT, request);

        this.currentRequest = request;
        this.setAddress_cliente(conexionCliente.getInetAddress());
        this.setClave_Cliente_TicketGrantingServer(CONFIG.legacyClientTgsSessionKey());
        this.setId_cliente(request.clientId());
        this.setId_TicketGrantingServer(request.ticketGrantingServerId());
    }

    public void responderSolicitudTGTalCliente(OutputStream outputStream) throws Exception {
        HashMap<String, Object> respuestaSolicitud = this.getRespuestaSolicitudTicket_TGS();

        SealedObject respuestaCifrada = encriptarObjeto(respuestaSolicitud, BDD_usuarios.get(this.id_cliente));

        Comunicacion.enviarObjeto(outputStream, respuestaCifrada);
    }

    public void setId_cliente(String id_cliente) {
        this.id_cliente = id_cliente;
    }

    public void setClave_Cliente_TicketGrantingServer(String clave_Cliente_TicketGrantingServer) {
        this.clave_Cliente_TicketGrantingServer = clave_Cliente_TicketGrantingServer;
    }

    public void setId_TicketGrantingServer(String id_TicketGrantingServer) {
        this.id_TicketGrantingServer = id_TicketGrantingServer;
    }

    public void setClave_TicketGrantingServer(String clave_TicketGrantingServer) {
        this.clave_TicketGrantingServer = clave_TicketGrantingServer;
    }

    public void setAddress_cliente(InetAddress address_cliente) {
        this.address_cliente = address_cliente;
    }

    HashMap<String, Object> getRespuestaSolicitudTicket_TGS() throws Exception {
        Ticket_TGS ticket_tgs = new Ticket_TGS(clave_Cliente_TicketGrantingServer, id_cliente, address_cliente,
                id_TicketGrantingServer, Math.max(1, CONFIG.ticketLifetime().toMinutes()));

        SealedObject ticket_tgs_cifrado = encriptarObjeto(ticket_tgs, clave_TicketGrantingServer);

        Instant issuedAt = toInstant(ticket_tgs.getMomentoCreacion_ticket());
        Instant expiresAt = toInstant(ticket_tgs.getTiempo_vida_ticket());
        String requestId = currentRequest == null ? "legacy-as-" + UUID.randomUUID() : currentRequest.requestId();
        String version = currentRequest == null ? ProtocolDefaults.CURRENT_VERSION : currentRequest.version();

        TicketTgs ticketDto = new TicketTgs(
                version,
                "legacy-ticket-tgs-" + requestId,
                issuedAt,
                expiresAt,
                ticket_tgs.getId_cliente(),
                ticket_tgs.getIp_cliente(),
                ticket_tgs.getId_TicketGrantingServer(),
                ticket_tgs.getClave_Cliente_TicketGrantingServer());
        AsResponse response = new AsResponse(
                version,
                requestId,
                issuedAt,
                expiresAt,
                ticket_tgs.getId_cliente(),
                ticket_tgs.getId_TicketGrantingServer(),
                ticket_tgs.getClave_Cliente_TicketGrantingServer(),
                ticketDto);

        return LegacyAsResponseMapper.toLegacyMap(response, ticket_tgs_cifrado);
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static class Ticket_TGS implements Serializable {
        final String clave_Cliente_TicketGrantingServer;
        final String id_cliente;
        final InetAddress address_cliente;
        final String id_TicketGrantingServer;
        final LocalDateTime creacion_ticket;
        final LocalDateTime tiempo_vida_ticket;

        public Ticket_TGS(String clave_Cliente_TicketGrantingServer, String id_cliente, InetAddress address_cliente,
                String id_TicketGrantingServer, long tiempoVida) {
            this.clave_Cliente_TicketGrantingServer = clave_Cliente_TicketGrantingServer;
            this.id_cliente = id_cliente;
            this.address_cliente = address_cliente;
            this.id_TicketGrantingServer = id_TicketGrantingServer;
            this.creacion_ticket = LocalDateTime.now();
            this.tiempo_vida_ticket = creacion_ticket.plusMinutes(tiempoVida);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Ticket_TGS{");
            sb.append("clave_Cliente_TicketGrantingServer=<redacted>");
            sb.append(", id_cliente='").append(id_cliente).append('\'');
            sb.append(", address_cliente=").append(address_cliente);
            sb.append(", id_TicketGrantingServer='").append(id_TicketGrantingServer).append('\'');
            sb.append(", creacion_ticket=").append(creacion_ticket);
            sb.append(", tiempo_vida_ticket=").append(tiempo_vida_ticket);
            sb.append('}');
            return sb.toString();
        }

        public String getClave_Cliente_TicketGrantingServer() {
            return clave_Cliente_TicketGrantingServer;
        }

        public String getId_cliente() {
            return id_cliente;
        }

        public InetAddress getAddress_cliente() {
            return address_cliente;
        }

        public String getId_TicketGrantingServer() {
            return id_TicketGrantingServer;
        }

        public LocalDateTime getMomentoCreacion_ticket() {
            return creacion_ticket;
        }

        public LocalDateTime getTiempo_vida_ticket() {
            return tiempo_vida_ticket;
        }

        public String getIp_cliente() {
            return address_cliente.getHostAddress();
        }
    }

}
