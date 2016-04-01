/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * VectorClockService.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */
 
// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * VectorClockService extends ClockService
 * 
 * Description:
 *  - VectorClockService provides and interface to the VectorTimeStamp class.
 */
public class VectorClockService extends ClockService {
    
    /**
     * VectorClockService constructor 
     * 
     * @param localName - name of the node who owns this clock
     * @param nodeMap - map of the nodes in the system
     */    
    public VectorClockService(String localName, HashMap<String, Node> nodeMap) {
        this.localName = localName;
        this.currentTime = new VectorTimeStamp(nodeMap);
    }
    
    // Increment the time for the current node
    @Override
    public void incrementTime() {
        // get the current time
        VectorTimeStamp current = (VectorTimeStamp) this.getCurrentTime();
        AtomicInteger local = current.getTimeStamp().get(localName);
        
        // increment and update
        int newTime = local.get() + step;
        current.getTimeStamp().put(localName, new AtomicInteger(newTime));
    }

    // Increment the current time based on the received time
    @Override
    public void incrementTimeOnReceive(TimeStamp rt) {
        // get the local and received time
        HashMap<String, AtomicInteger> local = ((VectorTimeStamp) this.getCurrentTime()).getTimeStamp();
        HashMap<String, AtomicInteger> received = ((VectorTimeStamp) rt).getTimeStamp();

        // increment and update
        for(Entry<String, AtomicInteger> entry : local.entrySet()) {
            int localTime = entry.getValue().get();
            int receivedTime = received.get(entry.getKey()).get();
            int newTime = Math.max(localTime, receivedTime);
            if(entry.getKey().equals(localName)) {
                newTime += 1;
            }
            local.put(entry.getKey(), new AtomicInteger(newTime));
        }
    }
    
    // Increment the current time based on the received time
    @Override
    public void incrementTimeOnMulticastReceive(TimeStamp rt) {
        // get the local and received time
        HashMap<String, AtomicInteger> local = ((VectorTimeStamp) this.getCurrentTime()).getTimeStamp();
        HashMap<String, AtomicInteger> received = ((VectorTimeStamp) rt).getTimeStamp();

        // increment and update
        for(Entry<String, AtomicInteger> entry : local.entrySet()) {
            int localTime = entry.getValue().get();
            int receivedTime = received.get(entry.getKey()).get();
            int newTime = Math.max(localTime, receivedTime); 
            local.put(entry.getKey(), new AtomicInteger(newTime));
        }
    }
}