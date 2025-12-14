package com.g13.ProyectoARC_2025_11_23;


import com.g13.ProyectoARC_2025_11_23.Mensaje;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*; // Importante: Contiene ExecutorService (Pool de Hilos)

/**
 * CLASE PRINCIPAL: Servidor (Híbrido TCP/UDP)
 * * OBJETIVO:
 * Coordinar toda la simulación. Es responsable de registrar a los clientes de forma segura
 * y de retransmitir sus mensajes lo más rápido posible.
 * * * ARQUITECTURA (Sesión 3):
 * Usa un modelo "Híbrido + Productor-Consumidor".
 * - Híbrido: Usa TCP para registro (seguridad) y UDP para simulación (velocidad).
 * - Productor-Consumidor: El hilo principal recibe paquetes (Productor) y un Pool de Hilos
 * los procesa en paralelo (Consumidores) para evitar cuellos de botella.
 */
public class Servidor {

    // Record simple para guardar los datos de cada cliente registrado.
    private record InfoCliente(int idCliente, int idGrupo, InetAddress direccion, int puerto) {}

    // --- ESTRUCTURAS DE DATOS CONCURRENTES ---
    // Usamos 'ConcurrentHashMap' porque múltiples hilos van a leer/escribir aquí a la vez.
    // Un HashMap normal daría error o corrupción de datos.
    private static Map<Integer, InfoCliente> clientesConectados = new ConcurrentHashMap<>();
    
    // Mapa para contar cuántos clientes de cada grupo han terminado. 
    // Clave: ID de Grupo, Valor: Cantidad de clientes finalizados.
    private static Map<Integer, Integer> finalizadosPorGrupo = new ConcurrentHashMap<>();
    
    private static int N, V, S; // Parámetros de la simulación.
    private static final int PUERTO = 10578;

    // --- THREAD POOL (La Clave del Rendimiento) ---
    // Creamos un equipo fijo de 50 trabajadores.
    // Si llegan 2000 mensajes, no creamos 2000 hilos (eso mataría la CPU).
    // Los 50 trabajadores se van turnando para procesarlos todos.
    private static ExecutorService pool = Executors.newFixedThreadPool(50);

    public static void main(String args[]) {
        // 1. CONFIGURACIÓN
        Scanner escaner = new Scanner(System.in);
        System.out.println("--- CONFIGURACIÓN DEL SERVIDOR HÍBRIDO (TCP+UDP) ---");
        System.out.print("Introduce N (Total clientes): ");
        N = escaner.nextInt();
        System.out.print("Introduce V (Vecinos por grupo): ");
        V = escaner.nextInt();
        System.out.print("Introduce S (Iteraciones): ");
        S = escaner.nextInt();
        
        if (N % V != 0) {
            System.out.println("Error: N debe ser múltiplo de V para que los grupos sean iguales.");
            return;
        }

        try {
            // =================================================================
            // FASE 1: REGISTRO VÍA TCP (Fiabilidad Crítica)
            // =================================================================
            // Usamos TCP aquí porque no podemos permitirnos perder ni un solo registro.
            System.out.println("\n--- FASE 1: REGISTRO (TCP) ---");
            ServerSocket serverSocketTCP = new ServerSocket(PUERTO);
            System.out.println("Esperando a " + N + " clientes en puerto TCP " + PUERTO + "...");

            // Bucle secuencial: Atendemos a los clientes de uno en uno para darles su ID.
            for (int i = 0; i < N; i++) {
                // 'accept()' bloquea hasta que entra un cliente.
                Socket clienteTCP = serverSocketTCP.accept();
                
                // Canales de E/S para hablar con el cliente.
                DataOutputStream dos = new DataOutputStream(clienteTCP.getOutputStream());
                DataInputStream dis = new DataInputStream(clienteTCP.getInputStream());

                // El cliente nos dice en qué puerto UDP va a escuchar luego.
                int puertoUDPCliente = dis.readInt(); 
                InetAddress ipCliente = clienteTCP.getInetAddress();

                // Asignamos ID y Grupo matemáticamente.
                int idCliente = i;
                int idGrupo = i / V; // División entera para agrupar (ej: 0..9 -> Grupo 0).

                // Guardamos la ficha del cliente en memoria.
                InfoCliente nuevo = new InfoCliente(idCliente, idGrupo, ipCliente, puertoUDPCliente);
                clientesConectados.put(idCliente, nuevo);
                System.out.println("  -> Cliente " + idCliente + " registrado (TCP). UDP en: " + puertoUDPCliente);

                // Le enviamos su "DNI" (configuración) por TCP.
                dos.writeInt(idCliente);
                dos.writeInt(idGrupo);
                dos.writeInt(V);
                
                // Cerramos el canal TCP. El registro ha terminado con éxito.
                clienteTCP.close();
            }
            
            serverSocketTCP.close(); // Ya no admitimos más registros.
            System.out.println("Registro TCP completado. Todos los clientes listos.");

            // =================================================================
            // FASE 2: SIMULACIÓN UDP (Velocidad Máxima)
            // =================================================================
            // Abrimos el socket UDP para el bombardeo de datos.
            DatagramSocket socketUDP = new DatagramSocket(PUERTO); 
            System.out.println("\n--- FASE 2: SIMULACIÓN (UDP MULTI-HILO) ---");
            
            // Enviamos la señal de salida a todos (Broadcast manual).
            for (InfoCliente cliente : clientesConectados.values()) {
                Mensaje msjInicio = new Mensaje(Mensaje.messageType.INICIAR_SIMULACION, cliente.idCliente(), "", 0);
                byte[] datos = serializarMensaje(msjInicio);
                socketUDP.send(new DatagramPacket(datos, datos.length, cliente.direccion(), cliente.puerto()));
            }
            System.out.println("Señal de inicio enviada. Procesando mensajes en paralelo...");

            // Variable atómica (segura para hilos) para contar el progreso global.
            java.util.concurrent.atomic.AtomicInteger clientesFinalizados = new java.util.concurrent.atomic.AtomicInteger(0);
            
            // --- BUCLE PRINCIPAL (EL PRODUCTOR) ---
            // La única misión de este bucle es sacar paquetes de la tarjeta de red lo más rápido posible.
            while (clientesFinalizados.get() < N) {
                byte[] bufer = new byte[4096];
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                
                // 1. RECEPCIÓN (Bloqueante pero muy rápida).
                socketUDP.receive(peticion);

                // 2. CLONADO DE DATOS
                // ¡CRÍTICO! Si no copiamos los datos a un array nuevo ('datosCopia'), el siguiente 'receive'
                // sobrescribiría el buffer antes de que el hilo trabajador pudiera leerlo.
                byte[] datosCopia = Arrays.copyOf(peticion.getData(), peticion.getLength());
                int longitud = peticion.getLength();
                InetAddress ip = peticion.getAddress();
                int puerto = peticion.getPort();

                // 3. DESPACHO A WORKER (Pasar la patata caliente)
                // En lugar de procesar el mensaje aquí (que bloquearía la recepción),
                // se lo damos al Thread Pool para que lo haga en segundo plano.
                pool.execute(() -> {
                    try {
                        // --- CÓDIGO DEL TRABAJADOR (CONSUMIDOR) ---
                        // Esto se ejecuta en paralelo en uno de los 50 hilos.
                        Mensaje msj = deserializarMensaje(datosCopia, longitud);
                        InfoCliente remitente = buscarCliente(ip, puerto);
                        
                        // Solo procesamos si el cliente está registrado (seguridad básica).
                        if (remitente != null) {
                            procesarMensaje(socketUDP, msj, remitente, clientesFinalizados);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            // =================================================================
            // FASE 3: CIERRE
            // =================================================================
            pool.shutdown(); // Apagamos los trabajadores.
            socketUDP.close(); // Cerramos el socket.
            System.out.println("\n--- FIN DE LA SIMULACIÓN ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lógica de negocio del servidor.
     * Decide qué hacer con cada mensaje recibido (Reenviar, ACK, Cerrar grupo...).
     */
    private static void procesarMensaje(DatagramSocket socket, Mensaje msj, InfoCliente remitente, java.util.concurrent.atomic.AtomicInteger contadorFin) throws IOException {
        switch (msj.type) {
            case COMPARTIR_COORDENADAS:
                // El servidor actúa como repetidor: envía las coordenadas a todos los miembros del grupo
                // EXCEPTO al que las envió.
                for (InfoCliente vecino : clientesConectados.values()) {
                    if (vecino.idGrupo() == remitente.idGrupo() && vecino.idCliente() != remitente.idCliente()) {
                        byte[] datos = serializarMensaje(msj); 
                        socket.send(new DatagramPacket(datos, datos.length, vecino.direccion(), vecino.puerto()));
                    }
                }
                break;

            case ACK:
                // Reenvío simple de confirmación al destinatario específico.
                int idDestino = Integer.parseInt(msj.mensaje); // El destinatario viene en el contenido.
                InfoCliente destino = clientesConectados.get(idDestino);
                
                if (destino != null) {
                    // Construimos un nuevo ACK indicando quién confirma.
                    Mensaje msjAck = new Mensaje(Mensaje.messageType.ACK, idDestino, String.valueOf(remitente.idCliente()), msj.numIteracion);
                    byte[] datos = serializarMensaje(msjAck);
                    socket.send(new DatagramPacket(datos, datos.length, destino.direccion(), destino.puerto()));
                }
                break;

            case TIEMPOS_SIMULACION:
                // Un cliente ha terminado todas sus iteraciones.
                int terminados = contadorFin.incrementAndGet();
                System.out.println("Cliente " + remitente.idCliente() + " terminó. (" + terminados + "/" + N + ")");
                
                // LÓGICA DE CIERRE DE GRUPO
                // Contamos cuántos de ESTE grupo han terminado.
                int finGrupo = finalizadosPorGrupo.merge(remitente.idGrupo(), 1, Integer::sum);
                
                // Si el contador llega a V (todos los miembros), liberamos el grupo.
                if (finGrupo == V) {
                    System.out.println("Grupo " + remitente.idGrupo() + " FINALIZADO.");
                    Mensaje msjFin = new Mensaje(Mensaje.messageType.GROUP_DONE, 0, "", 0);
                    byte[] datosFin = serializarMensaje(msjFin);
                    
                    // Enviamos la señal de liberación a todos los miembros del grupo.
                    for (InfoCliente c : clientesConectados.values()) {
                        if (c.idGrupo() == remitente.idGrupo()) {
                            socket.send(new DatagramPacket(datosFin, datosFin.length, c.direccion(), c.puerto()));
                        }
                    }
                }
                break;
        }
    }

    // --- MÉTODOS AUXILIARES ---
    
    // Busca un cliente en el mapa usando su IP y Puerto (para saber quién nos habla).
    private static InfoCliente buscarCliente(InetAddress ip, int puerto) {
        for (InfoCliente c : clientesConectados.values()) {
            if (c.direccion().equals(ip) && c.puerto() == puerto) return c;
        }
        return null;
    }

    /* Esta función convierte un objeto Mensaje en un arreglo de bytes */
    private static byte[] serializarMensaje(Mensaje msj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msj);
        return bos.toByteArray();
    }

    /* Esta función saca un objeto Mensaje de un arreglo de bytes */
    private static Mensaje deserializarMensaje(byte[] datos, int len) throws IOException, ClassNotFoundException {
        return (Mensaje) new ObjectInputStream(new ByteArrayInputStream(datos, 0, len)).readObject();
    }
}