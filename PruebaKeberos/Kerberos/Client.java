package Kerberos;

import Seguridad.Comunicacion;
import Seguridad.Conexiones;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ServiceRequest;
import com.portfolio.auth.core.protocol.dto.TgsRequest;
import com.portfolio.auth.transport.javaio.JavaObjectTransport;
import com.portfolio.auth.transport.legacy.LegacyAsRequestMapper;
import com.portfolio.auth.transport.legacy.LegacyServiceRequestMapper;
import com.portfolio.auth.transport.legacy.LegacyTgsRequestMapper;

import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("LanguageDetectionInspection")
public class Client {
    private static final AuthConfig CONFIG = AuthConfig.fromEnvironment();

    private final String id_cliente;
    private final InetAddress address_cliente;
    private String id_Servidor;
    private SealedObject ticket_tgs;
    private String password_cliente;
    private String id_TicketGrantingServer;
    private SealedObject ticket_servicio;
    private String clave_Cliente_TicketGrantingServer;
    private String clave_cliente_servidor;

    public Client(String id_cliente, InetAddress address_cliente) {
        this.id_cliente = id_cliente;
        this.address_cliente = address_cliente;
    }

    public static void main(String[] args) throws Exception {

        System.out.println(
                "         -----------------------------------\n" +
                        "         --            KERBEROS 4         --\n" +
                        "         -------------- CLIENTE ------------\n" +
                        "         -----------------------------------\n");

        String ipCliente = CONFIG.clientHost();

        String ipAS = CONFIG.authenticationServerHost();
        int puertoAS = CONFIG.authenticationServerPort();

        String ipTGS = CONFIG.ticketGrantingServerHost();
        int puertoTGS = CONFIG.ticketGrantingServerPort();

        String ipServiceServer = CONFIG.serviceServerHost();
        int puertoServiceServer = CONFIG.serviceServerPort();

        System.out.println();

        Client cliente = new Client(CONFIG.defaultClientId(), InetAddress.getByName(ipCliente));
        cliente.setId_TicketGrantingServer(CONFIG.defaultTicketGrantingServerId());

        System.out.println("\n" +
                "--------------------------------------------------\n" +
                "-    INTERCAMBIO DE SERVICIO DE AUNTENTIFICACION:-\n" +
                "-                PARA OBTENER TGT                -\n" +
                "--------------------------------------------------");

        Socket conexionAS = Conexiones.obtenerConexion(puertoAS, ipAS);
        InputStream inputStream = conexionAS.getInputStream();
        OutputStream outputStream = conexionAS.getOutputStream();

        cliente.realizarPeticionTGThaciaAS(outputStream);
        cliente.recibirTGTdesdeAS(inputStream);

        conexionAS.close();

        System.out.println(
                "\n" +
                        "--------------------------------------------------\n" +
                        "-    INTERCAMBIO DE TGS: PARA OBTENER UN TICKET  -\n" +
                        "-             QUE CONCEDE UN SERVICIO            -\n" +
                        "--------------------------------------------------");

        cliente.setId_Servidor(CONFIG.defaultServiceId());

        Socket conexionTGS = Conexiones.obtenerConexion(puertoTGS, ipTGS);
        inputStream = conexionTGS.getInputStream();
        outputStream = conexionTGS.getOutputStream();

        cliente.realizarPeticionTickethaciaTGS(outputStream);
        cliente.recibirTicketdesdeTGS(inputStream);

        conexionTGS.close();

        System.out.println(
                "\n" +
                        "--------------------------------------------------\n" +
                        "-          INTERCAMBIO DE AUTENTIFICACION        -\n" +
                        "-    CLIENTE/SERVIDOR: PARA OBTENER UN SERVICIO  -\n" +
                        "--------------------------------------------------");

        Socket conexionServiceServer = Conexiones.obtenerConexion(puertoServiceServer, ipServiceServer);

        inputStream = conexionServiceServer.getInputStream();
        outputStream = conexionServiceServer.getOutputStream();

        cliente.realizarPeticionServiciohaciaServidor(outputStream);
        cliente.recibirServiciodesdeServidor(inputStream);

        conexionServiceServer.close();
    }

    public void realizarPeticionTGThaciaAS(OutputStream outputStream) throws Exception {
        HashMap<String, Object> solicitudTGS = this.generarSolicitudTGS();
        JavaObjectTransport.send(outputStream, solicitudTGS);
    }

    public void recibirTGTdesdeAS(InputStream inputStream) throws Exception {

        SealedObject respuetaCifrada = (SealedObject) Comunicacion.recibirObjeto(inputStream);

        HashMap<String, Object> respuestaDescifrada = (HashMap<String, Object>) AESUtils
                .desencriptarObjeto(respuetaCifrada, CONFIG.legacyClientSecret());
        System.out.printf("Respuesta recibida desde el AS. Campos: %s, TGS: %s, expira: %s \n\n",
                respuestaDescifrada.keySet(),
                respuestaDescifrada.get("[Id-tgs]"),
                respuestaDescifrada.get("[TiempoVida-2]"));

        this.setClave_Cliente_TicketGrantingServer((String) respuestaDescifrada.get("[K-c_tgs]"));

        this.setId_TicketGrantingServer((String) respuestaDescifrada.get("[Id-tgs]"));
        this.setTicket_tgs((SealedObject) respuestaDescifrada.get("[Ticket-tgs]"));
    }

    public void realizarPeticionTickethaciaTGS(OutputStream outputStream) throws Exception {
        HashMap<String, Object> solicitudIntercambioTGS = this.generarSolicitudIntercambioTGS();

        Comunicacion.enviarObjeto(outputStream, solicitudIntercambioTGS);
    }

    public void recibirTicketdesdeTGS(InputStream inputStream) throws Exception {
        SealedObject respuestaCifrada = (SealedObject) Comunicacion.recibirObjeto(inputStream);

        HashMap<String, Object> respuesta = (HashMap<String, Object>) AESUtils.desencriptarObjeto(respuestaCifrada,
                this.getClave_Cliente_TicketGrantingServer());

        System.out.printf("Respuesta recibida desde el TGS. Campos: %s, servicio: %s, expira: %s \n\n",
                respuesta.keySet(),
                respuesta.get("[Id-v]"),
                respuesta.get("[TiempoVida-4]"));

        this.setTicket_servicio((SealedObject) respuesta.get("[Ticket-v]"));

        this.setClave_cliente_servidor((String) respuesta.get("[K-c_v]"));

    }

    public void realizarPeticionServiciohaciaServidor(OutputStream outputStream) throws Exception {
        HashMap<String, Object> peticionServicio = this.generarSolicitudIntercambioServicio();

        Comunicacion.enviarObjeto(outputStream, peticionServicio);
        System.out.printf("Peticion de servicio enviada. Campos: %s\n", peticionServicio.keySet());
    }

    public void recibirServiciodesdeServidor(InputStream inputStream) throws Exception {
        SealedObject respuestaCifrada = (SealedObject) Comunicacion.recibirObjeto(inputStream);

        HashMap<String, Object> respuestaServicio = (HashMap<String, Object>) AESUtils
                .desencriptarObjeto(respuestaCifrada, this.getClave_cliente_servidor());

        System.out.printf("\nRespuesta recibida desde el servicio. Campos: %s\n", respuestaServicio.keySet());
        String servicioRecibido = (String) respuestaServicio.get("[Servicio]");

        System.out.println(servicioRecibido);
    }

    public String getClave_cliente_servidor() {
        return clave_cliente_servidor;
    }

    public void setClave_cliente_servidor(String clave_cliente_servidor) {
        this.clave_cliente_servidor = clave_cliente_servidor;
    }

    public SealedObject getTicket_servicio() {
        return ticket_servicio;
    }

    public void setTicket_servicio(SealedObject ticket_servicio) {
        this.ticket_servicio = ticket_servicio;
    }

    public String getClave_Cliente_TicketGrantingServer() {
        return clave_Cliente_TicketGrantingServer;
    }

    public void setClave_Cliente_TicketGrantingServer(String clave_Cliente_TicketGrantingServer) {
        this.clave_Cliente_TicketGrantingServer = clave_Cliente_TicketGrantingServer;
    }

    public void setPassword_cliente(String password_cliente) {
        this.password_cliente = password_cliente;
    }

    public void setId_TicketGrantingServer(String id_TicketGrantingServer) {
        this.id_TicketGrantingServer = id_TicketGrantingServer;
    }

    public HashMap<String, Object> generarSolicitudTGS() {
        AsRequest request = new AsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                UUID.randomUUID().toString(),
                id_cliente,
                id_TicketGrantingServer,
                Instant.now());

        return LegacyAsRequestMapper.toLegacyMap(request);
    }

    public HashMap<String, Object> generarSolicitudIntercambioTGS() throws Exception {

        ClientAuthentication autentificador_cliente = new ClientAuthentication(id_cliente, address_cliente);
        Instant issuedAt = toInstant(autentificador_cliente.getTimeStamp_ClientAuthentication());

        SecretKey clave_cliente_TGS = (SecretKey) AESUtils.getKeyFromPassword(this.clave_Cliente_TicketGrantingServer);
        SealedObject autentificadorCifrado = AESUtils.encriptarObjeto(autentificador_cliente, clave_cliente_TGS);

        ClientAuthenticator authenticatorDto = new ClientAuthenticator(
                ProtocolDefaults.CURRENT_VERSION,
                UUID.randomUUID().toString(),
                issuedAt,
                issuedAt.plus(CONFIG.replayWindow()),
                id_cliente,
                address_cliente.getHostAddress());
        TgsRequest request = new TgsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                UUID.randomUUID().toString(),
                issuedAt,
                id_cliente,
                id_Servidor,
                id_TicketGrantingServer,
                null,
                authenticatorDto);

        return LegacyTgsRequestMapper.toLegacyMap(request, ticket_tgs, autentificadorCifrado);
    }

    public HashMap<String, Object> generarSolicitudIntercambioServicio() throws Exception {

        ClientAuthentication autentificador_cliente = new ClientAuthentication(id_cliente, address_cliente);
        Instant issuedAt = toInstant(autentificador_cliente.getTimeStamp_ClientAuthentication());
        SecretKey clave_cliente_servidor = AESUtils.getKeyFromPassword(this.clave_cliente_servidor);
        SealedObject autentificadorCifrado = AESUtils.encriptarObjeto(autentificador_cliente, clave_cliente_servidor);

        ClientAuthenticator authenticatorDto = new ClientAuthenticator(
                ProtocolDefaults.CURRENT_VERSION,
                UUID.randomUUID().toString(),
                issuedAt,
                issuedAt.plus(CONFIG.replayWindow()),
                id_cliente,
                address_cliente.getHostAddress());
        ServiceRequest request = new ServiceRequest(
                ProtocolDefaults.CURRENT_VERSION,
                UUID.randomUUID().toString(),
                issuedAt,
                id_cliente,
                id_Servidor,
                null,
                authenticatorDto);

        System.out.printf("\n[Ticket-v] preparado para el servicio %s\n", id_Servidor);

        return LegacyServiceRequestMapper.toLegacyMap(request, ticket_servicio, autentificadorCifrado);
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public void setTicket_tgs(SealedObject ticket_tgs) {
        this.ticket_tgs = ticket_tgs;
    }

    public void setId_Servidor(String id_Servidor) {
        this.id_Servidor = id_Servidor;
    }

    public static class ClientAuthentication implements Serializable {

        private final String id_cliente;
        private final InetAddress address_cliente;
        private final LocalDateTime timeStamp_ClientAuthentication;

        public ClientAuthentication(String ID_cliente, String address_cliente) throws UnknownHostException {
            this.id_cliente = ID_cliente;
            this.address_cliente = InetAddress.getByName(address_cliente);
            timeStamp_ClientAuthentication = LocalDateTime.now();
        }

        public ClientAuthentication(String ID_cliente, InetAddress address_cliente) throws UnknownHostException {
            this.id_cliente = ID_cliente;
            this.address_cliente = address_cliente;
            timeStamp_ClientAuthentication = LocalDateTime.now();
        }

        public String getId_cliente() {
            return id_cliente;
        }

        public InetAddress getAddress_cliente() {
            return address_cliente;
        }

        public String getIp_cliente() {
            return address_cliente.getHostAddress();
        }

        public LocalDateTime getTimeStamp_ClientAuthentication() {
            return timeStamp_ClientAuthentication;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ClientAuthentication{");
            sb.append("id_cliente='").append(id_cliente).append('\'');
            sb.append(", address_cliente=").append(address_cliente);
            sb.append(", timeStamp_ClientAuthentication=").append(timeStamp_ClientAuthentication);
            sb.append('}');
            return sb.toString();
        }
    }

}
