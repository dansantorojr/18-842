/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * MessagePasser.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.Rule.Action;
import DistSystComm.ClockService.ClockType;

//import utility packages
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.ListIterator;

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
 *      (NOTE: Receive rules will be handled by the receiver).
 *  - MessagePasser keeps track of the nodes layed out in the configuration file and
 *      can only communicate with those nodes per the rules mentioned above.
 */
public class MessagePasser {
    // keep track of all nodes/rules present in the configuration file
    private HashMap<String, Node> nodeMap = new HashMap<String, Node>();
    private HashMap<String, Group> groupMap = new HashMap<String, Group>();

    // create new FIFO send queues
    private LinkedBlockingQueue<TimeStampedMessage> sQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    private LinkedBlockingQueue<TimeStampedMessage> sDelayQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    
    // create new FIFO receive queues
    private LinkedBlockingQueue<TimeStampedMessage> rQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    private LinkedBlockingQueue<TimeStampedMessage> rDelayQueue = new LinkedBlockingQueue<TimeStampedMessage>();
    private ArrayList<TimeStampedMessage> holdBackQueue = new ArrayList<TimeStampedMessage>();
    private ArrayList<TimeStampedMessage> multicastReceived = new ArrayList<TimeStampedMessage>();
    
    // clock fields
    private GroupClockService groupClockService;
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
        groupMap = ConfigParser.readGroups();

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
                groupClockService = new GroupClockService(clockType, localName, nodeMap, groupMap);
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
    public synchronized void send(TimeStampedMessage message) {
        // increment seqNum - this starts at -1, so the first message will be '0'
        seqNum++;
        
        // Set sequence number of the massage
        message.setSeqNum(seqNum);

        // send message
        ArrayList<Rule> sRules = ConfigParser.readSendRules();
        // Try to match a rule from the send rule list. If one is found, get the 
        //  action and sequnce number to which it applies
        Action action = Action.NIL;
        int actionSeqNum = -1;
        if(sRules.size() > 0) {
            for (Rule rule : sRules) {
                if (rule.ruleApplies(message) && (action == Action.NIL)) {
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
                    System.out.println("Message added to the queue: " + message.toString());
                }
                sQueue.add(message);
                break;
            // implicit: do nothing. That is, drop this message.
            case DROP:
                if(verbose) {
                    System.out.println("Message dropped:" + message.toString());
                }
                break;
            // add message to send queue if appropriate.
            case DROPAFTER:
                if(seqNum < actionSeqNum) {
                    sQueue.add(message);
                } else if(verbose) {
                    System.out.println("Message dropped: " + message.toString());
                }
                break;
            // add message to the sDelayQueue to be sent after other messages.
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

    /**
     * sendTimeStampedMessage - convert a message to a time stamped message and
     *  send using the send() method
     * 
     * @param message - message to send
     */
    public void sendTimeStampedMessage(Message message) {
        message.setSource(localName);
        clockService.incrementTime();
        TimeStampedMessage tsm = new TimeStampedMessage(message, clockService.getCurrentTime());
        this.send(tsm);
    }

    // Retrieve a message from the receive queue
    public synchronized TimeStampedMessage receive() {
        // NOTE: This method need not worry about the receive rules, that will
        //  be taken care of by the receiving thread.
        TimeStampedMessage message = null;
        boolean receivedMulti = false;
        boolean receivedDirect = false;
        
        if(holdBackQueue.size() > 0) {
            for(int i = 0; i < holdBackQueue.size(); i++) {
                if(removeFromHoldback(holdBackQueue.get(i)) && !receivedMulti) {
                    message = holdBackQueue.get(i);
                    holdBackQueue.remove(i);
                    receivedMulti = true;
                }
            }
        }
        
        while((rQueue.size() > 0) && !receivedDirect && !receivedMulti) {
            message = rQueue.poll();
            
            if(message.getData() instanceof MulticastPayload) {
                if(!inReceived(message)) {
                    multicastReceived.add(message);
                    if(!localName.equalsIgnoreCase(message.getSource())) {
                        this.recast(message);
                    }
                    if(removeFromHoldback(message)) {
                        String groupName = ((MulticastPayload)message.getData()).getGroupName();
                        clockService.incrementTimeOnReceive(message.getTimeStamp());
                        groupClockService.incrementTimeOnMulticastReceive(groupName, message.getTimeStamp());
                        receivedMulti = true;
                    } else {
                        holdBackQueue.add(message);
                        message = null;
                    }
                } else {
                    message = null;
                }
            } else {
                // update time stamp and log it
                receivedDirect = true;
                clockService.incrementTimeOnReceive(message.getTimeStamp());                
            }            
        }
        return message;
    }
    
    /** 
     * multicast - multicast a message
     * 
     * @paraam multicastMessage - message to mluticast
     */
    public void multicast(Message multicastMessage) {
        // get group to which this message is being sent
        MulticastPayload multicastPayload = (MulticastPayload)multicastMessage.getData();
        String groupName = multicastPayload.getGroupName();
        
        // set source and increment appropriate clocks
        multicastMessage.setSource(localName);
        clockService.incrementTime();
        groupClockService.incrementTime(groupName);

        // call send serially
        for(String destination : groupMap.get(groupName).getGroup()) {
            TimeStampedMessage tsm = new TimeStampedMessage(multicastMessage, groupClockService.getCurrentTime(groupName));
            tsm.setDestination(destination);
            this.send(tsm);   
        }
    }
    
    /**
     * recast - recast a message when it is received
     * 
     * @param tsm - time stamped message to re-cast
     */
    public void recast(TimeStampedMessage tsm) {
        // get group name
        MulticastPayload multicastPayload = (MulticastPayload)tsm.getData();
        String groupName = multicastPayload.getGroupName();
        
        // loop through all nodes and send 
        //  NOTE: add directly to the send queue to avoid evaluating rules
        for(String destination : groupMap.get(groupName).getGroup()) {
            if(!destination.equalsIgnoreCase(localName)) {
                TimeStampedMessage recastMessage = tsm.copyOf();
                recastMessage.setDestination(destination);
                seqNum++;
                recastMessage.setSeqNum(seqNum);
                sQueue.add(recastMessage);
            }
        }
    }

    /** 
     * removeFromHoldback - determine if a message can be removed from the holdback queue
     */
    private synchronized boolean removeFromHoldback(TimeStampedMessage message) {
        // local variables
        String source = message.getSource();
        HashMap<String, AtomicInteger> msgTime;
        HashMap<String, AtomicInteger> thisTime;
        String groupName;
        
        // boolean flag
        boolean remove = true;
        
        // determine if the message can be removed
        if(message.getTimeStamp() instanceof LogicalTimeStamp) {
            remove = true;
        } else if(message.getSource().equalsIgnoreCase(localName)) {
            remove = true;
        } else if(message.getData() instanceof MulticastPayload) {
            groupName = ((MulticastPayload)message.getData()).getGroupName();
            msgTime = ((VectorTimeStamp)message.getTimeStamp()).getTimeStamp();
            thisTime = ((VectorTimeStamp)groupClockService.getCurrentTime(groupName)).getTimeStamp();
            for(String groupMember : groupMap.get(groupName).getGroup()) {
                // only allow "true" to be returned if the current time value 
                //  of the sender in the local vector clock is one less than the 
                //  senders entry in the timestamp in the message
                if(groupMember.equalsIgnoreCase(source)) {
                    if(msgTime.get(groupMember).get() != (thisTime.get(groupMember).get() + 1)) {
                        remove = false;
                    }
                } 
                // only allow "true" to be returned if the current node's entry
                //  in the message timestamp is <= the current node's entry in the 
                //  local time stamp.
                else {
                    if(msgTime.get(groupMember).get() > thisTime.get(groupMember).get()) {
                        remove = false;
                    }
                }
            }
        }
        return remove;
    }
    
    // inReceived - determine if a message is in the received ArrayList 
    private boolean inReceived(TimeStampedMessage message) {
        boolean wasReceived = false;
        ListIterator<TimeStampedMessage> it = multicastReceived.listIterator();
        for(TimeStampedMessage received : multicastReceived) {
            if(received.isEquivalent(message)) {
                wasReceived = true;
            }
        }
        return wasReceived;
    }

    // return the hash map of the nodes in the system
    public HashMap<String, Node> getNodeMap() {
        return nodeMap;
    }
    
    // return the number of messages waiting to be sent
    public synchronized int sendStatus() {
        int waiting = sQueue.size() + sDelayQueue.size();
        clockService.incrementTime();
        return waiting;
    }
    
    // return the number of messages waiting to be received
    public synchronized int receiveStatus() {
        int waiting = rQueue.size() + rDelayQueue.size() + holdBackQueue.size();
        clockService.incrementTime();
        return waiting;
    }
    
    // return the current time stamp of this node
    public synchronized TimeStamp currentTime() {
        return clockService.getCurrentTime();
    }

    // stop listening and sending
    public void terminate() {
        // log the termination
        String status = "!" + localName + " LEAVING THE SYSTEM!";
        clockService.incrementTime();

        System.out.println("Terminating message passer" + toString() + "...");
        System.out.println("Terminating listener " + listener.toString() + "...");
        System.out.println("Terminating sender " + sender.toString() + "...");
    }

    // return a string version of the message passer
    public String toString() {
        return "MP[localName=" + localName + ", seqNum=" + seqNum +
                ", configFile=" + configFilename +
                ", listenSocket=" + listener.toString() + "]";
    }
}