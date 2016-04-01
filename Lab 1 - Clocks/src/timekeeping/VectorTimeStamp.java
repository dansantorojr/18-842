/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 1: Clocks
 * VectorTimeStamp.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * VectorTimeStamp extends TimeStamp implements Compareable
 * 
 * Description:
 *  - VectorTimeStamps extends the TimeStamp class to be used by the VectorClockService
 *      class.
 *  - VectorTimeStamp uses a hash map of AtomicIntegers as a time stamp. The key
 *      for this hash map is the name of the node
 *  - VectorTimeStamp implements Comparable in order to easily order the system events.
 */
public class VectorTimeStamp extends TimeStamp implements Comparable<VectorTimeStamp> {
    // set serial version - Why? Consistency?
    private static final long serialVersionUID = 1L;
    
    // clock/node information
    private HashMap<String, Node> nodeMap;
    private HashMap<String, AtomicInteger> time;
    
    /**
     * VectorTimeStamp constructor
     * 
     * @param nodeMap - hash map of the nodes in the system
     */
    public VectorTimeStamp(HashMap<String, Node> nodeMap) {
        this.nodeMap = nodeMap;
        this.time = new HashMap<String, AtomicInteger>(nodeMap.size());
        for(Entry<String, Node> current : nodeMap.entrySet()) {
            this.time.put(current.getKey(), new AtomicInteger(0));
        }
    }  
    
    // get time stamp
    @Override
    public HashMap<String, AtomicInteger> getTimeStamp() {
        return this.time;
    }
    
    // set time stamp
    @Override 
    @SuppressWarnings("unchecked")
    public void setTimeStamp(Object o) {
        this.time = ((HashMap<String, AtomicInteger>) o);
    }
    
    // get copy of time stamp
    @Override
    public TimeStamp copyOf() {
        VectorTimeStamp copy = new VectorTimeStamp(this.nodeMap);
        HashMap<String, AtomicInteger> copyMap = copy.getTimeStamp();
        for(Entry<String, AtomicInteger> entry : copyMap.entrySet()) {
            int copyTime = this.time.get(entry.getKey()).get(); 
            copyMap.put(entry.getKey(), new AtomicInteger(copyTime));
        }
        return copy;
    }
    
    /**
     * compareTo - help order events.
     * 
     * @param compare - timeStamp to which to compare this timeStamp 
     */
    @Override
    public int compareTo(VectorTimeStamp compare) {
        // boolean flags to determin order of events 
        boolean allBefore = true;
        boolean allAfter = true;
        
        // integer describing the current time and comparison time at a node
        int localTime;
        int compareTime;
        
        // loop through all nodes 
        for(Entry<String, AtomicInteger> entry : time.entrySet()) {
            localTime = entry.getValue().get();
            compareTime = compare.getTimeStamp().get(entry.getKey()).get();
            
            // compare time
            if(localTime < compareTime) {
                allAfter = false;
            }
            if (localTime > compareTime) {
                allBefore = false;
            }
        }
        
        // determine the order of these two events
        if(allBefore && !allAfter) {
            return -1;
        } else if (!allBefore && allAfter) {
            return 1;
        } else {
            return 0;
        }

    }
    
    @Override
    public String toString() {
        return this.time.toString();
    }
    
}