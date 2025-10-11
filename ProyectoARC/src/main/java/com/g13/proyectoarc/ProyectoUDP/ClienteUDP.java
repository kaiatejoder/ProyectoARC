import java.util.ArrayList;
import java.util.Scanner;

public class ClienteUDP {
    public static void main(String[] args) {
        Scanner escaner = new Scanner(System.in);
        System.out.println("--- CONFIGURACIÓN DEL CLIENTE ---");
        System.out.print("Introduce la IP del servidor (ej. 127.0.0.1): ");
        String ipServidor = escaner.nextLine();
        System.out.print("Introduce el puerto del servidor (ej. 10578): ");
        int puertoServidor = escaner.nextInt();
        
        // Los parámetros de la simulación deben ser conocidos por el cliente
        // y deben coincidir con los introducidos en el servidor.
        System.out.print("Introduce el número total de clientes (N): ");
        int numeroClientes = escaner.nextInt();
        System.out.print("Introduce el número de vecinos por grupo (V): ");
        int numeroVecinos = escaner.nextInt();
        System.out.print("Introduce el número de iteraciones por cliente (S): ");
        int numeroIteraciones = escaner.nextInt();
        escaner.close();

        System.out.println("\n=== Iniciando " + numeroClientes + " Clientes UDP ===");
        System.out.println("Conectando a: " + ipServidor + ":" + puertoServidor);
        
        ArrayList<Thread> listaHilosClientes = new ArrayList<>();
        
        // Creamos N hilos, donde cada hilo representará a un cliente.
        for (int i = 0; i < numeroClientes; i++) {
            // Pasamos los parámetros de la simulación a cada cliente (hilo).
            listaHilosClientes.add(new PersonaUDP(ipServidor, puertoServidor, numeroVecinos, numeroIteraciones));
        }
        
        // Iniciamos la ejecución de todos los hilos.
        for (Thread hiloCliente : listaHilosClientes) {
            hiloCliente.start();
            try {
                // Pequeña pausa entre el inicio de cada hilo para no saturar la red.
                Thread.sleep(20); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Esperamos a que todos los hilos de los clientes terminen su ejecución.
        for (Thread hiloCliente : listaHilosClientes) {
            try {
                hiloCliente.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("\n=== Todos los clientes han terminado su ejecución ===");
    }
}