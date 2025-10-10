package com.g13.proyectoarc.Servidor;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.*;

import com.g13.proyectoarc.Mensaje.Message;


/**
 * ServerHilo
 * Hilo que maneja las comunicaciones con un único cliente.
 * Recibe mensajes del cliente y responde según la acción solicitada.
 */
public class ServerHilo extends Thread {
    private Socket socket; // Socket del cliente conectado
    private ObjectOutputStream dos; // Flujo de salida (ENVÍA LOS DATOS)
    private ObjectInputStream dis;    // Flujo de entrada (RECIBE LOS DATOS)
    private int sesId; // Identificador de la sesión
    private  ArrayList<Float> time; // ArrayList para almacenar tiempos de simulación
    private  ArrayList<ServerHilo> nei; // ArrayList de vecinos (otros clientes)
    int n, s; //Número de clientes e iteraciones
    private  int fin = 0; // Contador de finalizados

    /** 
     * Constructor del hilo del servidor. Establece los flujos Input/Output
     * @param socket Socket del cliente conectado
     * @param id Identificador de la sesión
    */
    public ServerHilo(Socket socket, int id, ArrayList<ServerHilo> vecinos, ArrayList<Float> tiempos) {
        this.socket = socket;
        this.sesId = id;
        this.nei = vecinos;
        this.time = tiempos;
    
        try {
            dos = new ObjectOutputStream(socket.getOutputStream());
            dis = new ObjectInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(ServerHilo.class.getName()).log(Level.SEVERE, "CONEXIÓN FALLIDA GET DIS/DOS", ex);
        }
    }
    /**
     * "Destructor" de clase. Desconecta el socket del cliente.
     */
    public void desconnectar() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerHilo.class.getName()).log(Level.SEVERE, "EL SOCKET NO SE HA CERRADO", ex);
        }
    }
    @Override
    /**
     * Run.
     * Bucle principal del hilo. Espera mensajes del cliente y responde según la acción.
     */
    public void run() {

          Message m;

        try { 
            System.out.println("Cliente " + sesId + " conectado");
            dos.writeObject(new Message(Message.messageType.INICIALIZAR_IDCLIENTE, sesId, "",s));

            if (sesId == 0) //Así solo se imprime una vez
            {
                System.out.println("Todos los clientes conectados. Iniciando simulacion");
            }
            dos.writeObject(new Message(Message.messageType.INICIAR_SIMULACION, sesId, "", 0));
        while (true){
            
            m = (Message) dis.readObject(); // lee el puerto de Input
            System.out.println(m);
            String action = m.type.toString();
            switch(action){
                case "INICIAR_SIMULACION": //Mensaje del cliente para iniciar la simulación
                System.out.println("El cliente con idSesion "+this.sesId+" inicia la simulacion");
                dos.writeObject(new Message(Message.messageType.INICIAR_SIMULACION, sesId, "", 0));
            case "DESCONECTAR": //Mensaje del cliente para desconectarse
                System.out.println("El cliente con idSesion "+this.sesId+" se desconecta");
                dos.writeUTF("adios");
            break;
            case "TIEMPOS_SIMULACION": //Mensaje del cliente con los tiempos de simulación
                System.out.println("El cliente con idSesion "+this.sesId+" ha terminado la simulacion, tiempo medio: "+m.mensaje+" ms");
                actTiempos(Float.valueOf(m.mensaje), m.idCliente/s);
                fin++;
                if(fin==n){
                    System.out.println("Simulacion terminada. Tiempos medios por grupo:");
                    for(int i=0;i<time.size();i++){
                        System.out.println("Grupo "+i+": "+time.get(i)+" ms");
                    }
                    for(ServerHilo sh: nei){
                        sh.sendM(new Message(Message.messageType.DESCONECTAR, sh.sesId, "", 0));
                        sh.desconnectar();
                    }
        }
        break;
            case "COMPARTIR_COORDENADAS": //Mensaje del cliente con las coordenadas generadas
                for(ServerHilo sh: nei){
                    if(sh.sesId!=m.idCliente){
                        Server.enviarACliente(m, sh);
                    }
                }
                break;
            case "ACK": //Mensaje del cliente confirmando recepción de coordenadas
                for(ServerHilo sh: nei){
                    if(sh.sesId==Integer.valueOf(m.mensaje)){
                        Server.enviarACliente(m, sh);
                    }
                }
                break; 
    }}} catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ServerHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
        desconnectar();
    }
    

     public void sendM(Message m) throws IOException {
        synchronized (dos) {
            dos.writeObject(m);
            dos.flush();
        }
    }

    private synchronized void actTiempos(Float tiempo, int idGrupo) {
        Float currentTiempo = time.get(idGrupo);
        fin++;
        fin++;
        if (currentTiempo.isNaN()) {
            time.set(idGrupo, tiempo);
        } else {
            time.set(idGrupo, (currentTiempo + tiempo) / 2);
        }
    }
} 
