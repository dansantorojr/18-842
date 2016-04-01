/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * Sender.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;

// import network packages
import java.net.Socket;

// import IO packages
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Sender - implements Runnable
 * 
 * Description:
 *  - This class monitors the sQueue and the sDelayQueue and determines
 *      when a message can be sent.
 *  - Once a message in the sQueue has been detected, Sender takes all of the messages
 *      in the queue and attempts to send them.
 */
public class Sender implements Runnable {
    // network fields
    private HashMap<String, Socket> socketMap;
    private HashMap<String, ObjectOutputStream> oosMap;

    // message/node fields
    private HashMap<String, Node> nodeMap;
    private LinkedBlockingQueue<TimeStampedMessage> sQueue;
    private LinkedBlockingQueue<TimeStampedMessage> sDelayQueue;
    private LinkedBlockingQueue<TimeStampedMessage> finalSendQueue;

    /**
     * SenderThread constructor
     * 
     * @param nodeMap - hash map of Node objects keyed by name
     * @param sQueue - queue of Message objects to be sent first
     * @param sDelayQueue - queue of Message object to be sent second
     */
    public Sender(HashMap<String, Node> nodeMap, LinkedBlockingQueue<TimeStampedMessage> sQueue, 
            LinkedBlockingQueue<TimeStampedMessage> sDelayQueue) {
        this.nodeMap = nodeMap;
        this.sQueue = sQueue;
        this.sDelayQueue = sDelayQueue;
        socketMap = new HashMap<String, Socket>();
        oosMap = new HashMap<String, ObjectOutputStream>();
    }

    @Override
    public synchronized void run() {
        // conintually run the sender, look for things added to the send queue.
        while(true) {
            if(sQueue.size() > 0) {
                // How many messages are being sent?
                int numMessages = sQueue.size() + sDelayQueue.size();

                finalSendQueue = new LinkedBlockingQueue<TimeStampedMessage>();
                // add all elements of the sQueue to the finalSendQueue
                while(!sQueue.isEmpty()) {
                    finalSendQueue.add(sQueue.poll());
                }
                // add all elements of the sDelayQueue to the finalSendQueue
                while(!sDelayQueue.isEmpty()) {
                    finalSendQueue.add(sDelayQueue.poll());
                }
                
                // Send all messages in the finalSendQueue
                while(!finalSendQueue.isEmpty()) {
                    // message to be send
                    TimeStampedMessage message = finalSendQueue.poll();
                    
                    // get destination information
                    Node destination = nodeMap.get(message.getDestination());
                    String IP = destination.getIP();
                    int port = destination.getPort();
                    
                    /**
                     * Check and see if this destination has been sent a message.
                     *  - if the destination is already in the map, then pull the
                     *      output object stream out of the map and send the message.
                     *  - if the destination is not in the map, then open a socket
                     *      and add the socket and the output stream to their 
                     *      respective maps.
                     */
                    if(!socketMap.containsKey(destination.getName())) {
                        try {
                            // open up a socket and add it to the hashmap
                            Socket socket = new Socket(IP, port);
                            socketMap.put(destination.getName(), socket);
                            // open up an ouput stream and add it to the hash map
                            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                            oosMap.put(destination.getName(), oos);
                        } 
                        catch(Exception e) {
                            System.err.println("SENDER ERROR: Connection to " + destination.getName() + " could not be made.");                             
                        }
                    }

                    // get the correct output stream to send to
                    ObjectOutputStream sendTo = oosMap.get(destination.getName());

                    // send the message
                    try {
                        sendTo.writeObject(message);
                        sendTo.flush();                        
                    }
                    catch (IOException e) {
                        System.err.println("SENDER ERROR: Output stream to " + destination.getName() + " could not be instantiated.");
                    }                    
                }               
            }
        }
    }

    // terminate - close all sockets
    public void terminate() throws IOException {
        for(Socket s : socketMap.values()) {
            s.close();
        }
        return;
    }
}
