/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * ClockService.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */
 
// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;

/** 
 * ClockService - is a unique class that does not extend anything.
 * 
 * Description: 
 *  - ClockService implements an interface to time stamps for a given node.
 *  - This class is abstract because it needs to be extended by LogicalClockService
 *      and VectorClockService
 */
public abstract class ClockService {
    // step size field
    public final int step = 1;
    
    // clock type enumeration
    public enum ClockType {
        LOGICAL, VECTOR
    } 
    
    // time fields
    protected TimeStamp currentTime;
    private static ClockService instance;
    protected String localName;

    /**
     * ClockService constructor
     * 
     * @param type - type of clock (VECTOR or LOGICAL)
     * @param localName - name of the node who owns this clock
     * @param nodeMap - HashMap of the nodes in the system
     */
    public static ClockService getClockService(ClockType type, String localName, 
                                                    HashMap<String, Node> nodeMap) {
        
        // based on the type, get an appropriate clock service
        switch(type) {
            case VECTOR:
                instance = new VectorClockService(localName, nodeMap);
                return instance;
            case LOGICAL:
            default:
                instance = new LogicalClockService(localName);
                return instance;
        }
    }
    
    // return the current time
    public TimeStamp getCurrentTime() {
        return this.currentTime;
    }
    
    /**
     * incrementTime - increment the time stamp for the current type and node.
     * 
     * NOTE: This is abstract because it will be implemented by the ClockService
     *  extensions (LogicalClockService/VectorClodkService)
     */
    public abstract void incrementTime();
    
    /**
     * incrementTimeOnReceive- increment the time stamp for the current type and
     *  node based on the received time stamp.
     * 
     * NOTE: This is abstract because it will be implemented by the ClockService
     *  extensions (LogicalClockService/VectorClodkService)
     * 
     * @param ts - received time stamp
     */
    public abstract void incrementTimeOnReceive(TimeStamp ts);
    
    
    public abstract void incrementTimeOnMulticastReceive(TimeStamp ts);

 }