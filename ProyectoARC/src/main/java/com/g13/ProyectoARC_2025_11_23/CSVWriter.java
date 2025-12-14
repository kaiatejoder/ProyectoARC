package com.g13.ProyectoARC_2025_11_23;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CSVWriter {

    private static final String DIR = "data";

    private static final String LAT_CLIENTES = DIR + "/LatenciaporClientes.csv";
    private static final String LAT_GRUPOS   = DIR + "/LatenciaporGVecinos.csv";
    private static final String THROUGHPUT   = DIR + "/ThroughPutPorClientes.csv";

    public CSVWriter() {
        crearDirectorio();
        initFile(LAT_CLIENTES, "NumeroClientes,LatenciaMediaMs");
        initFile(LAT_GRUPOS, "NumeroGrupos,LatenciaMediaMs");
        initFile(THROUGHPUT, "NumeroClientes,BitsPorSegundo");
    }

    private void crearDirectorio() {
        File dir = new File(DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void initFile(String path, String header) {
        File file = new File(path);
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append(header).append("\n");
            } catch (IOException e) {
                System.err.println("Error creando CSV " + path);
            }
        }
    }

    /* ================== ESCRITURAS ================== */

    public synchronized void writeLatenciaPorClientes(int numClientes, double latenciaMediaMs) {
        writeLine(LAT_CLIENTES, numClientes + "," + latenciaMediaMs);
    }

    public synchronized void writeLatenciaPorGrupos(int numGrupos, double latenciaMediaMs) {
        writeLine(LAT_GRUPOS, numGrupos + "," + latenciaMediaMs);
    }

    public synchronized void writeThroughput(int numClientes, double bitsPorSegundo) {
        writeLine(THROUGHPUT, numClientes + "," + bitsPorSegundo);
    }

    private void writeLine(String path, String line) {
        try (FileWriter writer = new FileWriter(path, true)) {
            writer.append(line).append("\n");
        } catch (IOException e) {
            System.err.println("Error escribiendo en CSV " + path);
        }
    }
}
