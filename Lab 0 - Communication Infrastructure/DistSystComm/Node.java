/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 0: Communications Infrastructure
 * Node.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

// Node is a unique class that does not implement/extend anything.
public class Node {
    // native node fields    
    private String name;
    private String IP;
    private int port;
    
    /**
     * Empty node constructor - used by Utility.java 
     */
    public Node(){}
    
    /**
     * Node constructor 
     * 
     * @param name - name of the node
     * @param ip - IP address of the node
     * @param port - the port on which this node will be listening
     */
    public Node(String name, String ip, int port) {
        this.name = name;
        this.IP = ip;
        this.port = port;
    }

    // NAME methods
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // IP methods
    public void setIP(String IP) {
        this.IP = IP;
    }
    
    public String getIP() {
        return IP;
    }

    // PORT methods
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getPort() {
        return port;
    }

    // Return full node definition to a string
    @Override
    public String toString() {
        return "ND[name=" + name + ", IP=" + IP + ", port=" + port + "]";
    }
}
