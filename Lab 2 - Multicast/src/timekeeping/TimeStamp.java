/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * TimeStamp.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;

/**
 * Timestamp implements serializable
 * 
 * Description:
 *  - This is an abstract implementation because it will be extended by 
 *      LogicalTimeStamp and VectorTimeStamp
 */
public abstract class TimeStamp implements java.io.Serializable {
    // set serial version - Why? Consistency?
    private static final long serialVersionUID = 1L;
    
    // set time stamp
    abstract public void setTimeStamp(Object o);
    
    // get time stamp
    abstract public Object getTimeStamp();
    
    // get a copy of the time stamp
    abstract public TimeStamp copyOf();
}