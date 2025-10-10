package com.g13.proyectoarc.Client;
import com.g13.proyectoarc.Mensaje.Message;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;

/**
 * Thread que simula un cliente que se conecta al servidor, envía un saludo y espera una respuesta.
 */
class ClienteThread extends Thread {
    protected Socket sk;
    protected ObjectOutputStream dos;
    protected ObjectInputStream dis;
    private int id;
    private Message m;
    private static int PUERTO = 10578;
    private static String IP = "127.0.0.1";
    private int i = 0, num_iteraciones = 1;
    /**
     * Constructor de la clase 
     * @param id Identificador del hilo
     */
    public ClienteThread(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        try {
            sk = new Socket(IP, PUERTO);
            dos = new ObjectOutputStream(sk.getOutputStream());
            dis = new ObjectInputStream(sk.getInputStream());
            int iter = 1, ack = 0;
            long t_ini = 0, t_fin;
            float media = 0;
            while (iter < i) {
                m = (Message) dis.readObject();
                String action = m.type.toString();
                switch(action){
                    case "INICIAR_SIMULACION" -> t_ini = generarCoordenadas(iter);
                    case "INICIALIZAR_IDCLIENTE" -> {
                        id = m.idCliente;
                        i = Integer.valueOf(m.mensaje);
                        num_iteraciones = m.numIteracion;
                    }
                    case "COMPARTIR_COORDENADAS" -> dos.writeObject(new Message(Message.messageType.ACK, id, Integer.toString(m.idCliente), m.numIteracion));
                    //System.out.println("Coordenadas " + m.mensaje + " recibidas del cliente " + m.idCliente + " de la iteracion " + m.numIteracion);
                    case "ACK" -> {
                        if (m.numIteracion == iter) {
                            ack++;
                        }
                        if (ack == i - 1) {
                            ack = 0;
                            iter++;
                            t_fin = System.currentTimeMillis();
                            media += (t_fin - t_ini);
                            System.out.println("Cliente " + id + " ha finalizado la iteracion " + iter + " en " + (t_fin - t_ini) + " ms");
                            if (iter == num_iteraciones) {
                                media = media / num_iteraciones;
                                System.out.println("Cliente " + id + " ha finalizado todas las iteraciones. Tiempo medio: " + media + " ms");
                                dos.writeObject(new Message(Message.messageType.TIEMPOS_SIMULACION, id, Float.toString(media), 0));
                            } else {
                                dos.writeObject(new Message(Message.messageType.INICIAR_SIMULACION, id, "", iter));
                            }
                        }
                    }
                }
               
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, "HA FALLADO AQUI", ex);
        }
    }
    private long generarCoordenadas(int iteracion) throws IOException {
        int x, y, z;
        x = (int) (Math.random() * 100);
        y = (int) (Math.random() * 100);
        z = (int) (Math.random() * 100);

        dos.writeObject(new Message(Message.messageType.COMPARTIR_COORDENADAS, id, "(" + x + "," + y + "," + z + ")", iteracion));
        dos.flush();
        return System.currentTimeMillis();
    }
}

public class Cliente {
    private static int numeroDeClientes = 4; //Por default
    public Cliente(int numero) {
        Cliente.numeroDeClientes = numero; // Número de clientes a simular
    }
    public static void main(String[] args) {
         // Por defecto, 5 clientes
        ArrayList<Thread> clients = new ArrayList<>();
        for (int i = 0; i < numeroDeClientes; i++) {
            clients.add(new ClienteThread(i));
        }
        for (Thread thread : clients) {
            thread.start();
        }
    }
}
