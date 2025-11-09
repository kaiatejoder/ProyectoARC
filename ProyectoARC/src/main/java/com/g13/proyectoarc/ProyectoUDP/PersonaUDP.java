package com.g13.proyectoarc.ProyectoUDP;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class PersonaUDP extends Thread {
    // Información del servidor y la simulación
    private String ipServidor;
    private int puertoServidor;
    private int V_vecinos, S_iteraciones;

    // Información específica de este cliente (asignada por el servidor)
    private int idCliente, idGrupo;
    private DatagramSocket socket;
    
    // Cola segura para hilos para comunicar ACKs entre el hilo de escucha y el principal
    private final ConcurrentLinkedQueue<Message> acksRecibidos = new ConcurrentLinkedQueue<>();
    
    private volatile boolean grupoHaTerminado = false;

    public PersonaUDP(String ip, int puerto, int v, int s) {
        this.ipServidor = ip;
        this.puertoServidor = puerto;
        this.V_vecinos = v;
        this.S_iteraciones = s;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(21000); // Timeout de 21 segundos
            InetAddress direccionServidor = InetAddress.getByName(ipServidor);

            // FASE 1: REGISTRO
            registrar(direccionServidor);
            
            // ESPERAR SEÑAL DE INICIO
            esperarSenalInicio();

            // Iniciar un hilo de escucha para procesar mensajes entrantes de forma asíncrona
            Thread hiloEscucha = new Thread(this::escucharAlServidor);
            hiloEscucha.start();

            // FASE 2: SIMULACIÓN
            List<Long> tiemposDeRespuesta = new ArrayList<>();
            for (int i = 0; i < S_iteraciones; i++) {
                System.out.println("[Cliente " + idCliente + "] Iniciando ciclo " + (i + 1) + "/" + S_iteraciones);
                acksRecibidos.clear();

                // Genera un mensaje con coordenadas
                String coordsStr = "(" + ThreadLocalRandom.current().nextInt(0, 101) + ")"; // Solo como ejemplo
                Message msgCoords = new Message(
                    Message.messageType.COMPARTIR_COORDENADAS,
                    idCliente,
                    coordsStr,
                    (i + 1) // Iteración actual
                );

                byte[] datosEnvio = serializarMensaje(msgCoords);
                DatagramPacket paqueteEnvio = new DatagramPacket(datosEnvio, datosEnvio.length, direccionServidor, puertoServidor);
                long tiempoInicio = System.nanoTime();
                socket.send(paqueteEnvio);

                // Espera a recibir todos los reconocimientos de los vecinos (V-1)
                esperarAcks();

                long tiempoFin = System.nanoTime(); // Detener temporizador
                long duracion = (tiempoFin - tiempoInicio) / 1_000_000; // milisegundos
                tiemposDeRespuesta.add(duracion);
                System.out.println("[Cliente " + idCliente + "] Ciclo " + (i + 1) + " completado en " + duracion + "ms.");
            }

            // FASE 3: CÁLCULO Y FINALIZACIÓN
            double tiempoPromedio = tiemposDeRespuesta.stream().mapToLong(val -> val).average().orElse(0.0);
            System.out.printf("[Cliente %d] Simulación terminada. Tiempo medio: %.4f ms\n", idCliente, tiempoPromedio);

            Message msgFinalizado = new Message(
                Message.messageType.TIEMPOS_SIMULACION,
                idCliente,
                String.valueOf(tiempoPromedio),
                S_iteraciones
            );

            byte[] datosFinales = serializarMensaje(msgFinalizado);
            socket.send(new DatagramPacket(datosFinales, datosFinales.length, direccionServidor, puertoServidor));
            
            System.out.println("[Cliente " + idCliente + "] Esperando a que los vecinos del grupo terminen...");
            while (!grupoHaTerminado) {
                try {
                    Thread.sleep(250); // Esperar pasivamente
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            System.out.println("[Cliente " + idCliente + "] Grupo terminado. Desconectando.");
            
            // Interrumpimos el hilo de escucha ya que hemos terminado
            hiloEscucha.interrupt(); 
            hiloEscucha.join(1000);

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException | InterruptedException | ClassNotFoundException e) { // <-- ERROR CORREGIDO AQUÍ
            System.err.println("[Cliente " + idCliente + "] Error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void registrar(InetAddress direccionServidor) throws IOException, ClassNotFoundException {
        
        // 1. Crear el mensaje de registro
        Message msgRegistro = new Message(
            Message.messageType.INICIALIZAR_IDCLIENTE,
            -1, // ID desconocido, el servidor lo asignará
            "", // No se necesita mensaje
            0
        );

        // 2. Serializar y enviar
        byte[] mensajeRegistro = serializarMensaje(msgRegistro);
        DatagramPacket paqueteRegistro = new DatagramPacket(mensajeRegistro, mensajeRegistro.length, direccionServidor, puertoServidor);
        socket.send(paqueteRegistro);

        // 3. Esperar y deserializar la respuesta
        byte[] bufer = new byte[4096];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufer, bufer.length);
        socket.receive(paqueteRespuesta);

        Message msgRespuesta = deserializarMensaje(paqueteRespuesta.getData(), paqueteRespuesta.getLength());

        // 4. Procesar la respuesta
        if (msgRespuesta.type == Message.messageType.INICIALIZAR_IDCLIENTE) {
            this.idCliente = msgRespuesta.idCliente;
            this.V_vecinos = Integer.parseInt(msgRespuesta.mensaje); // V viene en 'mensaje'
            this.idGrupo = msgRespuesta.numIteracion; // El Grupo viene en 'numIteracion'
            System.out.println("[Cliente " + idCliente + "] Registrado en Grupo " + idGrupo);
        } else {
            throw new IOException("Error: Respuesta inesperada del servidor durante el registro.");
        }
    }
    
    private void esperarSenalInicio() throws IOException, ClassNotFoundException {
        byte[] bufer = new byte[4096];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufer, bufer.length);
        socket.receive(paqueteRespuesta); // Espera la señal

        Message msg = deserializarMensaje(paqueteRespuesta.getData(), paqueteRespuesta.getLength());

        if (msg.type == Message.messageType.INICIAR_SIMULACION) {
            System.out.println("[Cliente " + idCliente + "] Señal de inicio recibida.");
        } else {
            throw new IOException("Señal de inicio inesperada: " + msg.type);
        }
    }

    private void esperarAcks() {
        long inicioEspera = System.currentTimeMillis();
        // El ciclo termina cuando el cliente recibe los reconocimientos de todos los vecinos
        while (acksRecibidos.size() < (V_vecinos - 1)) {
            try {
                Thread.sleep(50); // Pequeña pausa para no consumir 100% de CPU
                if (System.currentTimeMillis() - inicioEspera > 20000) { // Timeout de 20 segundos
                    System.err.println("[Cliente " + idCliente + "] TIMEOUT: No se recibieron todos los ACKs a tiempo.");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    // Este método corre en un hilo separado y su única función es escuchar
    private void escucharAlServidor() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] bufer = new byte[4096];
                DatagramPacket paqueteRecibido = new DatagramPacket(bufer, bufer.length);
                socket.receive(paqueteRecibido);

                // 1. Deserializar CUALQUIER mensaje entrante
                Message msgRecibido = deserializarMensaje(paqueteRecibido.getData(), paqueteRecibido.getLength());

                // 2. Actuar según el tipo
                switch (msgRecibido.type) {
                    case COMPARTIR_COORDENADAS:
                        // Es un vecino, contestamos con ACK
                        Message msgAck = new Message(
                            Message.messageType.ACK,
                            this.idCliente, // Yo (el vecino)
                            String.valueOf(msgRecibido.idCliente), // Para el emisor original
                            msgRecibido.numIteracion
                        );
                        byte[] datosAck = serializarMensaje(msgAck);
                        DatagramPacket paqueteAck = new DatagramPacket(datosAck, datosAck.length, paqueteRecibido.getAddress(), paqueteRecibido.getPort());
                        socket.send(paqueteAck);
                        break;

                    case ACK:
                        // Es un ACK para nosotros
                        acksRecibidos.add(msgRecibido);
                        break;

                    case GROUP_DONE:
                        // El servidor nos dice que podemos morir
                        grupoHaTerminado = true;
                        break; // Salir del bucle y terminar el hilo

                    default:
                        // Ignorar otros tipos (INICIAR_SIMULACION, etc.)
                        break;
                }

            } catch (IOException e) {
                if (socket.isClosed() || Thread.currentThread().isInterrupted()) {
                    break; // Salir si el socket se cierra o nos interrumpen
                }
            } catch (ClassNotFoundException e) {
                System.err.println("[Cliente " + idCliente + "] Error al deserializar mensaje: " + e.getMessage());
            }
        }
    }
    
    /** Serializa un objeto Message a un array de bytes. */
    private byte[] serializarMensaje(Message msg) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        oos.flush();
        return bos.toByteArray();
    }

    /** Deserializa un array de bytes de vuelta a un objeto Message. */
    private Message deserializarMensaje(byte[] datos, int longitud) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(datos, 0, longitud);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Message) ois.readObject();
    }
}