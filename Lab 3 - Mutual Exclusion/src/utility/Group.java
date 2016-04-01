/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * Group.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */
 
// implement a package for this lab assignment
package DistSystComm;

// import utility packages
import java.util.ArrayList;

/** 
 * Group - 
 * 
 * Description:
 *  - defines a group of nodes for multicasting
 */
public class Group {
    private String name;
    private ArrayList<String> groupList;
    
    // Constructor
    public Group() {
        groupList = new ArrayList<String>();
    }
    
    // addNode - add a node to the group
    public void addNode(String n) {
        this.groupList.add(n);
    }
    
    // removeNode - remove a node from the group
    public void removeNode(String n) {
        this.groupList.remove(n);
    }
    
    // getSize - get the size of the group
    public int getSize() {
    	return this.groupList.size();
    }
    
    // getGroup - return the group
    public ArrayList<String> getGroup() {
        return groupList;
    }

    // getName - get the name of the group
    public String getName() {
        return this.name;
    }
    
    // setName - set the name of the group
    public void setName(String name) {
       this.name = name;
    }

    public String toString() {
        // return the name?
        return "[Group name= " + this.name + ", Members= " + this.groupList + "]";
    }
}