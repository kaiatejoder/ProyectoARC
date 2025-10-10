package com.g13.proyectoarc;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
/**
 * Thread que simula un cliente que se conecta al servidor, envía un saludo y espera una respuesta.
 */
class ClienteThread extends Thread {
    protected Socket sk;
    protected DataOutputStream dos;
    protected DataInputStream dis;
    private int id;
    private static int PUERTO = 10578;
    private static String IP = "127.0.0.1";
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
            dos = new DataOutputStream(sk.getOutputStream());
            dis = new DataInputStream(sk.getInputStream());
            System.out.println(id + " envia saludo");
            dos.writeUTF("hola"); //ENVÍA EL SALUDO POR EL PUERTO DE OUTPUT
            String respuesta = "";
            respuesta = dis.readUTF();//ESPERA LA RESPUESTA POR INPUT 
            System.out.println(id + " Servidor devuelve saludo: " + respuesta);
            //Cierra los puertos y el socket
            dis.close();
            dos.close();
            sk.close();
        } catch (IOException ex) {
            Logger.getLogger(ClienteThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

public class Cliente {
    private static int numeroDeClientes = 5; //Por default
    public Cliente(int numero) {
        Cliente.numeroDeClientes = numero; // Número de clientes a simular
    }
    public static void main(String[] args) {
         // Por defecto, 5 clientes
        ArrayList<Thread> clients = new ArrayList<Thread>();
        for (int i = 0; i < numeroDeClientes; i++) {
            clients.add(new ClienteThread(i));
        }
        for (Thread thread : clients) {
            thread.start();
        }
    }
}
