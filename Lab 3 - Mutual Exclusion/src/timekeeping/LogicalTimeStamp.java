/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * LogicalTimeStamp.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */
 
// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LogicalTimeStamp extends TimeStamp implements Compareable
 * 
 * Description:
 *  - LocgicalTimeStamp extends the TimeStamp class to be used by the LogicalClockService
 *      class.
 *  - LogicalTimeStamp uses an AtomicInteger as a time stamp
 *  - LogicalTimeStamp implements Comparable for system symmetry. This system does
 *      not order logical time stamps on a system scale, but this class was 
 *      written to mirror VectorTimeStamp which requires "Comparable" to easily
 *      order the log.
 */
public class LogicalTimeStamp extends TimeStamp implements Comparable<LogicalTimeStamp> {
    // set serial version - Why? Consistency?
    private static final long serialVersionUID = 1L;

    // clock implementation
    private AtomicInteger time;
    
    /**
     * LogicalTimeStamp constructor
     */
    public LogicalTimeStamp() {
        time = new AtomicInteger(0);
    }
    
    // get time stamp
    @Override
    public AtomicInteger getTimeStamp() {
        return this.time;
    }
    
    // set the time stamp
    @Override 
    public void setTimeStamp(Object o) {
        this.time = (AtomicInteger) o;
    }
    
    // create a copy of a time stamp
    @Override
    public TimeStamp copyOf() {
        TimeStamp copy = new LogicalTimeStamp();
        copy.setTimeStamp(this.time);
        return copy;
    }
    
    /**
     * compareTo  - 
     * 
     * return zero because logical clocks cannot be placed in appropriate
     *  time order without information about event dependencies.
     */
    @Override
    public int compareTo(LogicalTimeStamp compare) {
        return 0;
    }
    
    @Override
    public String toString() {
        return this.time.toString();
    }
}
