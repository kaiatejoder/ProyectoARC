public import java.io.*;
import java.net.*;
import java.util.*;
/**
 *  Clase principal de parte del cliente que envía y recibe mensajes UDP
 * @author Dani
 * @version 1.0.0,
 **/
public class PersonaUDP extends Thread {
    private int id; // Identificador del cliente
    private String serverHost; // Dirección del servidor
    private int serverPort; // Puerto del servidor
    
    public PersonaUDP(int id, String host, int port) {
        this.id = id; // Asignar identificador
        this.serverHost = host; // Asignar dirección del servidor
        this.serverPort = port; // Asignar puerto del servidor
    }
    
    @Override
    public void run() {
        DatagramSocket socket = null;
        
        try {
            // PASO 1: Crear socket
            socket = new DatagramSocket(); 
            
            // PASO 2: Configurar timeout
            socket.setSoTimeout(3000); // 3 segundos
            
            System.out.println("[Cliente " + id + "] Socket creado en puerto: " + socket.getLocalPort());
            
            // PASO 3: Preparar mensaje
            String mensaje = "hola";
            byte[] bufferEnvio = mensaje.getBytes(); 
            
            // PASO 4: Obtener dirección del servidor
            InetAddress direccionServidor = InetAddress.getByName(serverHost);
            
            // PASO 5: Crear paquete para enviar
            DatagramPacket paqueteEnvio = new DatagramPacket(bufferEnvio, bufferEnvio.length, direccionServidor, serverPort);
            
            // PASO 6: Enviar mensaje
            System.out.println("[Cliente " + id + "] Enviando: " + mensaje);
            socket.send(paqueteEnvio);
            
            // PASO 7: Preparar buffer para respuesta
            byte[] bufferRecepcion = new byte[1000];
            DatagramPacket paqueteRecepcion = new DatagramPacket(
                bufferRecepcion,
                bufferRecepcion.length
            );
            
            // PASO 8: Esperar respuesta (BLOQUEA aquí)
            socket.receive(paqueteRecepcion);
            
            // PASO 9: Extraer respuesta
            String respuesta = new String(
                paqueteRecepcion.getData(),
                0,
                paqueteRecepcion.getLength()
            );
            
            System.out.println("[Cliente " + id + "] Respuesta del servidor: " 
                + respuesta);
            
        } catch (SocketTimeoutException e) {
            System.out.println("[Cliente " + id + "] TIMEOUT: El servidor no respondió");
        } catch (UnknownHostException e) {
            System.out.println("[Cliente " + id + "] ERROR: Host desconocido - " 
                + e.getMessage());
        } catch (IOException e) {
            System.out.println("[Cliente " + id + "] ERROR IO: " + e.getMessage());
        } finally {
            // PASO 10: Cerrar socket (SIEMPRE)
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[Cliente " + id + "] Socket cerrado");
            }
        }
    }
}
