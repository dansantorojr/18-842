/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 1: Clocks
 * Interface.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */
 
// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.ClockService.ClockType;

// import java utility packages
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// import java io packages
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Interface is a unique class that does not implement/extend anything.
 * 
 * Description:
 *  - Interface is the 'main' program that simulates MessagePasser
 *  - Interface prompts the user, takes user input, does some error checking,
 *      and finally calls MessagePasser methods to exercise MessagePasser functionality.
 */
public class Interface {
    // initialization fields
    private static boolean startup = true;
    private static boolean clockSet = false;
    
    // input information fields
    private static String clockType;
    private static String configFilename;
    private static String localName;
    private static boolean verbose;
    private HashMap<String, Node> nodeMap = new HashMap<String, Node>();

    
    // MessagePass to be invoked
    private static MessagePasser mp;
    
    public static void main(String[] args) {
        verbose = false;
        for(String s : args) {
            if(s.equalsIgnoreCase("-v")) {
                verbose = true;
            }
        }
        
        // Introductory instructions
        System.out.println("WELCOME TO THIS PRODUCT OF 18-842 - DISTRIBUTED SYSTEMS");
        System.out.println("This is an implementation of Lab 1: Clocks (Spring 2016)");
        System.out.println("Brought to you by...");
        System.out.println("...Daniel Santoro // ddsantor.");
        System.out.println("...Akansha Patel // akanshap.");
        System.out.println("");
        System.out.println("To use this program there a few ground rules:");
        System.out.println("1) When we ask you to enter a command, you have five options...");
        System.out.println("     - 'send', 'receive', 'send status', 'receive status', and 'withdraw'.");
        System.out.println("2) Should you chose send, you will be asked for the destination."); 
        System.out.println("    The information we require is the name of the server you'd");
        System.out.println("    like to send a message to.");
        System.out.println("3) When we ask you for the 'type of message' can be anything you'd");
        System.out.println("    like. If you don't mind, keep it short. And, no. We won't");
        System.out.println("    respond to 'Pizza Orders' with a delivery.");
        System.out.println("4) When sending a message, don't make any assumptions about the");
        System.out.println("    privacy of its contents. No security was implemented.");
        System.out.println("");
        System.out.println("Enjoy!!");
        System.out.println("");
        
        // Run the simulator
        while(true) {
            try {
                // initialize userInput
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                
                // The first time this loop is run go through the startup sequence.
                if(startup) {
                    // obtain the local host name
                    System.out.println("First things first, who are you?");
                    localName = userInput.readLine();
                    
                    // obtain the location/name of .yaml configuration file
                    System.out.println("Welcome, " + localName + ". Can you tell me the name of the configuration file?");
                    configFilename = userInput.readLine();
                    if(configFilename.equalsIgnoreCase("")) {
                        configFilename = "config.yaml";
                    }

                    // create the logger
                    if(localName.equalsIgnoreCase("Logger")) {
                        Logger logger = new Logger(configFilename, localName, verbose);
                        
                        // This is the logger interface
                        while(true) {
                            System.out.println("\nWhat Would you like to do with the log? (view.status.withdraw).");
                            String ans = userInput.readLine();
                            if(ans.equalsIgnoreCase("view")) {
                                logger.updateLog();
                                System.out.println("Current log size: " + logger.logSize() + ".");
                                if(logger.logSize() > 0) {
                                    logger.printLog();
                                }
                            } else if(ans.equalsIgnoreCase("status")) {
                                System.out.println("Current log size: " + logger.logSize() + ".");
                            } else if (ans.equalsIgnoreCase("withdraw")) {
                                System.out.println("Thanks for stopping by!");
                                System.out.println("");
                                System.exit(0);                                
                            } else {
                                System.out.println("Unfortunately, we're not sure what that means.");
                            }
                        }
                    }

                    // create new MessagePasser and set startup flag
                    mp = new MessagePasser(configFilename, localName, verbose);
                    startup = false;
                }
                
                // prompt for new request
                System.out.println("");
                System.out.println("What would you like to do? (send.receive.multicast.status.withdraw)");
                String request = userInput.readLine();
                
                // 'withdraw' - terminate
                if(request.equalsIgnoreCase("withdraw") || request.equalsIgnoreCase("x")) {
                    System.out.println("Thanks for stopping by!");
                    System.out.println("");
                    mp.terminate();
                    System.exit(0);
                } 
                // 'send' - send a message to someone
                else if (request.equalsIgnoreCase("send") || request.equalsIgnoreCase("s")) {
                    // prompt for destination
                    System.out.println("To whom would you like to speak?");
                    HashMap<String, Node> nodeMap = mp.getNodeMap();
                    for(Map.Entry<String, Node> entry : nodeMap.entrySet()){
                        System.out.println("   - " + entry.getKey());
                    }
                    
                    // ensure destination is in the configuration file
                    String destination = userInput.readLine();
                    if(!nodeMap.containsKey(destination)) {
                        System.out.println("Unfortunately, " + destination + " is not an available node to speak with.");
                    } 
                    // get the rest of the message information
                    else {
                        // type of message
                        boolean validKind = false;
                        String kind = "";
                        while(!validKind) {
                            System.out.println("Great! What type of message would you like to send to " + destination + "?");
                            kind = userInput.readLine();   
                            if(kind.equalsIgnoreCase("multicast")) {
                                System.out.println("We're sorry. That message type is reserved and cannot be used in this context.");
                                validKind = false;
                            } else {
                                validKind = true;
                            }
                        }

                        // message payload
                        System.out.println("What would you like to say to " + destination + "?");
                        String payload = userInput.readLine();

                        // invoke MessagePasser's send method
                        Message toSend = new Message(destination, kind, payload);
                        mp.sendTimeStampedMessage(toSend);
                        //mp.send(toSend);
                    }
                } 
                else if(request.equalsIgnoreCase("multicast") || request.equalsIgnoreCase("m")) {
                    // prompt for destination
                    System.out.println("Which group would you like to speak to?");
                    HashMap<String, Group> groupMap = ConfigParser.readGroups();
                    //Iterator it = groupMap.entrySet().iterator();
                    for(Map.Entry<String, Group> entry : groupMap.entrySet()){
                        System.out.println("   - " + entry.getKey());
                    }

                    // ensure destination is in the configuration file
                    String groupDestination = userInput.readLine();
                    if(!groupMap.containsKey(groupDestination)) {
                        System.out.println("Unfortunately, " + groupDestination + " is not an available group to multicast a message to.");
                    } 
                    // get the rest of the message information
                    else {
                        // type of message
                        String kind = "multicast";
                        
                        // message payload
                        System.out.println("What would you like to say to " + groupDestination + "?");
                        String payload = userInput.readLine();
                        MulticastPayload multicastPayload = new MulticastPayload(groupDestination, payload);

                        
                        // invoke MessagePasser's send method
                        Message toSend = new Message(kind, multicastPayload);
                        mp.multicast(toSend);
                    }
                }
                // 'receive' - receive a message
                else if (request.equalsIgnoreCase("receive") || request.equalsIgnoreCase("r")) {
                    // call MessagePasser's receive method
                    System.out.println("Let's see what's in the queue...");
                    TimeStampedMessage received = mp.receive();
                    if(received == null) {
                        System.out.println("Unfortunately, there were no messages waiting for you.");
                    } else {
                        System.out.println(received.toString());
                    }
                } 
                // 'send status' - get status of sent messages 
                else if (request.equalsIgnoreCase("status")) {
                    System.out.println("There are " + mp.sendStatus() + " messages waiting to be sent.");
                    System.out.println("There are " + mp.receiveStatus() + " messages waiting to be received.");
                    System.out.println("Current time: " + mp.currentTime());
                }
                // unknown input
                else {
                    System.out.println("Unfortunately, we're not sure what that means.");
                }
            }
            catch (IOException e) {
                System.err.println("INTERFACE ERROR: Could not read user input.");
            }
        }
    }
}
