import java.io.IOException;
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
    private final ConcurrentLinkedQueue<String> acksRecibidos = new ConcurrentLinkedQueue<>();

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
                String coordenadas = "COORDS;" + idCliente + ";" + ThreadLocalRandom.current().nextInt(0, 101);
                byte[] datosEnvio = coordenadas.getBytes();
                DatagramPacket paqueteEnvio = new DatagramPacket(datosEnvio, datosEnvio.length, direccionServidor, puertoServidor);

                long tiempoInicio = System.nanoTime(); // Iniciar temporizador
                socket.send(paqueteEnvio); // Envía mensaje al servidor

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

            String mensajeFinalizado = "DONE;" + tiempoPromedio;
            socket.send(new DatagramPacket(mensajeFinalizado.getBytes(), mensajeFinalizado.length(), direccionServidor, puertoServidor));
            
            // Interrumpimos el hilo de escucha ya que hemos terminado
            hiloEscucha.interrupt(); 
            hiloEscucha.join(1000);

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException | InterruptedException e) {
            System.err.println("[Cliente " + idCliente + "] Error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void registrar(InetAddress direccionServidor) throws IOException {
        byte[] mensajeRegistro = "REGISTER".getBytes();
        DatagramPacket paqueteRegistro = new DatagramPacket(mensajeRegistro, mensajeRegistro.length, direccionServidor, puertoServidor);
        socket.send(paqueteRegistro);

        byte[] bufer = new byte[1024];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufer, bufer.length);
        socket.receive(paqueteRespuesta);

        String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
        // Formato esperado: REGISTRADO;idCliente;idGrupo;V
        String[] partes = respuesta.split(";");
        this.idCliente = Integer.parseInt(partes[1]);
        this.idGrupo = Integer.parseInt(partes[2]);
        this.V_vecinos = Integer.parseInt(partes[3]);
        System.out.println("[Cliente " + idCliente + "] Registrado en Grupo " + idGrupo);
    }
    
    private void esperarSenalInicio() throws IOException {
        byte[] bufer = new byte[1024];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufer, bufer.length);
        socket.receive(paqueteRespuesta); // Espera la señal
        String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
        if (respuesta.equals("INICIAR_SIMULACION")) {
             System.out.println("[Cliente " + idCliente + "] Señal de inicio recibida.");
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
                byte[] bufer = new byte[1024];
                DatagramPacket paqueteRecibido = new DatagramPacket(bufer, bufer.length);
                socket.receive(paqueteRecibido);

                String mensaje = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                String[] partes = mensaje.split(";");

                if (partes[0].equals("COORDS")) { // Es un mensaje de un vecino
                    int idRemitenteOriginal = Integer.parseInt(partes[1]);
                    // Cuando le llega un mensaje a un vecino, este contesta con un ACK
                    String mensajeAck = "ACK;" + idRemitenteOriginal;
                    byte[] datosAck = mensajeAck.getBytes();
                    DatagramPacket paqueteAck = new DatagramPacket(datosAck, datosAck.length, paqueteRecibido.getAddress(), paqueteRecibido.getPort());
                    socket.send(paqueteAck);
                } else if (partes[0].equals("ACK_DE")) { // Es un ACK para nosotros
                    acksRecibidos.add(mensaje);
                }
            } catch (IOException e) {
                if (socket.isClosed() || Thread.currentThread().isInterrupted()) {
                    break; // Salir del bucle si el socket se cierra o el hilo se interrumpe
                }
            }
        }
    }
}