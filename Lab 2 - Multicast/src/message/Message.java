/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * Message.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

/**
 * Messages implements a serializable object so that it can be sent over TCP.
 */
public class Message implements java.io.Serializable {
    // set serial version - Why? Consistency?
    private static final long serialVersionUID = 1L;

    // native message fields
    private String kind;
    private String source;
    private String destination;
    private int seqNum;
    private Object data;
    
    public Message() {};

    /**
     * Message Constructor
     *
     * @param dest - destination
     * @param kind - type of message
     * @param data - message payload
     */
    public Message(String dest, String kind, Object data) {
        this.destination = dest;
        this.kind = kind;
        this.data = data;
    }
    
    public Message(String kind, Object data) {
        this.kind = kind;
        this.data = data;
    }
    
    /**
     * Message constructor
     * 
     * @param kind - type of message
     * @param src - source of message
     * @param dest - destination of the messages
     * @param seqNum - sequence number of the message
     * @param data - data contained in the message
     */
    public Message(String kind, String src, String dest, int seqNum, Object data) {
        this.kind = kind;
        this.source = src;
        this.destination = dest;
        this.seqNum = seqNum;
        this.data = data;
    }
    
    // KIND methods
    public void setKind(String kind) {
        this.kind = kind;
    }  
    
    public String getKind() {
        return kind;
    }

    // SOURCE methods
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSource() {
        return source;
    }
    
    // DESTINATION methods
    public void setDestination(String destination) {
        this.destination = destination;
    }    
    
    public String getDestination() {
        return destination;
    }

    // SEQNUM methods
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }
    
    public int getSeqNum() {
        return seqNum;
    }

    // DATA METHODS
    public void setData(Object data) {
        this.data = data;
    }
    
    public Object getData() {
        return data;
    }
    
    public boolean isEquivalent(Message message) {
        boolean broadFlag = false;
        boolean narrowFlag = false;
        
        if(this.getKind().equalsIgnoreCase(message.getKind()) &&
            this.getSource().equalsIgnoreCase(message.getSource()) &&
            this.getData().equals(message.getData())) {
                broadFlag = true;
        }
        
        if((message.getData() instanceof MulticastPayload) && broadFlag) {
            narrowFlag = true;
        } else if(this.getDestination().equalsIgnoreCase(message.getDestination()) &&
                    (this.getSeqNum() == message.getSeqNum())) {
            narrowFlag = true;
        }
        return (broadFlag && narrowFlag);
    }

    @Override
    public String toString() {
        return "Msg[seqNum=" + this.getSeqNum() + 
            " , src->dest=" + this.getSource() + "->" + 
            this.getDestination() + ", kind=" + this.getKind() +
            ", payload=" + this.getData() + "]";
    }
}
