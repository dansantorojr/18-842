/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * LoggerReceiver.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.Rule.Action;

// import utility packages
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

// import network packages
import java.net.Socket;
import java.net.InetAddress;

// import io packages
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * LoggerReciever implements Runnable
 * 
 * Description:
 *  - This object is created when the LoggerListener object detects an incomming connection.
 *  - This uses the input socket to receive incomming messages.
 */
public class LoggerReceiver implements Runnable {
    // network fields
    private Socket socket;
    private InetAddress incomingAddress;
    private boolean senderDead;
    
    // message/node fields
    private LinkedBlockingQueue<TimeStampedMessage> rQueue;
    private TimeStampedMessage message = null;
    
    // input stream field
    private ObjectInputStream incoming;
    
    // verbose flag
    boolean verbose = false;

    /**
     * LoggerReceiver constructor
     * 
     * @param sock - socket through which messages will be received
     * @param rQueue 0 syncronized queue of incoming messages.
     * @param verbose - verbose flag
     */
    public LoggerReceiver(Socket socket, LinkedBlockingQueue<TimeStampedMessage> rQueue, boolean verbose) {
        this(socket, rQueue);
        this.verbose = verbose;
    }
    
    /** 
     * LoggerReceiver constructor
     * 
     * @param socket - socket through which messages will be received
     * @param rQueue - syncronized queue of incoming messages.
     */
    public LoggerReceiver(Socket socket, LinkedBlockingQueue<TimeStampedMessage> rQueue) {
        this.socket = socket;
        this.rQueue = rQueue;
        senderDead = false;
    }

    @Override
    public void run() {
        // create an incoming object stream through which messages can be received
        try {
            incoming = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            incomingAddress = socket.getInetAddress();
        } catch (IOException e) {
            System.err.println("RECEIVER ERROR: Unable to open input stream.");
            System.exit(0);
        }

        // continually try to receive messages through this socket
        while(true) {
            message = null;
            /**
             * Attempt reading from the incoming socket
             *  - if the read is successful it is assumed the sender is not dead
             *  - if the read is unsuccessful, then it is assumed that the sender
             *      is dead. However, we will keep polling input stream to make
             *      sure that the message just wasn't dropped.
             *  - the "senderDead" flag is used in such a way that only prints the
             *      the error string once per disconnect.
             */
            try {
                message = (TimeStampedMessage) incoming.readObject();
                senderDead = false;
            } catch (IOException e) {
                if(!senderDead) {
                    System.out.println("RECEIVER ERROR: Sender disconnected @" + incomingAddress.toString());                    
                }       
                senderDead = true;
                
            } catch (ClassNotFoundException e) {
                System.err.println("RECEIVER ERROR: Object class could not be determined.");
            }            
            
            // only execute if there is a message present
            if(message != null) {
                synchronized(rQueue) {
                    rQueue.add(message);
                }
            }
        }
    }

    public void terminate() throws IOException {
        socket.close();
    }
}
