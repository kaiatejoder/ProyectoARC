import java.util.ArrayList;
import java.util.Scanner;

/**
 * CLASE PRINCIPAL: Cliente (El Lanzador)
 * * OBJETIVO: 
 * Esta clase actúa como el "Director de Orquesta". Su única función es preparar la simulación,
 * crear los hilos que representan a cada usuario (clase Persona) y recopilar los resultados finales.
 * * NO realiza la comunicación de red directamente. Eso lo delega en cada hilo 'Persona'.
 */
public class Cliente {
    
    public static void main(String[] args) {
        // 1. CONFIGURACIÓN INICIAL
        // Usamos Scanner para pedir los parámetros de la simulación por consola.
        // Estos datos deben coincidir con los introducidos en el Servidor para que la lógica de grupos funcione.
        Scanner escaner = new Scanner(System.in);
        System.out.println("--- CONFIGURACIÓN DEL CLIENTE ---");
        
        System.out.print("Introduce la IP (ej. 127.0.0.1): ");
        String ipServidor = escaner.nextLine();
        
        System.out.print("Introduce el puerto (ej. 10578): ");
        int puertoServidor = escaner.nextInt();
        
        System.out.print("Introduce N (Total Clientes): ");
        int numeroClientes = escaner.nextInt();
        
        System.out.print("Introduce V (Vecinos): ");
        int numeroVecinos = escaner.nextInt();
        
        System.out.print("Introduce S (Iteraciones): ");
        int numeroIteraciones = escaner.nextInt();
        
        escaner.close(); // Cerramos el scanner para liberar recursos.

        System.out.println("\n=== Iniciando " + numeroClientes + " Clientes ===");
        
        // 2. CREACIÓN DE CLIENTES (INSTANCIACIÓN)
        // Creamos una lista para guardar referencias a todos los hilos 'Persona'.
        // Es vital guardar estas referencias para poder leer sus estadísticas (éxito/fracaso) al final.
        ArrayList<Persona> listaClientes = new ArrayList<>();
        
        for (int i = 0; i < numeroClientes; i++) {
            // Creamos una nueva 'Persona' (que es un Thread) pasándole los datos de conexión.
            // NOTA: Al hacer 'new', el hilo se crea pero AÚN NO ARRANCA.
            listaClientes.add(new Persona(ipServidor, puertoServidor, numeroVecinos, numeroIteraciones));
        }
        
        // 3. EJECUCIÓN DE CLIENTES (START)
        // Recorremos la lista y arrancamos cada hilo.
        for (Persona cliente : listaClientes) {
            cliente.start(); // Llama al método 'run()' de la clase Persona en un proceso paralelo.
            
            try { 
                // Introducimos una pausa minúscula (5ms) entre arranques.
                // ¿POR QUÉ? Para evitar saturar el sistema operativo intentando abrir
                // N conexiones TCP en el mismo milisegundo exacto (protección contra "Connection Refused").
                Thread.sleep(5); 
            } catch (InterruptedException e) {}
        }
        
        System.out.println(">>> Clientes iniciados. Esperando finalización...");
        
        // 4. SINCRONIZACIÓN (JOIN)
        // El hilo principal (main) debe esperar a que los N clientes terminen su trabajo.
        // El método 'join()' bloquea al main hasta que el hilo 'cliente' muere.
        for (Persona cliente : listaClientes) {
            try { 
                cliente.join(); 
            } catch (InterruptedException e) {}
        }
        
        // Si llegamos aquí, significa que TODOS los hilos (Persona) han terminado (bien o mal).
        
        // ============================================================
        // 5. REPORTE FINAL DE LA SIMULACIÓN (DASHBOARD)
        // ============================================================
        // Recorremos la lista de clientes (que ahora están "muertos" pero guardan sus datos en memoria)
        // para calcular estadísticas globales.
        
        System.out.println("\n==================================================");
        System.out.println("       REPORTE FINAL DE LA SIMULACIÓN");
        System.out.println("==================================================");
        
        int exitosos = 0;
        int fallidos = 0;
        double sumaTiempos = 0;
        int totalTimeouts = 0;
        ArrayList<Integer> idsFallidos = new ArrayList<>();

        for (Persona p : listaClientes) {
            // Verificamos si el cliente terminó correctamente su ciclo completo.
            if (p.isFinalizadoConExito()) {
                exitosos++;
                sumaTiempos += p.getTiempoMedioFinal(); // Acumulamos para la media global.
            } else {
                fallidos++;
                idsFallidos.add(p.getIdCliente()); // Guardamos el ID del que falló para depurar.
            }
            // Sumamos los timeouts (paquetes perdidos que requirieron espera) de todos, exitosos o no.
            totalTimeouts += p.getCantidadTimeouts();
        }

        // Cálculo de la media global (evitando división por cero si nadie tuvo éxito).
        double tiempoMedioGlobal = (exitosos > 0) ? (sumaTiempos / exitosos) : 0.0;
        double porcentajeExito = (numeroClientes > 0) ? ((double) exitosos / numeroClientes) * 100 : 0.0;

        // IMPRESIÓN DE RESULTADOS
        System.out.printf("Clientes Totales:       %d\n", numeroClientes);
        System.out.printf("Clientes Exitosos:      %d (%.2f%%)\n", exitosos, porcentajeExito);
        System.out.printf("Clientes Fallidos:      %d\n", fallidos);
        
        if (fallidos > 0) {
            System.out.println(" -> IDs Fallidos: " + idsFallidos);
        }
        
        System.out.println("--------------------------------------------------");
        System.out.printf("Tiempo Medio Global:    %.4f ms\n", tiempoMedioGlobal);
        System.out.printf("Total Timeouts (UDP):   %d\n", totalTimeouts);
        // Los Timeouts UDP indican congestión en la red, pero no necesariamente fracaso de la simulación.
        
        String estado = (exitosos == numeroClientes) ? "ÉXITO TOTAL" : "CON ERRORES";
        System.out.println("==================================================");
        System.out.println(" ESTADO: " + estado);
        System.out.println("==================================================");
    }
}