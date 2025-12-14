package com.g13.ProyectoARC_2025_11_23;
import java.io.Serializable; // Importación CRÍTICA: Permite convertir objetos en bytes.

/**
 * CLASE: Mensaje (El Protocolo de Comunicación)
 * * * OBJETIVO: 
 * Esta clase define el "formato del paquete" estandarizado que viaja por la red
 * entre el Cliente y el Servidor.
 * Imagina que es un sobre de carta: todos (clientes y servidor) deben usar este 
 * mismo sobre para entenderse.
 * * * CONCEPTOS CLAVE:
 * 
 * 1. Serializable: Implementar esta interfaz es obligatorio para poder enviar
 * una clase compleja (no un simple texto o número) a través de Sockets.
 * Java "congela" (serializa) el objeto en una ristra de ceros y unos, lo envía
 * por el cable, y el receptor lo "descongela" (deserializa) para reconstruir el objeto original.
 */
public class Mensaje implements Serializable {
    
    // El serialVersionUID se usa para verificar que el emisor y el receptor 
    // tienen la misma versión de la clase. Si cambias el código de Mensaje, 
    // es recomendable regenerarlo, aunque Java lo puede calcular automáticamente.
    
    /**
     * ENUM: Tipos de Mensaje (El "Asunto" de la carta)
     * * Este enumerado es fundamental. Sirve para que el receptor sepa QUÉ tiene que hacer
     * con los datos antes de leer el contenido. Actúa como un "switch" lógico.
     */
    public enum messageType {
        // --- FASE 1: GESTIÓN Y CONFIGURACIÓN (TCP) ---
        
        /** El servidor envía esto (por UDP) para ordenar: "¡Ya estamos todos, empezad a calcular!" */
        INICIAR_SIMULACION,     
        
        /** Respuesta del registro TCP. Contiene la ID asignada al cliente y su grupo. */
        INICIALIZAR_IDCLIENTE,  
        
        /** (Opcional) Se usa para comunicar parámetros iniciales como el número de vecinos V. */
        INICIALIZAR_NUMVECINOS, 
        
        /** Aviso de cierre de conexión explícito. */
        DESCONECTAR,            
        
        // --- FASE 2: SIMULACIÓN (Bucle Rápido UDP) ---
        
        /** El mensaje principal de carga. "Yo (Cliente X) estoy en la posición (x,y,z)". */
        COMPARTIR_COORDENADAS,  
        
        /** "Acknowledgment" (Acuse de Recibo). Confirma que un vecino recibió las coordenadas. 
         * Es vital para sincronizar el avance de los ciclos. */
        ACK,                    
        
        // --- FASE 3: CIERRE Y RESULTADOS (UDP Controlado) ---
        
        /** El cliente reporta al final: "He terminado mis iteraciones, mi tiempo medio es X ms". */
        TIEMPOS_SIMULACION,     
        
        /** El servidor autoriza: "Todos en tu grupo han terminado, ya puedes desconectarte". 
         * Evita que un cliente se vaya antes de tiempo y deje colgados a sus vecinos. */
        GROUP_DONE;
    }
    
    // --- DATOS DEL MENSAJE (El contenido del sobre) ---
    
    /** Identificador del cliente implicado (generalmente quien envía el mensaje). */
    public int idCliente;
    
    /** * La carga útil (Payload). Es un String genérico que cambia de significado según el 'type'.
     * - Si es COMPARTIR_COORDENADAS: Contiene "(10, 20, 30)"
     * - Si es ACK: Contiene el ID del destinatario.
     * - Si es TIEMPOS_SIMULACION: Contiene el valor del tiempo "150.5".
     */
    public String mensaje; 
    
    /** * Número de ciclo/iteración al que pertenece este mensaje.
     * IMPORTANTE: Sirve para no mezclar un ACK del ciclo 1 con una coordenada del ciclo 2
     * si llegan desordenados por la red UDP.
     */
    public int numIteracion;
    
    /** El tipo de mensaje (del enum definido arriba). */
    public messageType type;
    
    /**
     * Constructor de la clase Mensaje.
     * @param tipoMensaje El "Asunto" (enum).
     * @param idCliente Quién lo envía.
     * @param mensaje El dato concreto (Payload).
     * @param numIter En qué vuelta del bucle estamos (0 si no aplica).
     */
    public Mensaje(messageType tipoMensaje, int idCliente, String mensaje, int numIter) {
        this.idCliente = idCliente;
        this.mensaje = mensaje;
        this.numIteracion = numIter;
        this.type = tipoMensaje;
    }

    /**
     * Método toString para depuración.
     * Permite hacer System.out.println(miMensaje) y ver su contenido legible en la consola.
     */
    @Override
    public String toString() {
        return "Mensaje{" + "idCliente= " + idCliente + ", Mensaje= " + mensaje + ", numIteracion= " + numIteracion + ", tipoMensaje= " + type + '}';
    }
    
    
}