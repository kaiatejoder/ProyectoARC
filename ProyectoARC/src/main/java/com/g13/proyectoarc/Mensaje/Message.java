package com.g13.proyectoarc.Mensaje;
import java.io.Serializable;

/**
 * Mensaje. Esta clase representa un mensaje que se envía entre el cliente y el servidor.
 * Implementa Serializable para permitir la serialización del objeto (Enviarlo por sockets)
 * Contiene un enum messageType que define los diferentes tipos de mensajes que se pueden 
 * enviar.
 * @author Carla Terol
 * @version 1.0
 */
public class Message implements Serializable {
    public enum messageType {
        INICIAR_SIMULACION, INICIALIZAR_IDCLIENTE, 
        INICIALIZAR_NUMVECINOS, DESCONECTAR, 
        COMPARTIR_COORDENADAS, ACK, TIEMPOS_SIMULACION;
    }
    
    public int idCliente;
    public String mensaje;
    public int numIteracion;
    public messageType type;
    /**
     * Constructor de la clase Mensaje.
     * @param tipoMensaje Tipo de mensaje (definido en el enum messageType)
     * @param idCliente Identificador del cliente que envía el mensaje 
     * @param mensaje Contenido del mensaje
     * @param numIter Número de iteración (si aplica)
     * 
     */
    public Message(messageType tipoMensaje, int idCliente, String mensaje, int numIter) {
        this.idCliente = idCliente;
        this.mensaje = mensaje;
        this.numIteracion = numIter;
        this.type = tipoMensaje;
    }

    @Override
    public String toString() {
        return "Mensaje{" + "idCliente= " + idCliente + ", Mensaje= " + mensaje + ", numIteracion= " + numIteracion + ", tipoMensaje= " + type + '}';
    }
    
}
