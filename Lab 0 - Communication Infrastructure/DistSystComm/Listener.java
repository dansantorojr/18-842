/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 0: Communications Infrastructure
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

    // local send and receive rules and configuration
    private String configFilename;

    // message fields
    private LinkedBlockingQueue<Message> rQueue;
    private LinkedBlockingQueue<Message> rDelayQueue;
    
    // threading fileds
    private LinkedList<Thread> rThreads;

    /**
     * Listener constructor 
     * 
     * @param configFilename - path to the configuration filename
     * @param rQueue - queue for received messages
     * @param rDelayQueue - queue for delayed received messages
     */
    public Listener(int port, String configFilename, 
                        LinkedBlockingQueue<Message> rQueue, 
                        LinkedBlockingQueue<Message> rDelayQueue) {
        this.port = port;
        this.configFilename = configFilename;
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
                Thread receiveThread = new Thread(new Receiver(receiveSocket, configFilename, rQueue, rDelayQueue));
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
