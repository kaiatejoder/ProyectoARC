package com.g13.proyectoINTENTO_NUEVO;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorUDP {

    // Estructura para almacenar la información de cada cliente conectado.
    private record InfoCliente(int idCliente, int idGrupo, InetAddress direccion, int puerto) {}

    // Mapa para almacenar a los clientes conectados de forma segura para hilos.
    private static Map<Integer, InfoCliente> clientesConectados = new ConcurrentHashMap<>();
    
    // Parámetros de la simulación
    private static int N, V, S;

    public static void main(String args[]) {
        Scanner escaner = new Scanner(System.in);
        System.out.println("--- CONFIGURACIÓN DEL SERVIDOR ---");
        System.out.print("Introduce el número total de clientes (N): ");
        N = escaner.nextInt();
        System.out.print("Introduce el número de vecinos por grupo (V): ");
        V = escaner.nextInt();
        System.out.print("Introduce el número de iteraciones (S): ");
        S = escaner.nextInt();

        if (N % V != 0) {
            System.out.println("Error: N debe ser múltiplo de V.");
            escaner.close();
            return;
        }

        System.out.print("Inicializando servidor UDP... ");

        try (DatagramSocket socketUDP = new DatagramSocket(10578)) {
            System.out.println("\t[OK]");
            System.out.println("Servidor escuchando en puerto " + socketUDP.getLocalPort());

            // =================================================================
            // FASE 1: INICIO Y REGISTRO DE CLIENTES
            // =================================================================
            System.out.println("\nFASE DE INICIO: Esperando " + N + " clientes...");
            for (int i = 0; i < N; i++) {
                byte[] bufer = new byte[1024];
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socketUDP.receive(peticion);

                InetAddress direccionCliente = peticion.getAddress();
                int puertoCliente = peticion.getPort();

                // Asignamos ID de cliente y de grupo según el orden de conexión
                int idCliente = i;
                int idGrupo = i / V;

                InfoCliente nuevoCliente = new InfoCliente(idCliente, idGrupo, direccionCliente, puertoCliente);
                clientesConectados.put(idCliente, nuevoCliente);
                System.out.println("  -> Cliente " + idCliente + " (Grupo " + idGrupo + ") registrado desde " + direccionCliente + ":" + puertoCliente);

                // Enviamos al cliente su información de registro
                String respuesta = "REGISTRADO;" + idCliente + ";" + idGrupo + ";" + V;
                byte[] datosRespuesta = respuesta.getBytes();
                DatagramPacket paqueteRespuesta = new DatagramPacket(datosRespuesta, datosRespuesta.length, direccionCliente, puertoCliente);
                socketUDP.send(paqueteRespuesta);
            }
            System.out.println("Registro completo. Todos los " + N + " clientes están conectados.");

            // =================================================================
            // FASE 2: SIMULACIÓN
            // =================================================================
            System.out.println("\nFASE DE SIMULACIÓN: Enviando señal de inicio...");
            String senalInicio = "INICIAR_SIMULACION";
            for (InfoCliente cliente : clientesConectados.values()) {
                byte[] datosInicio = senalInicio.getBytes();
                DatagramPacket paqueteInicio = new DatagramPacket(datosInicio, datosInicio.length, cliente.direccion(), cliente.puerto());
                socketUDP.send(paqueteInicio);
            }
            System.out.println("Simulación iniciada. Procesando mensajes...");

            int clientesFinalizados = 0;
            Map<Integer, Double> tiemposPorGrupo = new HashMap<>();
            double tiempoTotalSistema = 0.0;

            // Bucle principal para procesar mensajes de la simulación
            while (clientesFinalizados < N) {
                byte[] bufer = new byte[1024];
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socketUDP.receive(peticion);

                String mensajeRecibido = new String(peticion.getData(), 0, peticion.getLength());
                String[] partes = mensajeRecibido.split(";");
                String comando = partes[0];

                InfoCliente clienteRemitente = buscarClientePorDireccion(peticion.getAddress(), peticion.getPort());
                if (clienteRemitente == null) continue; // Ignorar si el cliente no está registrado

                switch (comando) {
                    case "COORDS": // Un cliente envía sus coordenadas
                        gestionarCoordenadas(socketUDP, peticion.getData(), peticion.getLength(), clienteRemitente);
                        break;
                    case "ACK": // Un vecino envía un ACK
                        gestionarAck(socketUDP, partes, clienteRemitente);
                        break;
                    case "DONE": // Un cliente ha terminado sus S iteraciones
                        clientesFinalizados++;
                        double tiempoMedioCliente = Double.parseDouble(partes[1]);
                        tiempoTotalSistema += tiempoMedioCliente;
                        tiemposPorGrupo.merge(clienteRemitente.idGrupo(), tiempoMedioCliente, Double::sum);
                        System.out.println("  -> Cliente " + clienteRemitente.idCliente() + " ha terminado. (" + clientesFinalizados + "/" + N + ")");
                        break;
                }
            }
            
            // =================================================================
            // FASE 3: CÁLCULO DE RESULTADOS
            // =================================================================
            System.out.println("\nFASE DE CÁLCULO: Todos los clientes han terminado.");
            System.out.println("--- RESULTADOS FINALES ---");
            System.out.printf("Tiempo medio de respuesta de todo el sistema: %.4f ms\n", tiempoTotalSistema / N);
            for (Map.Entry<Integer, Double> entrada : tiemposPorGrupo.entrySet()) {
                System.out.printf("  - Tiempo medio del Grupo %d: %.4f ms\n", entrada.getKey(), entrada.getValue() / V);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gestionarCoordenadas(DatagramSocket socket, byte[] datosOriginales, int longitud, InfoCliente remitente) throws IOException {
        System.out.println("Recibido COORDS del Cliente " + remitente.idCliente() + ". Retransmitiendo a su grupo (" + remitente.idGrupo() + ")...");
        // El servidor debe retransmitir el mensaje a los clientes vecinos
        for (InfoCliente vecino : clientesConectados.values()) {
            if (vecino.idGrupo() == remitente.idGrupo() && vecino.idCliente() != remitente.idCliente()) {
                DatagramPacket paqueteReenviado = new DatagramPacket(datosOriginales, longitud, vecino.direccion(), vecino.puerto());
                socket.send(paqueteReenviado);
            }
        }
    }

    private static void gestionarAck(DatagramSocket socket, String[] partes, InfoCliente remitenteAck) throws IOException {
        int idRemitenteOriginal = Integer.parseInt(partes[1]); // El ACK debe decir para quién es
        InfoCliente remitenteOriginal = clientesConectados.get(idRemitenteOriginal);

        if (remitenteOriginal != null) {
            // El servidor devuelve cada reconocimiento al cliente que mandó el mensaje
            String mensajeAckReenviado = "ACK_DE;" + remitenteAck.idCliente();
            byte[] datosAck = mensajeAckReenviado.getBytes();
            DatagramPacket paqueteAck = new DatagramPacket(datosAck, datosAck.length, remitenteOriginal.direccion(), remitenteOriginal.puerto());
            socket.send(paqueteAck);
            System.out.println("  -> Reenviado ACK del Cliente " + remitenteAck.idCliente() + " al Cliente " + idRemitenteOriginal);
        }
        else{
            System.out.println("  -> ERROR: No se encontró el cliente original para el ACK recibido.");
        }
    }
    
    private static InfoCliente buscarClientePorDireccion(InetAddress direccion, int puerto) {
        for (InfoCliente cliente : clientesConectados.values()) {
            if (cliente.direccion().equals(direccion) && cliente.puerto() == puerto) {
                return cliente;
            }
        }
        return null;
    }
}