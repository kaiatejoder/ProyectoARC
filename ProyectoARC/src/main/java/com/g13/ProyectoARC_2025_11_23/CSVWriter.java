/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.g13.ProyectoARC_2025_11_23;

import java.io.FileWriter;
import java.io.IOException;

public class CSVWriter {

    private String filePath;

    // Constructor que recibe la ruta del archivo CSV
    public CSVWriter(String filePath) {
        this.filePath = filePath;
    }
// Método que escribe una línea en el archivo CSV
    public void initcsv() {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.append("");
            writer.append("\n"); // nueva línea
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo CSV: " + e.getMessage());
        }
    }
    // Método que escribe una línea en el archivo CSV
    public void writecsv(String data) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.append(data);
            writer.append("\n"); // nueva línea
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo CSV: " + e.getMessage());
        }
    }
}
