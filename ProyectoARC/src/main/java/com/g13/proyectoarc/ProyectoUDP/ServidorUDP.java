package com.g13.proyectoarc.ProyectoUDP;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class ServidorUDP {

    // Estructura para almacenar la información de cada cliente conectado.
    private record InfoCliente(int idCliente, int idGrupo, InetAddress direccion, int puerto) {}

    // Mapa para almacenar a los clientes conectados de forma segura para hilos.
    private static Map<Integer, InfoCliente> clientesConectados = new ConcurrentHashMap<>();
    private static Map<Integer, Integer> finalizadosPorGrupo = new ConcurrentHashMap<>();
    
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
                byte[] bufer = new byte[4096]; // Buffer aumentado
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socketUDP.receive(peticion);

                try {
                    // 1. Deserializar el mensaje de registro
                    Message msgRegistro = deserializarMensaje(peticion.getData(), peticion.getLength());

                    // El cliente envía "INICIALIZAR_IDCLIENTE" para registrarse
                    if (msgRegistro.type != Message.messageType.INICIALIZAR_IDCLIENTE) {
                        System.out.println("Se recibió un mensaje inesperado. Esperando registro.");
                        i--; // No contar esta iteración
                        continue;
                    }

                    InetAddress direccionCliente = peticion.getAddress();
                    int puertoCliente = peticion.getPort();

                    // 2. Asignar IDs
                    int idCliente = i;
                    int idGrupo = i / V;

                    InfoCliente nuevoCliente = new InfoCliente(idCliente, idGrupo, direccionCliente, puertoCliente);
                    clientesConectados.put(idCliente, nuevoCliente);
                    System.out.println("  -> Cliente " + idCliente + " (Grupo " + idGrupo + ") registrado desde " + direccionCliente + ":" + puertoCliente);

                    // 3. Serializar y enviar la respuesta
                    Message msgRespuesta = new Message(
                        Message.messageType.INICIALIZAR_IDCLIENTE,
                        idCliente,
                        String.valueOf(V), // Enviar V en el campo mensaje
                        idGrupo            // Enviar idGrupo en numIteracion
                    );

                    byte[] datosRespuesta = serializarMensaje(msgRespuesta);
                    DatagramPacket paqueteRespuesta = new DatagramPacket(datosRespuesta, datosRespuesta.length, direccionCliente, puertoCliente);
                    socketUDP.send(paqueteRespuesta);

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    i--; // No contar esta iteración
                }
            }
            System.out.println("Registro completo. Todos los " + N + " clientes están conectados.");

            // =================================================================
            // FASE 2: SIMULACIÓN
            // =================================================================
            System.out.println("\nFASE DE SIMULACIÓN: Enviando señal de inicio...");
            
            // Enviar Objeto Message
            for (InfoCliente cliente : clientesConectados.values()) {
                Message msgInicio = new Message(
                    Message.messageType.INICIAR_SIMULACION,
                    cliente.idCliente(),
                    "", 
                    0
                );
                
                byte[] datosInicio = serializarMensaje(msgInicio);
                DatagramPacket paqueteInicio = new DatagramPacket(datosInicio, datosInicio.length, cliente.direccion(), cliente.puerto());
                socketUDP.send(paqueteInicio);
            }
            System.out.println("Simulación iniciada. Procesando mensajes...");

            int clientesFinalizados = 0;
            Map<Integer, Double> tiemposPorGrupo = new HashMap<>();
            double tiempoTotalSistema = 0.0;

            // Bucle principal para procesar mensajes de la simulación
            while (clientesFinalizados < N) {
                byte[] bufer = new byte[4096]; // Buffer aumentado
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socketUDP.receive(peticion);

                try {
                    // 1. Deserializar el mensaje
                    Message msgRecibido = deserializarMensaje(peticion.getData(), peticion.getLength());
                    Message.messageType comando = msgRecibido.type;

                    InfoCliente clienteRemitente = buscarClientePorDireccion(peticion.getAddress(), peticion.getPort());
                    if (clienteRemitente == null) continue; // Ignorar si el cliente no está registrado

                    // 2. Usar el enum en el switch
                    switch (comando) {
                        case COMPARTIR_COORDENADAS: // Un cliente envía sus coordenadas
                            // Reenviar los bytes originales para eficiencia
                            gestionarCoordenadas(socketUDP, peticion.getData(), peticion.getLength(), clienteRemitente);
                            break;
                        case ACK: // Un vecino envía un ACK
                            gestionarAck(socketUDP, msgRecibido, clienteRemitente); // Pasamos el objeto Message
                            break;
                        case TIEMPOS_SIMULACION: // Un cliente ha terminado
                            clientesFinalizados++;
                            double tiempoMedioCliente = Double.parseDouble(msgRecibido.mensaje);
                            tiempoTotalSistema += tiempoMedioCliente;
                            tiemposPorGrupo.merge(clienteRemitente.idGrupo(), tiempoMedioCliente, Double::sum);
                            System.out.println("  -> Cliente " + clienteRemitente.idCliente() + " ha terminado. (" + clientesFinalizados + "/" + N + ")");

                            // --- INICIO LÓGICA DE FIN DE GRUPO ---
                            int finalizadosEsteGrupo = finalizadosPorGrupo.merge(clienteRemitente.idGrupo(), 1, Integer::sum);
                            if (finalizadosEsteGrupo == V) {
                                System.out.println("==== GRUPO " + clienteRemitente.idGrupo() + " HA TERMINADO ====");
                                // Notificar a todos los miembros de este grupo
                                for (InfoCliente cliente : clientesConectados.values()) {
                                    if (cliente.idGrupo() == clienteRemitente.idGrupo()) {
                                        Message msgFinGrupo = new Message(Message.messageType.GROUP_DONE, cliente.idCliente(), "", 0);
                                        byte[] datosFinGrupo = serializarMensaje(msgFinGrupo);
                                        DatagramPacket paqueteFin = new DatagramPacket(datosFinGrupo, datosFinGrupo.length, cliente.direccion(), cliente.puerto());
                                        socketUDP.send(paqueteFin);
                                    }
                                }
                            }
                            // --- FIN LÓGICA DE FIN DE GRUPO ---
                            break;
                        default:
                            // Ignorar otros tipos de mensaje si no se esperan aquí
                            System.out.println("Recibido mensaje inesperado: " + comando);
                            break;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
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
        for (InfoCliente vecino : clientesConectados.values()) {
            if (vecino.idGrupo() == remitente.idGrupo() && vecino.idCliente() != remitente.idCliente()) {
                DatagramPacket paqueteReenviado = new DatagramPacket(datosOriginales, longitud, vecino.direccion(), vecino.puerto());
                socket.send(paqueteReenviado);
            }
        }
    }

    private static void gestionarAck(DatagramSocket socket, Message msgAck, InfoCliente remitenteAck) throws IOException {
        try {
            // El cliente debe poner el ID del emisor original en el campo "mensaje"
            int idRemitenteOriginal = Integer.parseInt(msgAck.mensaje); 
            InfoCliente remitenteOriginal = clientesConectados.get(idRemitenteOriginal);

            if (remitenteOriginal != null) {
                // El servidor devuelve cada reconocimiento al cliente que mandó el mensaje
                Message msgAckReenviado = new Message(
                    Message.messageType.ACK,
                    idRemitenteOriginal, // Destinatario
                    String.valueOf(remitenteAck.idCliente()), // Quién envía el ACK
                    msgAck.numIteracion 
                );
                
                byte[] datosAck = serializarMensaje(msgAckReenviado); // Serializar el nuevo mensaje
                DatagramPacket paqueteAck = new DatagramPacket(datosAck, datosAck.length, remitenteOriginal.direccion(), remitenteOriginal.puerto());
                socket.send(paqueteAck);
            }
            else{
                System.out.println("  -> ERROR: No se encontró el cliente original para el ACK recibido.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: El mensaje ACK no contenía un ID de cliente válido en el campo 'mensaje'.");
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
    
    /** Serializa un objeto Message a un array de bytes. */
    private static byte[] serializarMensaje(Message msg) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        oos.flush();
        return bos.toByteArray();
    }

    /** Deserializa un array de bytes de vuelta a un objeto Message. */
    private static Message deserializarMensaje(byte[] datos, int longitud) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(datos, 0, longitud);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Message) ois.readObject();
    }
}