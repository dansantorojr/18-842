/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * LogicalClockService.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LogicalClockService extends ClockService
 * 
 * Description:
 *  - LogicalClockService provides and interface to the LogicalTimeStamp class.
 */
public class LogicalClockService extends ClockService {
    
    /**
     * LogicalClockService constructor 
     * 
     * @param localName - name of the node who owns this clock
     */
    public LogicalClockService(String localName) {
        this.localName = localName;
        this.currentTime = new LogicalTimeStamp();
    }
    
    // Increment the current time by the step size.
    @Override
    public void incrementTime() {
        LogicalTimeStamp ct = (LogicalTimeStamp) this.getCurrentTime();
        int newTime = ct.getTimeStamp().get() + step;
        ct.setTimeStamp(new AtomicInteger(newTime));
    }
    
    // Increment the current time based on the received time
    @Override
    public void incrementTimeOnReceive(TimeStamp rt) {
        // get the local and received time
        int local = ((AtomicInteger)this.getCurrentTime().getTimeStamp()).get();
        int received = ((AtomicInteger)rt.getTimeStamp()).get();
        
        // increment and update
        int newTime = Math.max(local, received) + 1;
        this.getCurrentTime().setTimeStamp(new AtomicInteger(newTime));
    }
    
    @Override
    public void incrementTimeOnMulticastReceive(TimeStamp rt) {
        incrementTimeOnReceive(rt);
    }
}