/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 0: Communications Infrastructure
 * MessagePasser.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.Rule.Action;

//import utility packages
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;

// import network packages
import java.net.InetAddress;
import java.net.UnknownHostException;

// import exception packages
import java.io.IOException;

/**
 * MessagePasser is a unique class that does not extend anything.
 * 
 * Description:
 *  - MessagePasser contains buffers for sending and receiving messages both 
 *      immediately and in a delayed fashion.
 *  - MessagePasser keeps track of send rules such that all messages 
 *      sent between nodes adhere to the rules oulined in the config file.
 *      (NOTE: Recieve rules will be handled by the receiver).
 *  - MessagePasser keeps track of the nodes layed out in the configuration file and
 *      can only communicate with those nodes per the rules mentioned above.
 */
public class MessagePasser {
    // keep track of all nodes/rules present in the configuration file
    private HashMap<String, Node> nodeMap = new HashMap<String, Node>();

    // create new FIFO send queues
    private LinkedBlockingQueue<Message> sQueue         = new LinkedBlockingQueue<Message>();
    private LinkedBlockingQueue<Message> sDelayQueue    = new LinkedBlockingQueue<Message>();
    
    // create new FIFO recieve queues
    private LinkedBlockingQueue<Message> rQueue         = new LinkedBlockingQueue<Message>();
    private LinkedBlockingQueue<Message> rDelayQueue    = new LinkedBlockingQueue<Message>();

    // variables to keep track of state information
    private String configFilename;
    private String localName;
    private int seqNum;

    // listening and sending threads
    private Listener listener;
    private Sender sender;
    private Thread listenerThread;
    private Thread senderThread;

    /**
     * MessagePasser
     *
     * @param configFilename - filepath to configuration file
     * @param localName - name of the local host
     */
    public MessagePasser(String configFilename, String localName) {
        // set state variables based on inputs
        this.configFilename = configFilename;
        this.localName = localName;
        seqNum = -1;

        // parse configuration file
        nodeMap = Utility.readConfiguration(configFilename);

        /**
         * Ensure that the localName is in the configuration file. 
         *  -if the localName is not in configuration file then there is no way
         *      to enforce the send/receive rules. Therefore, this is an 
         *      error condition.
         */
        if(!nodeMap.containsKey(localName)) {
            System.err.println("MP ERROR: " + localName + " not present in configuration file (" + configFilename + ").");
            System.exit(0);
        } else {
            try {
                // get info about the local host and the expected host IP address
                String localIP = InetAddress.getLocalHost().getHostAddress().toString();
                String nodeIP = nodeMap.get(localName).getIP();
                /**
                 * Ensure that the IP address associated with local name is, in fact, 
                 *  the IP address of the current host. 
                 *  - if the localIP is not in the configuration file then there is no
                 *      way to enforce the send/receive rules. Therefore, this is an 
                 *      error condition
                 */            
                if(!localIP.equals(nodeIP)) {
                    System.err.println("MP ERROR: " + localIP + " does not match " + localName + " IP in configuration file (" + configFilename + ").");
                    System.exit(0);                
                } 
                // No issues? Great. Get started.
                else {
                    // create new listener/sender objects
                    listener = new Listener(nodeMap.get(localName).getPort(), configFilename, rQueue, rDelayQueue);
                    sender = new Sender(nodeMap, sQueue, sDelayQueue);  
                    
                    // create new listend/sender threads
                    listenerThread = new Thread(listener);
                    senderThread = new Thread(sender);
                    
                    // start listening/sending
                    listenerThread.start();
                    senderThread.start();
                }
            }
            catch (UnknownHostException e) {
                System.err.println("MP ERROR: Local host could not be found. Please connected to the internet.");
                System.exit(0);
            }
        }
    }


    /**
     * Send a message on the network.
     *
     * @param message - message to be sent.
     */
    public void send(Message message) {
        // increment seqNum - this starts at -1, so the first message will be '0'
        seqNum++;
        
        // Set source and sequence number of the massage
        message.setSource(localName);
        message.setSeqNum(seqNum);

        // Read rules from configuration file
        ArrayList<Rule> sRules = Utility.readSendRules(configFilename);

        // Try to match a rule from the send rule list. If one is found, get the 
        //  action and sequnce number to which it applies
        Action action = Action.NIL;
        int actionSeqNum = -1;
        for (Rule rule : sRules) {
            if (Utility.ruleApplies(message, rule) && (action == Action.NIL)) {
                action = rule.getAction();
                actionSeqNum = rule.getSeqNum();
            }
        }

        // Perform action according to the matched rule's type.
        switch (action) {
            // no rule? no issue. Add message to the queue.
            case NIL:
                System.out.println("Message added to the queue: " + message.toString());
                sQueue.add(message);
                break;
            // implicit: do nothing. That is, drop this message.
            case DROP:
                System.out.println("Message dropped:" + message.toString());
                break;
            // add message to send queue if appropriate
            case DROPAFTER:
                if(seqNum < actionSeqNum) {
                    sQueue.add(message);
                } else {
                    System.out.println("Message dropped: " + message.toString());
                }
                break;
            // add message to the sDelayQueue to be sent after other messages
            case DELAY:
                sDelayQueue.add(message);
                break;
            // default catch-all. This code should never be executed.
            default:
                System.err.println("ERROR: MessageParser switch statement fall through.");
                // Add this message into sQueue
                sQueue.add(message);
                break;
        }
    }

    // Retrieve a message from the receive queue
    public synchronized Message receive() {
        // NOTE: This method need not worry about the receive rules, that will
        //  be taken care of by the receiving thread.
        Message message = null;
        if(rQueue.size() > 0) {
            message = rQueue.poll();
        }
        return message;
    }

    // stop listening and sending
    public void terminate() {
        System.out.println("Terminating message passer" + toString() + "...");
        System.out.println("Terminating listener " + listener.toString() + "...");
        System.out.println("Terminating sender " + sender.toString() + "...");
    }

    // return the hash map of the nodes in the system
    public HashMap<String, Node> getNodeMap() {
        return nodeMap;
    }
    
    // return the number of messages waiting to be sent
    public synchronized int sendStatus() {
        return sQueue.size() + sDelayQueue.size();
    }
    
    // return the number of messages waiting to be received
    public synchronized int receiveStatus() {
        return rQueue.size() + rDelayQueue.size();
    }

    // return a string version of the message passer
    public String toString() {
        return "MP[localName=" + localName + ", seqNum=" + seqNum +
                ", configFile=" + configFilename +
                ", listenSocket=" + listener.toString() + "]";
    }
}