/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * LoggerListener.java
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
 * LoggerListener implements Runnable
 * 
 * Description:
 *  - LoggerListener listens for incomming connections. When an incoming connection is
 *      detected, the LoggerListener spins off a receiver thread to collect messages from
 *      that sender, and keeps listening.
 */
public class LoggerListener implements Runnable {
    // network fields
    private ServerSocket listenSocket; 
    private int port;

    // message fields
    private LinkedBlockingQueue<TimeStampedMessage> rQueue;

    // threading fileds
    private LinkedList<Thread> rThreads;
    
    private boolean verbose = false;

    /**
     * LoggerListener verbose constructor
     * 
     * @param port - port on which to listen
     * @param rQueue - queue for received messages
     * @param verbose - verbose flag
     */
    public LoggerListener(int port, LinkedBlockingQueue<TimeStampedMessage> rQueue, boolean verbose) {
        this(port, rQueue);
        this.verbose = verbose;
    }
    
    /**
     * Listener constructor 
     * 
     * @param port - port on which to listen
     * @param rQueue - queue for received messages
     */
    public LoggerListener(int port, LinkedBlockingQueue<TimeStampedMessage> rQueue) {
        this.port = port;
        this.rQueue = rQueue;
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
                Thread receiveThread = new Thread(new LoggerReceiver(receiveSocket, rQueue, verbose));
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
