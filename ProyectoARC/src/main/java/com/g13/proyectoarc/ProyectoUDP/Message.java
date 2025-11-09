package com.g13.proyectoarc.ProyectoUDP;

import java.io.Serializable; // Interfaz necesaria para que el objeto pueda ser enviado por red (serializado).

/**
 * Mensaje. Esta clase representa un mensaje que se envía entre el cliente y el servidor.
 * Implementa Serializable para permitir la serialización del objeto (Enviarlo por sockets).
 * Contiene un enum messageType que define los diferentes tipos de mensajes que se pueden 
 * enviar, estandarizando el protocolo de comunicación.
 * */
public class Message implements Serializable {
    
    // El 'serialVersionUID' no está explícito, pero es buena práctica incluirlo para la serialización.
    
    /**
     * Enumeración que define todos los tipos de mensajes posibles en el protocolo.
     */
    public enum messageType {
        INICIAR_SIMULACION,     // Mensaje del servidor al cliente para iniciar una nueva iteración.
        INICIALIZAR_IDCLIENTE,  // Mensaje del servidor para asignar el ID y parámetros iniciales al cliente.
        INICIALIZAR_NUMVECINOS, // Mensaje para establecer el número de vecinos (posiblemente no usado o con alias).
        DESCONECTAR,            // Mensaje para notificar el fin de la conexión o la desconexión.
        COMPARTIR_COORDENADAS,  // Mensaje del cliente al servidor (o reenviado) con las coordenadas generadas.
        ACK,                    // Acknowledge (Acuse de recibo). Confirmación de recepción o procesamiento.
        TIEMPOS_SIMULACION,     // Mensaje del cliente al servidor reportando los tiempos medios de ejecución.
        GROUP_DONE;
    }
    
    public int idCliente;       // Identificador del cliente que origina o al que va dirigido el mensaje.
    public String mensaje;      // Contenido principal del mensaje (p. ej., las coordenadas, o el número total de clientes).
    public int numIteracion;    // Número de la iteración actual de la simulación a la que se refiere el mensaje.
    public messageType type;    // El tipo de mensaje, usando el enum definido arriba.
    
    /**
     * Constructor de la clase Mensaje.
     * @param tipoMensaje Tipo de mensaje (definido en el enum messageType)
     * @param idCliente Identificador del cliente que envía el mensaje 
     * @param mensaje Contenido del mensaje (puede ser un dato, una cadena o un parámetro).
     * @param numIter Número de iteración (0 si no aplica).
     */
    public Message(messageType tipoMensaje, int idCliente, String mensaje, int numIter) {
        this.idCliente = idCliente;
        this.mensaje = mensaje;
        this.numIteracion = numIter;
        this.type = tipoMensaje;
    }

    /**
     * Devuelve una representación en cadena del objeto Message, útil para la depuración (logs).
     * @return Una cadena con los valores de los atributos del mensaje.
     */
    @Override
    public String toString() {
        return "Mensaje{" + "idCliente= " + idCliente + ", Mensaje= " + mensaje + ", numIteracion= " + numIteracion + ", tipoMensaje= " + type + '}';
    }
    
}