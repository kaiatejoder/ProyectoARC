package com.g13.proyectoarc.Mensaje;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;

/**
 * Mensaje. Esta clase representa un mensaje que se envía entre el cliente y el servidor.
 * Implementa Serializable para permitir la serialización del objeto (Enviarlo por sockets)
 * Contiene un enum messageType que define los diferentes tipos de mensajes que se pueden 
 * enviar.
 * TO-DO: Esto incluye métodos para serializar y deserializar el objeto Mensaje como 
 * DataOutputStream/DataInputStream, los cuales pasan directamente a bytes para transmisión UDP y TCP.
 * @author Carla Terol
 * @version 1.0
 */
public class Message {
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
     * 
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
        return "Mensaje{" + "idCliente= " + this.idCliente + ", Mensaje= " + this.mensaje + ", numIteracion= " + this.numIteracion + ", tipoMensaje= " + type + '}';
    }
    public DataOutputStream serialize(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(this.idCliente);
            dos.writeUTF(type.name());
            dos.writeUTF(this.mensaje);
            dos.writeInt(this.numIteracion);
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dos;
    }
    public Message deserialize(DataInputStream dis){
        try {
            int m = dis.readInt();
            String s = dis.readUTF();
            int n = dis.readInt();
            String t = dis.readUTF();
            Message data = new Message(messageType.valueOf(s), m, t, n);
            return data;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Obtiene el tipo de mensaje.
     * @return El tipo de mensaje (messageType)
     */
    public messageType getType() {
        return this.type;
    }
    /**
     * Obtiene el ID del cliente.
     * @return El ID del cliente
     */
    public int getId() {
        return this.idCliente;
    }
    /**
     * Obtiene el tipo de mensaje.
     * @return El tipo de mensaje (messageType)
     */
    public String getBody() {
        return this.mensaje;
    }
    /**
     * Obtiene el número de iteración.
     * @return El número de iteración
     */
    public int getI() {
        return this.numIteracion;
    }
    
}
