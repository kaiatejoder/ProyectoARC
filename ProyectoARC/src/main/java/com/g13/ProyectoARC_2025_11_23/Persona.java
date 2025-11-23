package ProyectoHibrido;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CLASE: Persona (El Cliente Inteligente)
 * * OBJETIVO: 
 * Esta clase representa a un usuario individual en la simulación. Es un HILO (Thread) independiente.
 * * LÓGICA HÍBRIDA:
 * Implementa la lógica de conectarse primero por TCP (para asegurar el registro) y luego
 * conmutar a UDP (para la velocidad de la simulación).
 */
public class Persona extends Thread {
    // --- DATOS DE CONEXIÓN ---
    private String ipServidor;
    private int puertoServidor;
    private int V_vecinos, S_iteraciones;

    // --- DATOS DEL CLIENTE ---
    private int idCliente, idGrupo; // Asignados por el servidor.
    private DatagramSocket socketUDP; // El "buzón" para enviar/recibir paquetes rápidos.
    
    // --- SINCRONIZACIÓN ---
    // Cola "Thread-Safe" para recibir los ACKs del hilo de escucha sin bloqueos.
    private final ConcurrentLinkedQueue<Mensaje> acksRecibidos = new ConcurrentLinkedQueue<>();
    
    // "Volatile" asegura que si el hilo de escucha cambia esta variable, el hilo principal
    // se entere inmediatamente (evita caché de CPU). Es el "interruptor de apagado".
    private volatile boolean grupoHaTerminado = false;

    // --- ESTADÍSTICAS (Para el reporte final) ---
    private double tiempoMedioFinal = 0.0;
    private boolean finalizadoConExito = false;
    private int cantidadTimeouts = 0;

    public Persona(String ip, int puerto, int v, int s) {
        this.ipServidor = ip;
        this.puertoServidor = puerto;
        this.V_vecinos = v;
        this.S_iteraciones = s;
    }

    /**
     * MÉTODO PRINCIPAL DEL HILO (Lo que ejecuta el cliente)
     */
    @Override
    public void run() {
        try {
            // 1. PREPARACIÓN UDP
            // Abrimos el socket UDP primero para saber en qué puerto local estamos escuchando.
            // Necesitamos decirle este puerto al servidor durante el registro TCP.
            socketUDP = new DatagramSocket();
            socketUDP.setSoTimeout(3000000); // Si no llega nada en 30s, lanza excepción (Timeout).
            
            int miPuertoUDP = socketUDP.getLocalPort();
            InetAddress direccionServidor = InetAddress.getByName(ipServidor);

            // 2. FASE 1: REGISTRO (TCP)
            // Llamamos a un método auxiliar que gestiona la conexión fiable TCP.
            registrarPorTCP(direccionServidor, miPuertoUDP);
            
            // 3. FASE DE ESPERA (UDP)
            // Ahora esperamos la señal de "Pistoletazo de Salida" por UDP.
            esperarSenalInicio();

            // 4. INICIO DEL HILO DE ESCUCHA (SECUNDARIO)
            // Lanzamos un segundo hilo cuya ÚNICA misión es escuchar mensajes entrantes (ACKs, Coordenadas).
            // Esto permite al hilo principal dedicarse a enviar y medir tiempos sin bloquearse leyendo.
            Thread hiloEscucha = new Thread(this::escucharAlServidor);
            hiloEscucha.start();

            // 5. FASE 2: SIMULACIÓN (BUCLE PRINCIPAL)
            List<Long> tiemposDeRespuesta = new ArrayList<>();
            
            for (int i = 0; i < S_iteraciones; i++) {
                acksRecibidos.clear(); // Limpiamos la bandeja de entrada para el nuevo ciclo.

                // Generamos coordenadas aleatorias (simulación de movimiento).
                String coordsStr = "(" + ThreadLocalRandom.current().nextInt(0, 101) + ")"; 
                
                // Creamos el mensaje.
                Mensaje msjCoords = new Mensaje(
                    Mensaje.messageType.COMPARTIR_COORDENADAS,
                    idCliente,
                    coordsStr,
                    (i + 1)
                );

                // Serializamos (convertimos a bytes) y enviamos.
                byte[] datosEnvio = serializarMensaje(msjCoords);
                DatagramPacket paqueteEnvio = new DatagramPacket(datosEnvio, datosEnvio.length, direccionServidor, puertoServidor);
                
                long tiempoInicio = System.nanoTime(); // Cronómetro ON
                socketUDP.send(paqueteEnvio);

                // Esperamos a tener los ACKs de todos los vecinos (V-1).
                esperarAcks();

                long tiempoFin = System.nanoTime(); // Cronómetro OFF
                long duracion = (tiempoFin - tiempoInicio) / 1_000_000; // Convertir a ms.
                tiemposDeRespuesta.add(duracion);
            }

            // 6. FASE 3: FINALIZACIÓN Y REPORTE
            // Calculamos la media de todos los ciclos.
            double tiempoPromedio = tiemposDeRespuesta.stream().mapToLong(val -> val).average().orElse(0.0);
            this.tiempoMedioFinal = tiempoPromedio;
            
            // Enviamos nuestro resultado final al servidor.
            Mensaje msjFinalizado = new Mensaje(
                Mensaje.messageType.TIEMPOS_SIMULACION,
                idCliente,
                String.valueOf(tiempoPromedio),
                S_iteraciones
            );

            byte[] datosFinales = serializarMensaje(msjFinalizado);
            socketUDP.send(new DatagramPacket(datosFinales, datosFinales.length, direccionServidor, puertoServidor));
            
            // --- ESPERA ACTIVA DE CIERRE DE GRUPO ---
            // No podemos desconectarnos aún. Debemos esperar a que el servidor nos diga (GROUP_DONE)
            // que todos nuestros vecinos han terminado también. Si nos vamos antes, dejamos a los vecinos colgados.
            while (!grupoHaTerminado) {
                try {
                    Thread.sleep(250); // Espera pasiva para no quemar CPU.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Si salimos del bucle, es que recibimos GROUP_DONE.
            // Apagamos el hilo de escucha y cerramos.
            hiloEscucha.interrupt(); 
            hiloEscucha.join(1000);
            
            this.finalizadoConExito = true; // ¡Éxito!

        } catch (Exception e) {
            System.err.println("[Cliente " + idCliente + "] Error Fatal: " + e.getMessage());
            this.finalizadoConExito = false;
        } finally {
            if (socketUDP != null && !socketUDP.isClosed()) {
                socketUDP.close();
            }
        }
    }

    /**
     * Método auxiliar para el Registro TCP.
     * Es sincrónico y bloqueante (espera respuesta) porque es crítico.
     */
    private void registrarPorTCP(InetAddress direccionServidor, int miPuertoUDP) throws IOException {
        Socket socketTCP = new Socket(direccionServidor, puertoServidor);
        DataOutputStream dos = new DataOutputStream(socketTCP.getOutputStream());
        DataInputStream dis = new DataInputStream(socketTCP.getInputStream());

        // Enviamos nuestro puerto UDP para que el servidor sepa dónde contestarnos.
        dos.writeInt(miPuertoUDP);

        // Recibimos nuestra identidad.
        this.idCliente = dis.readInt();
        this.idGrupo = dis.readInt();
        int vRecibido = dis.readInt(); 
        
        System.out.println("[Cliente " + idCliente + "] Registrado por TCP. Grupo: " + idGrupo);
        socketTCP.close(); // Cerramos TCP inmediatamente.
    }
    
    private void esperarSenalInicio() throws IOException, ClassNotFoundException {
        byte[] bufer = new byte[4096];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufer, bufer.length);
        socketUDP.receive(paqueteRespuesta); 

        Mensaje msj = deserializarMensaje(paqueteRespuesta.getData(), paqueteRespuesta.getLength());

        if (msj.type == Mensaje.messageType.INICIAR_SIMULACION) {
            System.out.println("[Cliente " + idCliente + "] Señal de inicio recibida.");
        } else {
            throw new IOException("Señal de inicio inesperada: " + msj.type);
        }
    }

    /**
     * Bucle de espera de ACKs.
     * Se queda aquí hasta recibir (V-1) confirmaciones o hasta que salta el Timeout.
     */
    private void esperarAcks() {
        long inicioEspera = System.currentTimeMillis();
        // Esperamos hasta tener ACKs de todos los vecinos (V-1).
        while (acksRecibidos.size() < (V_vecinos - 1)) {
            try {
                Thread.sleep(10); // Pequeña pausa para no saturar.
                
                // Si pasan más de 10s, asumimos pérdida de paquetes y continuamos (con error).
                if (System.currentTimeMillis() - inicioEspera > 10000) { 
                    this.cantidadTimeouts++; 
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Lógica del Hilo de Escucha Secundario.
     * Procesa mensajes entrantes y actúa inmediatamente.
     */
    private void escucharAlServidor() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] bufer = new byte[4096];
                DatagramPacket paqueteRecibido = new DatagramPacket(bufer, bufer.length);
                socketUDP.receive(paqueteRecibido);

                Mensaje msjRecibido = deserializarMensaje(paqueteRecibido.getData(), paqueteRecibido.getLength());

                switch (msjRecibido.type) {
                    case COMPARTIR_COORDENADAS:
                        // Si un vecino nos manda coordenadas, le respondemos con un ACK inmediatamente.
                        Mensaje msjAck = new Mensaje(
                            Mensaje.messageType.ACK,
                            this.idCliente, 
                            String.valueOf(msjRecibido.idCliente), 
                            msjRecibido.numIteracion
                        );
                        byte[] datosAck = serializarMensaje(msjAck);
                        DatagramPacket paqueteAck = new DatagramPacket(datosAck, datosAck.length, paqueteRecibido.getAddress(), paqueteRecibido.getPort());
                        socketUDP.send(paqueteAck);
                        break;

                    case ACK:
                        // Si recibimos un ACK, lo guardamos en la cola para que el hilo principal lo cuente.
                        acksRecibidos.add(msjRecibido);
                        break;

                    case GROUP_DONE:
                        // Señal de apagado recibida. Cambiamos el flag para liberar al hilo principal.
                        grupoHaTerminado = true;
                        break; 

                    default:
                        break;
                }
            } catch (IOException e) {
                // Es normal que salte al cerrar el socket al final.
                if (socketUDP.isClosed() || Thread.currentThread().isInterrupted()) break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
    // --- MÉTODOS DE SERIALIZACIÓN (Convertir Objeto <-> Bytes) ---
    private byte[] serializarMensaje(Mensaje msj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msj);
        return bos.toByteArray();
    }

    private Mensaje deserializarMensaje(byte[] datos, int longitud) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(datos, 0, longitud);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Mensaje) ois.readObject();
    }

    // --- GETTERS PARA ESTADÍSTICAS ---
    public double getTiempoMedioFinal() { return tiempoMedioFinal; }
    public boolean isFinalizadoConExito() { return finalizadoConExito; }
    public int getCantidadTimeouts() { return cantidadTimeouts; }
    public int getIdCliente() { return idCliente; }
}