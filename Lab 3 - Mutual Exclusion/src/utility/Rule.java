/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 3: Mutual Exclusion
 * Rule.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

// Node is a unique class that does not implement/extend anything.
public class Rule {
    // ennumeration of possible actions
    public enum Action {
        DROP, DROPAFTER, DELAY, NIL;
    }

    // native fields of a rule
    private String kind;        // send/recieve
    private Action action;      // drop/dropAfter/delay/nil
    private String source;      // source node for this rule
    private String destination; // destination node for this rule
    private int seqNum = -1;    // message sequence number for this rule

    // KIND method
    public void setKind(String kind) {
        this.kind = kind;
    }
    
    public String getKind() {
        return kind;
    }

    // ACTION methods
    public void setAction(Action action) {
        this.action = action;
    }
    
    public Action getAction() {
        return action;
    }

    // SOURCE methods
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSource() {
        return source;
    }

    // DESTINATION methods
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    // SEQUENCENUMBER methods
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }
    
    public int getSeqNum() {
        return seqNum;
    }

    /**
     * ruleApplies - determines if this rule applies to a message
     * 
     * @param message - message to which this rule will be compared
     * @returns boolean indicating the application of this rule to the message
     */
    public boolean ruleApplies(TimeStampedMessage message) {
    	Action action = this.getAction();
    	
    	// switch/case statement based on the action
    	switch(action) {
    		// NIL: implicitly, the rule cannot apply
    		case NIL: 
    			return false;
    		// DROP/DELAY: the conditionals for these are the same, therefore this
    		//	is intentional fall-through
    		case DROP:
    		case DELAY:
				if((this.getSource() == null) || this.getSource().equalsIgnoreCase(message.getSource())) {
					if((this.getDestination() == null) || this.getDestination().equalsIgnoreCase(message.getDestination())) {
						if((this.getKind() == null) || this.getKind().equalsIgnoreCase(message.getKind())) {
							if((this.getSeqNum() < 0) || (this.getSeqNum() == message.getSeqNum())) {
								return true;
							}
						}
					}
				}
				return false;
			// DROPAFTER: the conditional for this is very similar to DROP/DELAY
			//	with a slight change in the sequence number conditional.
			case DROPAFTER:
				if((this.getSource() == null) || this.getSource().equalsIgnoreCase(message.getSource())) {
					if((this.getDestination() == null) || this.getDestination().equalsIgnoreCase(message.getDestination())) {
						if((this.getKind() == null) || this.getKind().equalsIgnoreCase(message.getKind())) {
							if((this.getSeqNum() < 0) || (this.getSeqNum() < message.getSeqNum())) {
								return true;
							}
						}
					}
				}
				return false;
    	}
    	return false;
    }

    @Override
    public String toString() {
        return "R[kind=" + kind + ", src=" + source + 
                ", dest=" + destination + ", srcNum=" + seqNum +"]";
    }
}
