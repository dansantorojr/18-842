// Reliable Causally Ordered Multicast
public void rcoMulticast(Message m, Group g) {
    g = incrementTime();    // increment the time
    m.setTimeStamp(g);      // set the time stamp   
    for(all nodes in g) {   // send the message
        send(m);
    }
}

// Reliable Causally Ordered Deliver
public Message rcoDeliver(Message m) {
    if((m not it Received) and (m not in HoldBack)) {
        HoldBack.add(m) // add the message to the received list
        if(myName is not m.getSource()) {
            rcoMulticast(m, g);
        }
        if(HoldBack is ordered correctly) {
            for(all heldMessage in HoldBack) {
                Received.add(heldMessage)
            }
        }
    }
}

// Things that need implemented apart from rcoMulticast + rcoDeliver 
utility class Group -> Implemented as an array list of node names (strings)
ConfigParser -> GroupMap -> hashap with group name as a key, and group object as the object