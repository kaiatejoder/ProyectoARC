/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.g13.proyectoarc;
import java.io.*;
import java.net.*;
import java.util.logging.*;
/**
 * ServerHilo
 * Hilo que maneja las comunicaciones con un único cliente.
 * Recibe mensajes del cliente y responde según la acción solicitada.
 */
public class ServerHilo extends Thread {
    private Socket socket; // Socket del cliente conectado
    private DataOutputStream dos; // Flujo de salida (ENVÍA LOS DATOS)
    private DataInputStream dis;    // Flujo de entrada (RECIBE LOS DATOS)
    private int sesId;
    /** 
     * Constructor del hilo del servidor. Establece los flujos Input/Output
     * @param socket Socket del cliente conectado
     * @param id Identificador de la sesión
    */
    public ServerHilo(Socket socket, int id) {
        this.socket = socket;
        this.sesId = id;
    
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(ServerHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * "Destructor" de clase. Desconecta el socket del cliente.
     */
    public void desconnectar() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    /**
     * Run.
     * Bucle principal del hilo. Espera mensajes del cliente y responde según la acción.
     */
    public void run() {
        String accion = "";
        try {
            accion = dis.readUTF(); // lee el puerto de Input
            if(accion.equals("hola")){
                System.out.println("El cliente con idSesion "+this.sesId+" saluda");
                dos.writeUTF("adios");
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerHilo.class.getName()).log(Level.SEVERE, null, ex);
        }
        desconnectar();
    }
} 
