/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * MulticastPayload.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */
 
// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.ArrayList;

/**
 * MulticastPayload implements java.io.Serializable
 * 
 * Description:
 *  - define a payload for multicast payload
 */
public class MulticastPayload implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // native timestamp field
    private ArrayList<Object> multicastPayload;
    
    /**
     * MulticastMessage constructor 
     * 
     * @param groupName - name of the group to which this is being delivered
     * @param payload - payload of the message
     */
    public MulticastPayload(String groupName, Object payload) {
        multicastPayload = new ArrayList<Object>(2);
        multicastPayload.add(0, groupName);
        multicastPayload.add(1, payload);
    }
    
    /** 
     * MulticastMessage constructor (empty) 
     */
    public MulticastPayload() {
        multicastPayload = new ArrayList<Object>(2);
    }
    
    /**
     * setGroupName
     * 
     * @param groupName - new group name
     */
    public void setGroupName(String groupName) {
        this.multicastPayload.set(0, groupName);
    }
    
    // groupName - get the current group name
    public String getGroupName() {
        return (String)this.multicastPayload.get(0);
    }
    

    // setPayload - set the payload of this message
    public void setPayload(Object payload) {
        this.multicastPayload.set(1, payload);
    }
    
    
    // getPayload - return the payload
    public Object getPayload() {
        return this.multicastPayload.get(1);
    }
    
    public String toString() {
        return "MCP[grp=" + this.getGroupName() + ", payload=" + this.getPayload() + "]";
    }
}