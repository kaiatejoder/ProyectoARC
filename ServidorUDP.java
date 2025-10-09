import java.io.*;
import java.net.*;

public class ServidorUDP {
    
    final static int SERVER_PORT = 10578; // Puerto del servidor
    
    public static void main(String args[]) {
        System.out.print("Inicializando servidor UDP... ");
        
        try {
            // PASO 1: Crear socket servidor
            DatagramSocket socketUDP = new DatagramSocket(SERVER_PORT); 
            System.out.println("\t[OK]");
            System.out.println("Servidor escuchando en puerto " + SERVER_PORT);
            
            byte[] buffer = new byte[1000];
            int idSession = 0;
            
            // PASO 2: Bucle infinito
            while (true) {
                // PASO 3: Preparar paquete para recibir
                DatagramPacket peticion = new DatagramPacket(buffer, buffer.length); 
                
                // PASO 4: Esperar petición (BLOQUEA aquí)
                socketUDP.receive(peticion);
                
                // PASO 5: Extraer información
                String mensajeRecibido = new String(peticion.getData(), 0, peticion.getLength()); //se le pone .getLength() para que no coja basura del buffer si creaste un buffer de 1000 bytes pero solo recibiste "hola" (4 bytes), getData() te devolvería todo el array de 1000 bytes, pero getLength() te dice que solo los primeros 4 bytes son válidos.
                InetAddress clienteAddress = peticion.getAddress(); // devuelve el IP del cliente
                int clientePort = peticion.getPort(); // Devuelve el puerto desde el cual el cliente envio el paquete
                
                // Mostrar información recibida
                System.out.println("\n=== Nueva Petición ===");
                System.out.println("De: " + clienteAddress + ":" + clientePort);
                System.out.println("Mensaje: " + mensajeRecibido);
                System.out.println("ID Sesión: " + idSession);

                // PASO 6: Procesar mensaje
                String respuesta = procesarMensaje(mensajeRecibido, idSession); //dando respuesta con la función procesarMensaje

                // PASO 7: Preparar respuesta
                byte[] datosRespuesta = respuesta.getBytes();
                DatagramPacket paqueteRespuesta = new DatagramPacket(datosRespuesta, datosRespuesta.length,clienteAddress,clientePort);

                // PASO 8: Enviar respuesta
                socketUDP.send(paqueteRespuesta);
                System.out.println("Respuesta enviada: " + respuesta);
                
                idSession++;
                
                // PASO 9: Limpiar buffer
                buffer = new byte[1000];
            }
            
        } catch (SocketException e) {
            System.out.println("Error Socket: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error IO: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Método para procesar mensajes (personalizable)
    private static String procesarMensaje(String mensaje, int idSession) {
        if (mensaje.equals("hola")) {
            System.out.println("Cliente " + idSession + " saluda");
            return "adios";
        } else if (mensaje.equals("ping")) {
            return "pong";
        } else if (mensaje.equals("fecha")) {
            return new java.util.Date().toString();
        } else {
            return "Comando no reconocido: " + mensaje;
        }
    }
}