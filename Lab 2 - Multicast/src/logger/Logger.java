/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * Logger.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.ClockService.ClockType;

//import utility packages
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

// import network packages
import java.net.InetAddress;
import java.net.UnknownHostException;

// import exception packages
import java.io.IOException;

/**
 * Logger is a unique class that does not extend anything.
 * 
 * Description:
 *  - Logger contains buffers for receiving log messages
 *  - Logger keeps track of the nodes layed out in the configuration file and
 *      can only communicate with those nodes.
 */
public class Logger {
    // keep track of all nodes/received messages
    private HashMap<String, Node> nodeMap = new HashMap<String, Node>();
    private LinkedList<TimeStampedMessage> log;
    
    // create new FIFO recieve queue
    private LinkedBlockingQueue<TimeStampedMessage> rQueue = new LinkedBlockingQueue<TimeStampedMessage>();

    // variables to keep track of state information
    private String configFilename;
    private String localName;
    private ClockType clockType;

    // listening threads
    private LoggerListener listener;
    private Thread listenerThread;

    // boolean flags
    private boolean verbose = false;
    
    /**
     * Logger - verbose constructor
     * 
     * @param configFilename - filepath to configuration file
     * @param localName - name of local host
     * @param verbose - verbose output setting
     */
    public Logger(String configFilename, String localName, boolean verbose) {
        this(configFilename, localName);
        this.verbose = verbose;
    }

    /**
     * Logger
     *
     * @param configFilename - filepath to configuration file
     * @param localName - name of the local host
     */
    public Logger(String configFilename, String localName) {
        // set state variables based on inputs
        this.configFilename = configFilename;
        this.localName = localName;
        this.log = new LinkedList<TimeStampedMessage>();

        // parse configuration file
        ConfigParser.configFilename = configFilename;
        nodeMap = ConfigParser.readConfiguration();
        this.clockType = ConfigParser.systemClockType();

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
                 *      way to communicate with this node.
                 */            
                if(!localIP.equals(nodeIP)) {
                    System.err.println("MP ERROR: " + localIP + " does not match " + localName + " IP in configuration file (" + configFilename + ").");
                    System.exit(0);                
                } 
                // No issues? Great. Get started.
                else {
                    // create new listener/sender objects
                    listener = new LoggerListener(nodeMap.get(localName).getPort(), rQueue);

                    // create new listend/sender threads
                    listenerThread = new Thread(listener);

                    // start listening/sending
                    listenerThread.start();
                }
            }
            catch (UnknownHostException e) {
                System.err.println("MP ERROR: Local host could not be found. Please connected to the internet.");
                System.exit(0);
            }
        }
    }
    
    // Retrieve a message from the receive queue
    public synchronized TimeStampedMessage receive() {
        System.out.println("Loggger receive method.");
        // NOTE: This method need not worry about the receive rules, that will
        //  be taken care of by the receiving thread.
        TimeStampedMessage message = null;
        if(rQueue.size() > 0) {
            message = rQueue.poll();
        }
        return message;
    }

    // update and sort the log
    public void updateLog() {
        int numNewMessages = rQueue.size();

        // add all newly received messages to the log
        for(int i = 0; i < numNewMessages; i++) {
            TimeStampedMessage received = receive();
            log.add(received);
        }            
        
        // sort the log
        if(clockType == ClockType.VECTOR) {
            Collections.sort(log);
        }
    }

    // format a log entry suitable for terminal output
    private String formatData(TimeStampedMessage msg) {
        String out = "";
        out += msg.getKind().toUpperCase() + " @time" + msg.getTimeStamp() + "; \n";
        out += "\tlog src : " + msg.getSource() + "\n";
        out += "\tlog data: " + msg.getData().toString();  
        return out;
    }
    
    // print the log out in the terminal
    public void printLog() {
        // local variable instantiation
        TimeStampedMessage current, next;
        int compareVal;
    
        // update the log
        updateLog();
        
        // print out the log based on the type of system clock that is implemented
        if(clockType == ClockType.VECTOR) {
            // vector clock header
            System.out.println("VECTOR CLOCK IMPLEMENTED.");
            System.out.println("Log ordered in total system order.");
            
            // loop through all entries in the log and print.
            for(int i = 0; i < this.log.size() - 1; i++) {
                
                // print out the current log entry
                current = log.get(i);
                System.out.println(formatData(current));
                
                // investigate the next log entry to determine the relationship
                //  between the current message and then next message
                next = log.get(i+1);
                compareVal = current.compareTo(next);
                
                if(compareVal == 0) {
                    System.out.println("\nCONCURRENT WITH [||]\n");
                } else if(compareVal == -1) {
                    System.out.println("\nHAPPEND BEFORE [->]\n");
                } else {
                    System.out.println("\nLOG NOT SORTED\n");
                }
            }
            
            // print out the last log entry
            System.out.println(formatData(log.get(log.size() - 1)));            
        } else {
            // logical clock header
            System.out.println("LOGICAL CLOCK IMPLEMENTED.");
            System.out.println("Log ordered by the time received by the log.");
            
            // iterate through all entries and print out in the order that they
            //  they were received.
            ListIterator<TimeStampedMessage> logIterator = log.listIterator();
	        while (logIterator.hasNext()) {
	            System.out.println(formatData(logIterator.next()));
	        }
        }
    }
    
    // get the log size
    public int logSize() {
        return log.size();
    }

    // return the hash map of the nodes in the system
    public HashMap<String, Node> getNodeMap() {
        return nodeMap;
    }
    
    // stop listening and sending
    public void terminate() {
        System.out.println("Terminating message passer" + toString() + "...");
        System.out.println("Terminating listener " + listener.toString() + "...");
    }

    // return a string version of the message passer
    public String toString() {
        return "Logger";
    }
}