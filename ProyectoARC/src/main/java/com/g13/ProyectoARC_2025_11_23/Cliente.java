package com.g13.ProyectoARC_2025_11_23;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * CLASE PRINCIPAL: Cliente (El Lanzador)
 */
public class Cliente {
    
    public static void main(String[] args) {
        // 1. CONFIGURACIÓN INICIAL
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
        
        escaner.close(); 
        // --- CSV ---
        CSVWriter csvWriter = new CSVWriter();
        System.out.println("\n=== Iniciando " + numeroClientes + " Clientes ===");
        
        // 2. CREACIÓN DE CLIENTES (INSTANCIACIÓN)
        ArrayList<Persona> listaClientes = new ArrayList<>();
        long tiempoInicioSimulacion = System.nanoTime();

        for (int i = 0; i < numeroClientes; i++) {
            listaClientes.add(new Persona(ipServidor, puertoServidor, numeroVecinos, numeroIteraciones));
        }
        
        // 3. EJECUCIÓN DE CLIENTES (START)
        for (Persona cliente : listaClientes) {
            cliente.start();
            try { Thread.sleep(5); } catch (InterruptedException e) {}
        }
        
        System.out.println(">>> Clientes iniciados. Esperando finalización...");
        
        // 4. SINCRONIZACIÓN (JOIN)
        for (Persona cliente : listaClientes) {
            try { cliente.join(); } catch (InterruptedException e) {}
        }
        long tiempoFinSimulacion = System.nanoTime();
        double tiempoTotalSegundos = (tiempoFinSimulacion - tiempoInicioSimulacion) / 1_000_000_000.0;
        
        // 5. REPORTE FINAL DE LA SIMULACIÓN
        int exitosos = 0;
        int fallidos = 0;
        double sumaTiempos = 0;
        int totalTimeouts = 0;
        long mensajesTotales = 0;
        ArrayList<Integer> idsFallidos = new ArrayList<>();

        // Para latencia por grupo
        Map<Integer, Double> sumaGrupo = new HashMap<>();
        Map<Integer, Integer> countGrupo = new HashMap<>();

        for (Persona p : listaClientes) {
            if (p.isFinalizadoConExito()) {
                exitosos++;
                sumaTiempos += p.getTiempoMedioFinal();

                // Latencia por grupo
                int g = p.getIdGrupo();
                sumaGrupo.put(g, sumaGrupo.getOrDefault(g, 0.0) + p.getTiempoMedioFinal());
                countGrupo.put(g, countGrupo.getOrDefault(g, 0) + 1);
            } else {
                fallidos++;
                idsFallidos.add(p.getIdCliente());
            }

            totalTimeouts += p.getCantidadTimeouts();
            mensajesTotales += p.getRespuestasCorrectas();
        }

        // Cálculo de métricas
        double tiempoMedioGlobal = (exitosos > 0) ? (sumaTiempos / exitosos) : 0.0;
        double porcentajeExito = (numeroClientes > 0) ? ((double) exitosos / numeroClientes) * 100.0 : 0.0;

        // Latencia por grupo
        Map<Integer, Double> latenciaMediaGrupo = new HashMap<>();
        for (Integer g : sumaGrupo.keySet()) {
            latenciaMediaGrupo.put(g, sumaGrupo.get(g) / countGrupo.get(g));
        }

        // Throughput
        final int TAM_MENSAJE_BITS = 1024 * 8; // Ajustar según tamaño real de mensajes
        double throughput = (mensajesTotales * TAM_MENSAJE_BITS) / tiempoTotalSegundos;

        // VOLCAR A CSV
        csvWriter.writeLatenciaPorClientes(numeroClientes, tiempoMedioGlobal);

        if (numeroClientes == 100) {
            int numeroGrupos = numeroClientes / numeroVecinos;
            csvWriter.writeLatenciaPorGrupos(numeroGrupos, tiempoMedioGlobal);
        }

        csvWriter.writeThroughput(numeroClientes, throughput);

        // IMPRESIÓN EN CONSOLA
        System.out.println("\n==================================================");
        System.out.println("       REPORTE FINAL DE LA SIMULACIÓN");
        System.out.println("==================================================");
        System.out.printf("Clientes Totales:       %d\n", numeroClientes);
        System.out.printf("Clientes Exitosos:      %d (%.2f%%)\n", exitosos, porcentajeExito);
        System.out.printf("Clientes Fallidos:      %d\n", fallidos);
        if (fallidos > 0) {
            System.out.println(" -> IDs Fallidos: " + idsFallidos);
        }
        System.out.println("--------------------------------------------------");
        System.out.printf("Tiempo Medio Global:    %.4f ms\n", tiempoMedioGlobal);
        System.out.printf("Total Timeouts (UDP):   %d\n", totalTimeouts);
        System.out.printf("Throughput:             %.2f bits/s\n", throughput);
        System.out.println("==================================================");
        String estado = (exitosos == numeroClientes) ? "ÉXITO TOTAL" : "CON ERRORES";
        System.out.println(" ESTADO: " + estado);
        System.out.println("==================================================");
    }
}
