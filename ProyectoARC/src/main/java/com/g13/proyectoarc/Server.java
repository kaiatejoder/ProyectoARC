package com.g13.proyectoarc;
import java.io.*;
import java.net.*;
import java.util.logging.*;

/**
 * ESTOS SE CONECTAN MEDIANTE TCP
 * Servidor concurrente que acepta múltiples clientes y maneja cada conexión en un hilo separado.
 * Esta clase espera indefinidamente hasta una nueva conexión.
 * Cuando esta ocurre,acepta el socket, muestra un mensaje en consola y crea un nuevo hilo (ServerHilo) para manejar la conexión
 * pasando el socket y un ID del cliente.
 * Incrementa el identificador de sesión por cada conexión.
 * Si ocurre un error al iniciar el servidor, registra el error usando el logger.
 */
public class Server {
    private static int PUERTO = 10578; // Puerto del servidor
    public static void main(String args[]) throws IOException {
        ServerSocket ss;
        System.out.print("Inicializando servidor... ");
        try {
            ss = new ServerSocket(PUERTO); // Crear socket servidor
            System.out.println("\t[OK]");
            int idSession = 0;
            while (true) {
                Socket socket;
                socket = ss.accept();
                System.out.println("Nueva conexión entrante: "+socket);
                ((ServerHilo) new ServerHilo(socket, idSession)).start();
                idSession++;
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "ERROR INICIALIZANDO EL SERVIDOR...", ex);
        }
    }
} 

