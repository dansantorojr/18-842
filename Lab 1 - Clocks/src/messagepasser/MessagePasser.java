/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 1: Clocks
 * MessagePasser.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.Rule.Action;
import DistSystComm.ClockService.ClockType;

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
    private LinkedBlockingQueue<TimeStampedMessage> sQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    private LinkedBlockingQueue<TimeStampedMessage> sDelayQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    
    // create new FIFO recieve queues
    private LinkedBlockingQueue<TimeStampedMessage> rQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    private LinkedBlockingQueue<TimeStampedMessage> rDelayQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    
    // clock fields
    private ClockService clockService;
    private ClockType clockType;

    // variables to keep track of state information
    private String configFilename;
    private String localName;
    private int seqNum;

    // listening and sending threads
    private Listener listener;
    private Sender sender;
    private Thread listenerThread;
    private Thread senderThread;

    // verbose flag
    private boolean verbose = false;
    
    /** 
     * MessagePasser verbose constructor
     * 
     * @param configFilename - filepath to configuration file
     * @param localName - name of local host
     * @param verbose - verbose flag
     */
    public MessagePasser(String configFilename, String localName, boolean verbose) {
        this(configFilename, localName);
        this.verbose = verbose;
    }

    /**
     * MessagePasser constructor
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
        ConfigParser.configFilename = configFilename;
        nodeMap = ConfigParser.readConfiguration();

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
                    listener = new Listener(nodeMap.get(localName).getPort(), rQueue, rDelayQueue);
                    sender = new Sender(nodeMap, sQueue, sDelayQueue);  
                    
                    // create new listend/sender threads
                    listenerThread = new Thread(listener);
                    senderThread = new Thread(sender);
                    
                    // start listening/sending
                    listenerThread.start();
                    senderThread.start();
                }
                
                // create the clock
                clockType = ConfigParser.systemClockType();
                clockService = ClockService.getClockService(clockType, localName, nodeMap);
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
        ArrayList<Rule> sRules = ConfigParser.readSendRules();
        
        // log all sent messages
        clockService.incrementTime();
        TimeStampedMessage tsm = new TimeStampedMessage(message, clockService.getCurrentTime());
        sendToLogger(tsm, "sent message");
        
        // Try to match a rule from the send rule list. If one is found, get the 
        //  action and sequnce number to which it applies
        Action action = Action.NIL;
        int actionSeqNum = -1;
        if(sRules.size() > 0) {
            for (Rule rule : sRules) {
                if (rule.ruleApplies(tsm) && (action == Action.NIL)) {
                    action = rule.getAction();
                    actionSeqNum = rule.getSeqNum();
                }
            }            
        }
        
        // Perform action according to the matched rule's type.
        switch (action) {
            // no rule? no issue. Add message to the queue.
            case NIL:
                if(verbose) {
                    System.out.println("Message added to the queue: " + tsm.toString());
                }
                sQueue.add(tsm);
                break;
            // implicit: do nothing. That is, drop this message.
            case DROP:
                if(verbose) {
                    System.out.println("Message dropped:" + tsm.toString());
                }
                break;
            // add message to send queue if appropriate.
            case DROPAFTER:
                if(seqNum < actionSeqNum) {
                    sQueue.add(tsm);
                } else if(verbose) {
                    System.out.println("Message dropped: " + tsm.toString());
                }
                break;
            // add message to the sDelayQueue to be sent after other messages.
            case DELAY:
                sDelayQueue.add(tsm);
                break;
            // default catch-all. This code should never be executed.
            default:
                System.err.println("ERROR: MessageParser switch statement fall through.");
                // Add this message into sQueue
                sQueue.add(tsm);
                break;
        }
    }
    
    /**
     * sendToLogger - send a message to logger
     * 
     * @param message - message to be logged
     * @param info - indication of either 'message sent' or 'message received'
     */
    public void sendToLogger(TimeStampedMessage message, String info) {
        TimeStampedMessage toLog = new TimeStampedMessage(info, localName, "Logger", seqNum++, message, clockService.getCurrentTime()); 
        sQueue.add(toLog);
    }
    
    /** 
     * sendToLogger - send a status update to logger
     * 
     * @param status - status to be logged
     * @param info - indication of the type of status being sent
     */
    public void sendToLogger(String status, String info) {
        TimeStampedMessage toLog = new TimeStampedMessage(info, localName, "Logger", seqNum++, status, clockService.getCurrentTime());
        sQueue.add(toLog);
    }
    
    
    // Retrieve a message from the receive queue
    public synchronized TimeStampedMessage receive() {
        // NOTE: This method need not worry about the receive rules, that will
        //  be taken care of by the receiving thread.
        TimeStampedMessage message = null;
        if(rQueue.size() > 0) {
            message = rQueue.poll();
            // update time stamp and log it
            clockService.incrementTimeOnReceive(message.getTimeStamp());
            sendToLogger(message, "recieved message");                
        }
        return message;
    }

    // stop listening and sending
    public void terminate() {
        // log the termination
        String status = "!" + localName + " LEAVING THE SYSTEM!";
        clockService.incrementTime();
        sendToLogger(status, "status update");
        
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
        int waiting = sQueue.size() + sDelayQueue.size();
        
        // log this status update
        String status = localName + " waiting to be sent: " + waiting;
        clockService.incrementTime();
        sendToLogger(status, "status update");
        
        return waiting;
    }
    
    // return the number of messages waiting to be received
    public synchronized int receiveStatus() {
        int waiting = rQueue.size() + rDelayQueue.size();
        
        // log this status update
        String status = localName + " waiting to be received: " + waiting;
        clockService.incrementTime();
        sendToLogger(status, "status update");
        
        return waiting;
    }
    
    // return the current time stamp of this node
    public synchronized TimeStamp currentTime() {
        return clockService.getCurrentTime();
    }

    // return a string version of the message passer
    public String toString() {
        return "MP[localName=" + localName + ", seqNum=" + seqNum +
                ", configFile=" + configFilename +
                ", listenSocket=" + listener.toString() + "]";
    }
}