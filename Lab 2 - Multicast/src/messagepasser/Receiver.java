/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * Receiver.java
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
 * Receiver implements Runnable
 * 
 * Description:
 *  - This object is created when the Listener object detects an incomming connection.
 *  - This uses the input socket to receive incomming messages.
 *  - All incoming mesages are checked against the receive rules outline in the
 *      configuration file.
 */
public class Receiver implements Runnable {
    // network fields
    private Socket socket;
    private InetAddress incomingAddress;
    private boolean senderDead;
    
    // message/node fields
    private ArrayList<Rule> rRules;
    private LinkedBlockingQueue<TimeStampedMessage> rQueue;
    private LinkedBlockingQueue<TimeStampedMessage> rDelayQueue;
    private TimeStampedMessage message = null;
    
    // input stream field
    private ObjectInputStream incoming;
    
    // verbose flag
    boolean verbose = false;

    /**
     * Receiver verbose constructor
     * 
     * @param socket - socket through which messages will be received
     * @param rQueue - syncronized queue of incoming messages.
     * @param rDelayQueue - syncronized queue of incoming messages to be considered
     *      at a later time.
     * @param verbose - verbose flag
     */
    public Receiver(Socket socket, LinkedBlockingQueue<TimeStampedMessage> rQueue, 
                        LinkedBlockingQueue<TimeStampedMessage> rDelayQueue, boolean verbose) {
        this(socket, rQueue, rDelayQueue);
        this.verbose = verbose;
    }
    
    /** 
     * Receiver constructor
     * 
     * @param socket - socket through which messages will be received
     * @param rQueue - syncronized queue of incoming messages.
     * @param rDelayQueue - syncronized queue of incoming messages to be considered
     *      at a later time.
     */
    public Receiver(Socket socket, LinkedBlockingQueue<TimeStampedMessage> rQueue, 
                        LinkedBlockingQueue<TimeStampedMessage> rDelayQueue) {
                            
        this.socket = socket;
        this.rQueue = rQueue;
        this.rDelayQueue = rDelayQueue;
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
                message = (TimeStampedMessage)incoming.readObject();

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
                // Try to match a rule from the send rule list. If one is found, get the 
                //  action and sequnce number to which it applies
                rRules = ConfigParser.readReceiveRules();
                Action action = Action.NIL;
                int actionSeqNum = -1;
                int messageSeqNum = message.getSeqNum();
                if(rRules.size() > 0) {
                    for (Rule rule : rRules) {
                        if ((rule.ruleApplies(message)) && (action == Action.NIL)) {
                            action = rule.getAction();
                            actionSeqNum = rule.getSeqNum();
                        }
                    }                    
                }
    
                synchronized(rQueue) {
                    // Perform action according to the matched rule's type.
                    switch (action) {
                        // no rule? no issue. Add message to the queue.
                        case NIL:
                            rQueue.add(message);
                            break;
                        // implicit: do nothing. That is, drop this message.
                        case DROP:
                            if(verbose) {
                                System.out.println("Message dropped:" + message.toString());
                            }
                            break;
                        case DROPAFTER:
                            if(messageSeqNum < actionSeqNum) {
                                rQueue.add(message);
                            } else if(verbose) {
                                System.out.println("Message dropped: " + message.toString());
                            }
                            break;
                        // add message to the rDelayQueue to be received after other messages
                        case DELAY:
                            rDelayQueue.add(message);
                            break;
                        // default catch-all. This code should never be executed.
                        default:
                            System.err.println("ERROR: Receiver switch statement fall through.");
                            // add this message to the rQueue
                            rQueue.add(message);
                    }
                    
                    // if something has been added to the rQueue, then the rDelay
                    //  queue can be considered.
                    if(rQueue.size() > 0) {
                        if(verbose) {
                            System.out.println("A message has arrived.");
                        }
                        while(!rDelayQueue.isEmpty()) {
                            rQueue.add(rDelayQueue.poll());
                        }
                    }
                }
            }
        }
    }

    public void terminate() throws IOException {
        socket.close();
    }
}
