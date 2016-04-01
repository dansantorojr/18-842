
// implement a package for this lab assignment
package DistSystComm;
import DistSystComm.ClockService.ClockType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class GroupClockService {
    private HashMap<String, ClockService> groupClocks;
    private String localName;
    
    public GroupClockService(ClockType clockType, String localName, 
                                    HashMap<String, Node> nodeMap, 
                                    HashMap<String, Group> groupMap) {
        groupClocks = new HashMap<String, ClockService>();
        for(Map.Entry<String, Group> entry : groupMap.entrySet()){
            ArrayList<String> g = entry.getValue().getGroup();
            if(g.contains(localName)) {
                ClockService gc = ClockService.getClockService(clockType, localName, nodeMap);
                this.groupClocks.put(entry.getKey(), gc);
            }
        }
    }
    
    public TimeStamp getCurrentTime(String groupName) {
        return this.groupClocks.get(groupName).getCurrentTime();
    }
    
    public void incrementTime(String groupName) {
        this.groupClocks.get(groupName).incrementTime();
    }
    
    public void incrementTimeOnReceive(String groupName, TimeStamp ts) {
        this.groupClocks.get(groupName).incrementTimeOnReceive(ts);
    }
    
    public void incrementTimeOnMulticastReceive(String groupName, TimeStamp ts) {
        this.groupClocks.get(groupName).incrementTimeOnMulticastReceive(ts);
    }
}