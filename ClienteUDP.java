import java.util.ArrayList;

// Clase principal que lanza múltiples clientes
public class ClienteUDP {
    public static void main(String[] args) {

        //Datos necesarios para poder mandar el mensaje ya que en UDP no hay conexiones establecidas, por lo que cada paquete debe llevar toda la información de enrutamiento.
        String host = "127.0.0.1"; // Dirección del servidor
        int port = 10578; // Puerto del servidor
        int numClientes = 100; // Número de clientes a simular

        // Permitir parámetros por línea de comandos (java ClienteUDP 127.0.0.1 10578 1000)
        if (args.length >= 1) { // IP o hostname del servidor
            host = args[0];
        }
        if (args.length >= 2) { // Puerto del servidor
            port = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) { // Número de clientes a simular
            numClientes = Integer.parseInt(args[2]);
        }
        
        System.out.println("=== Iniciando Clientes UDP ===");
        System.out.println("Servidor: " + host + ":" + port);
        System.out.println("Número de clientes: " + numClientes);
        System.out.println();
        
        ArrayList<Thread> clientes = new ArrayList<Thread>();
        
        // Crear los hilos
        for (int i = 0; i < numClientes; i++) {
            clientes.add(new PersonaUDP(i, host, port));
        }
        
        // Iniciar todos los hilos
        for (Thread thread : clientes) {
            thread.start();
            
            // Opcional: pequeña pausa entre clientes
            try {
                Thread.sleep(10);  // 10ms entre cada cliente
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Esperar a que todos terminen
        for (Thread thread : clientes) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("\n=== Todos los clientes han terminado ===");
    }
}