/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * Listener.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */
 
// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

// import network packages
import java.net.ServerSocket;
import java.net.Socket;

// import exception packages
import java.io.IOException;


/**
 * Listener implements Runnable
 * 
 * Description:
 *  - Listener listens for incomming connections. When an incoming connection is
 *      detected, the Listener spins off a receiver thread to collect messages from
 *      that sender, and keeps listening.
 */
public class Listener implements Runnable {
    // network fields
    private ServerSocket listenSocket; 
    private int port;

    // message fields
    private LinkedBlockingQueue<TimeStampedMessage> rQueue;
    private LinkedBlockingQueue<TimeStampedMessage> rDelayQueue;
    
    // threading fileds
    private LinkedList<Thread> rThreads;
    
    // verbose flag
    private boolean verbose = false;

    /**
     * Listener verbose constructor
     * 
     * @param port - port on which to listen
     * @param rQueue - queue for received messages
     * @param rDelayQueue - queue for delayed received messages
     * @param verbose - verbose flag
     */
    public Listener(int port, LinkedBlockingQueue<TimeStampedMessage> rQueue, 
                        LinkedBlockingQueue<TimeStampedMessage> rDelayQueue, boolean verbose) {
        this(port, rQueue, rDelayQueue);
        this.verbose = verbose;
    }
    /**
     * Listener constructor 
     * 
     * @param port - port on which to listen
     * @param rQueue - queue for received messages
     * @param rDelayQueue - queue for delayed received messages
     */
    public Listener(int port, LinkedBlockingQueue<TimeStampedMessage> rQueue, 
                        LinkedBlockingQueue<TimeStampedMessage> rDelayQueue) {
        this.port = port;
        this.rQueue = rQueue;
        this.rDelayQueue = rDelayQueue;
        rThreads = new LinkedList<Thread>();

    }

    @Override
    public void run() {
        // attemp to listen
        try {
            // create a new server socket through which to listen
            listenSocket = new ServerSocket(port);
            
            // continually listen
            while(true) {
                // Listening for new incoming connection.
                Socket receiveSocket = listenSocket.accept();

                // Create a new thread for new incoming connection.
                Thread receiveThread = new Thread(new Receiver(receiveSocket, rQueue, rDelayQueue, verbose));
                receiveThread.start();
                rThreads.add(receiveThread); // keep track of all of the spawned threads
            }
        }
        catch (IOException e) {
            System.err.println("LISTENER ERROR: Cannot listen on port" + port + ".");
        }
    }

    // terminate - interrupt all threads, close the listening socket
    public void terminate() throws IOException {
        while(!rThreads.isEmpty()) {
            rThreads.removeFirst().interrupt();
        }
        listenSocket.close();
    }

    @Override
    public String toString() {
        return "Lst[port=" + port + "]";
    }
}
