/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * MessagePasser.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.Rule.Action;
import DistSystComm.ClockService.ClockType;

// import utility packages
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Collections;
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
    public enum RequestState {
        RELEASED, WANTED, HELD;
    }
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
    
    // critical section fields
    private ArrayList<TimeStampedMessage> CSrequestQueue = new ArrayList<TimeStampedMessage>();
    private RequestState CSstate = RequestState.RELEASED;
    private Boolean voted = false;
    private Integer CSreplies = 0;
    private String CSgroup;
    private Integer CSrequired;
    
    // clock fields
    private GroupClockService groupClockService;
    private ClockService clockService;
    private ClockType clockType;

    // variables to keep track of state information
    private String configFilename;
    private String localName;
    private int seqNum;
    private Integer msgSent;
    private Integer msgReceived;

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
                
                // set critical section fields
                ArrayList<String> groupList = nodeMap.get(localName).getGroupList();
                CSgroup = nodeMap.get(localName).getGroupList().get(0);
                CSrequired = groupMap.get(CSgroup).getGroup().size();
                
                // set data fields
                msgSent = 0;
                msgReceived = 0;
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
        msgSent++;
        
        // Set sequence number of the massage
        message.setSeqNum(seqNum);

        // send message
        ArrayList<Rule> sRules = ConfigParser.readSendRules();
        // Try to match a rule from the send rule list. If one is found, get the 
        //  action and sequnce number to which it applies
        Action action = Action.NIL;
        int actionSeqNum = -1;
        if(!sRules.isEmpty()) {
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
        
        // First - check the hold back queue to see if there is a message 
        //  that is ready to br received
        if(!holdBackQueue.isEmpty()) {
            // loop through all messages in the queue
            for(int i = 0; i < holdBackQueue.size(); i++) {
                // if the message is ready to be received, then pull it out and 
                //  and set the flag.
                if(removeFromHoldback(holdBackQueue.get(i)) && !receivedMulti) {
                    message = holdBackQueue.get(i);
                    holdBackQueue.remove(i);
                    receivedMulti = true;
                }
            }
        }
        
        // loop through through until a message has bene received
        while((!rQueue.isEmpty()) && !receivedDirect && !receivedMulti) {
            message = rQueue.poll();
            msgReceived++;
            
            // special procedure for a multicast message
            if(message.getData() instanceof MulticastPayload) {
                // execute this code block if this message has never been received before
                if(!inReceived(message)) {
                    // add it to the received queue
                    multicastReceived.add(message);
                    // recast if necessary
                    if(!localName.equalsIgnoreCase(message.getSource())) {
                        this.recast(message);
                    }
                    // if this message is ready to be received, then receive it. 
                    //  otherwise, place in the holdback queue
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
            } 
            // receive a direct message
            else {
                // update time stamp and log it
                receivedDirect = true;
                clockService.incrementTimeOnReceive(message.getTimeStamp());                
            }            
        }
        
        // if a critical section request has been received --
        if((message != null) && (message.getKind().equalsIgnoreCase("critical section request"))) {
            // add the message the request queue if...
            //  (1) the critical section is currently held by me, OR
            //  (2) I have voted already
            if((CSstate == RequestState.HELD) || (voted == true)) {
                CSrequestQueue.add(message);
            } else {
                // set flag
                voted = true;
                
                // initialize message
                String kind = "critical section reply";
                String payload = message.getSource();
                String destination = message.getSource();
                String groupName = ((MulticastPayload)message.getData()).getGroupName();
                
                // invoke multicast
                MulticastPayload multicastPayload = new MulticastPayload(groupName, payload);
                Message toSend = new Message(destination, kind, multicastPayload);
                multicast(toSend);
            }
        }
        
        // if a critical section release has been received --
        if((message != null) && (message.getKind().equalsIgnoreCase("critical section release"))) {
            // if the request queue isn't empty, then reply to next message
            if(!CSrequestQueue.isEmpty()) {
                // sort the queue/set the voted flag
                Collections.sort(CSrequestQueue);
                voted = true;
                
                // get the the next request out of the queue
                Message request = CSrequestQueue.get(0);
                CSrequestQueue.remove(0);
                
                // initialize message
                String kind = "critical section reply";
                String payload = request.getSource();
                String destination = request.getSource();
                String groupName = ((MulticastPayload)request.getData()).getGroupName();
                
                // invoke multicast
                MulticastPayload multicastPayload = new MulticastPayload(groupName, payload);
                Message toSend = new Message(destination, kind, multicastPayload);
                multicast(toSend);
            } else {
                voted = false;
            }
        }
        
        // if a critical section reply has been received --
        if((message != null) && (message.getKind().equalsIgnoreCase("critical section reply"))) {
            // determine who the reply was intended for (stored in the message payload)
            String replyTo = (String)((MulticastPayload)message.getData()).getPayload();
            
            // if the reply was for me, then increment the CSreplies 
            //  (Critical Section replies) field and compare to the number of 
            //  required replies to obtain the critical section.
            if(replyTo.equalsIgnoreCase(localName)) {
                CSreplies += 1;
                if(CSreplies == CSrequired) {
                    CSstate = RequestState.HELD;
                    System.out.println("Critical section obtained");
                }                
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
     * requestCriticalSection - multicast a message to the predetermined 
     *  'CSgroup' (Critical Section group) to request the critical section
     */
    public synchronized void requestCriticalSection() {
        // only send the request if the critical section...
        //  (1) is not currently held
        //  (2) has not already been requested
        if(CSstate == RequestState.RELEASED) {
            // set state
            CSstate = RequestState.WANTED;
            
            // initialize message
            String kind = "critical section request";
            String payload = "future expansion"; 
            String groupDestination = CSgroup;
            
            // invoke multicast
            MulticastPayload multicastPayload = new MulticastPayload(groupDestination, payload);
            Message toSend = new Message(kind, multicastPayload);
            multicast(toSend);            
        }
    }

    /**
     * releaseCriticalSection - multicast a message to the predetermined 
     *  'CSgroup' (Critical Section group) to release the critical section
     */
    public synchronized void releaseCriticalSection() {
        // only send release if critical section is currently held
        if(CSstate == RequestState.HELD) {
            // reset variables
            CSstate = RequestState.RELEASED;
            CSreplies = 0;
            
            // initialize message
            String kind = "critical section release";
            String payload = "future expansion";
            String groupDestination = CSgroup;
    
            // invoke multicast
            MulticastPayload multicastPayload = new MulticastPayload(groupDestination, payload);
            Message toSend = new Message(kind, multicastPayload);
            multicast(toSend);
        }
    }

    /** 
     * removeFromHoldback - determine if a message can be removed from the holdback queue
     * 
     * @param message - determine if this message can be removed
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
    private synchronized int sendStatus() {
        int waiting = sQueue.size() + sDelayQueue.size();
        return waiting;
    }
    
    // return the number of messages waiting to be received
    private synchronized int receiveStatus() {
        int waiting = rQueue.size() + rDelayQueue.size() + holdBackQueue.size();
        return waiting;
    }
    
    // return the current time stamp of this node
    private synchronized TimeStamp currentTime() {
        return clockService.getCurrentTime();
    }
    
    // return the current critical section state
    private RequestState criticalSectionState() {
        return CSstate;
    }
    
    // return the current critical section voting status
    private boolean getVoted() {
        return voted;
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
    
    // print the current status
    public synchronized void PrintStatus() {
        System.out.println(localName + " Status:");
        System.out.println("  Total messages sent: " + msgSent.toString());
        System.out.println("  Total messages received: " + msgReceived.toString());
        System.out.println("  There are " + sendStatus() + " messages waiting to be sent.");
        System.out.println("  There are " + receiveStatus() + " messages waiting to be received.");
        if(CSstate == RequestState.HELD) {
            System.out.println("  Critical section is currently held by me!");
        } else if (CSstate == RequestState.WANTED) {
            System.out.println("  Critical section has been requested.");
        } else if (CSstate == RequestState.RELEASED) {
            System.out.println("  Critical section has been released.");
        }
        System.out.println("  Voted: " + voted);
        System.out.println("  Affirmative replies: " + CSreplies.toString() + " of " + CSrequired.toString());
        System.out.println("  Current time: " + currentTime());        
    }

    // return a string version of the message passer
    public String toString() {
        return "MP[localName=" + localName + ", seqNum=" + seqNum +
                ", configFile=" + configFilename +
                ", listenSocket=" + listener.toString() + "]";
    }
}