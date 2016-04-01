/**
 * 18-842 Distributed Systems
 * Spring 2016
 * Lab 2: Multicast
 * Utility.java
 * Daniel Santoro // ddsantor. Akansha Patel // akanshap.
 */

// implement a package for this lab assignment
package DistSystComm;

import DistSystComm.Rule.Action;
import DistSystComm.ClockService.ClockType;

// import utility packages
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
// import io packages
import java.io.*;

// import snakeyaml package
import org.yaml.snakeyaml.Yaml;

/**
 * Utility is a unique class and does not implement/extend anything.
 * 
 * - Utility is reponsible for reading the configuration/send rules/receive
 * rules out of the configuration file. - Utility also houses the "ruleApplies"
 * function that determines if there is some action required based on the
 * current message and the current state of the send and receive rules.
 */
public class ConfigParser {

	public static String configFilename;

	/**
	 * readFile - read the configuration file. - this method is private and is
	 * only used within this class.
	 * 
	 * @param configFilenam
	 *            - path to the configuration file.
	 * @returns Map of the parsed Yaml file
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> readFile() {
		// create new Yaml and Map objects
		Yaml yaml = new Yaml();
		Map<String, Object> data = null;

		// attempt reading the configuration file. If the configuration file
		// cannot
		// cannot be found, exit.
		try {
			InputStream input = new FileInputStream(new File(configFilename));
			data = (Map<String, Object>) yaml.load(input);
		} catch (Exception e) {
			System.err.println("READ ERROR: " + configFilename + " cannot be found.");
			System.exit(0);
		}
		return data;
	}

	/**
	 * readConfiguration - read the configuration out of the configuration file
	 * 
	 * @param configFilename
	 *            - path to the configuration file
	 * @returns HashMap of nodes present in the configuration file
	 */
	public static HashMap<String, Node> readConfiguration() {
		// read configuration file and create new hashmap
		Map<String, Object> config = readFile();
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();

		// loop through all entries in the config Map
		for (Entry<String, Object> entry : config.entrySet()) {
			// only deal with the configuration
			if (entry.getKey().equalsIgnoreCase("configuration")) {
				// iterate through all configuration entries
				@SuppressWarnings("unchecked")
				Iterator<Map<String, Object>> iterator = ((ArrayList<Map<String, Object>>) entry.getValue()).iterator();
				while (iterator.hasNext()) {
					// iterate through all fields within the configuration entry
					Node node = new Node();
					for (Map.Entry<String, Object> entry1 : iterator.next().entrySet()) {
						if (entry1.getKey().equalsIgnoreCase("name")) {
							node.setName(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("ip")) {
							node.setIP(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("port")) {
							node.setPort(Integer.parseInt(entry1.getValue().toString()));
						}
					}
					nodeMap.put(node.getName(), node);
				}
			}
		}
		return nodeMap;
	}
	
	// NOTE: This should eventually return a HashMap<String, Group>
	public static HashMap<String, Group> readGroups() {
		
		Map<String, Object> config = readFile();
		HashMap<String, Group> groupMap = new HashMap<String, Group>();
		
		for (Entry<String, Object> entry : config.entrySet()) {
			// only deal with the configuration
			if (entry.getKey().equalsIgnoreCase("groups")) {
				// iterate through all configuration entries
				@SuppressWarnings("unchecked")
				Iterator<Map<String, Object>> iterator = ((ArrayList<Map<String, Object>>) entry.getValue()).iterator();
				while (iterator.hasNext()) {
					// iterate through all fields within the configuration entry
					Group group = new Group();
					for (Map.Entry<String, Object> entry1 : iterator.next().entrySet()) {
						if (entry1.getKey().equalsIgnoreCase("name")) {
							group.setName(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("members")) {
							@SuppressWarnings("unchecked")
							Iterator<String> list = ((ArrayList<String>) entry1.getValue()).iterator();
							while(list.hasNext()){
								group.addNode(list.next());
							}
						}
					}
					groupMap.put(group.getName(), group);
				}

			}
		}
		return groupMap;
	}

	/**
	 * readSendRules - read send rules out of the configuration file
	 * 
	 * @param configFilename
	 *            - path to the configuration file
	 * @returns ArrayList of rules
	 */
	public static ArrayList<Rule> readSendRules() {
		// read file and initialize the ArrayList
		Map<String, Object> config = readFile();
		ArrayList<Rule> sRules = new ArrayList<Rule>();

		// loop through all entries in the config map
		for (Entry<String, Object> entry : config.entrySet()) {
			// only deal with send rules
			if (entry.getKey().equalsIgnoreCase("sendRules")) {
				// iterate through all send rule entries
				@SuppressWarnings("unchecked")
				Iterator<Map<String, Object>> iterator = ((ArrayList<Map<String, Object>>) entry.getValue()).iterator();
				while (iterator.hasNext()) {
					Rule rule = new Rule();
					// iterate through all fields within a send rule entry
					for (Map.Entry<String, Object> entry1 : iterator.next().entrySet()) {
						// determine the action to be taken
						if (entry1.getKey().equalsIgnoreCase("Action")) {
							if (entry1.getValue().toString().equalsIgnoreCase("drop")) {
								rule.setAction(Action.DROP);
							} else if (entry1.getValue().toString().equalsIgnoreCase("dropAfter")) {
								rule.setAction(Action.DROPAFTER);
							} else if (entry1.getValue().toString().equalsIgnoreCase("delay")) {
								rule.setAction(Action.DELAY);
							} else {
								rule.setAction(Action.NIL);
							}
						}
						// set all other fields
						else if (entry1.getKey().equalsIgnoreCase("src")) {
							rule.setSource(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("dest")) {
							rule.setDestination(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("kind")) {
							rule.setKind(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("seqNum")) {
							rule.setSeqNum(Integer.parseInt(entry1.getValue().toString()));
						}
					}
					sRules.add(rule);
				}
			}
		}
		return sRules;
	}

	/**
	 * readReceiveRules - read the receive rules out of the configuration file
	 * 
	 * @param configFilename
	 *            - path to the configuration file
	 * @returns ArrayList of rules
	 */
	public static ArrayList<Rule> readReceiveRules() {
		// read file and initialize the ArrayList
		Map<String, Object> config = readFile();
		ArrayList<Rule> rRules = new ArrayList<Rule>();

		// loop through all entries in the config map
		for (Entry<String, Object> entry : config.entrySet()) {
			// only deal with receive rules
			if (entry.getKey().equalsIgnoreCase("receiveRules")) {
				// iterate through all receive rule entries
				@SuppressWarnings("unchecked")
				Iterator<Map<String, Object>> iterator = ((ArrayList<Map<String, Object>>) entry.getValue()).iterator();
				while (iterator.hasNext()) {
					Rule rule = new Rule();
					// iterate through all fields within a receive rule entry
					for (Map.Entry<String, Object> entry1 : iterator.next().entrySet()) {
						// determine the action to be taken
						if (entry1.getKey().equalsIgnoreCase("Action")) {
							if (entry1.getValue().toString().equalsIgnoreCase("drop")) {
								rule.setAction(Action.DROP);
							} else if (entry1.getValue().toString().equalsIgnoreCase("dropAfter")) {
								rule.setAction(Action.DROPAFTER);
							} else if (entry1.getValue().toString().equalsIgnoreCase("delay")) {
								rule.setAction(Action.DELAY);
							} else {
								rule.setAction(Action.NIL);
							}
						}
						// set all other fields
						else if (entry1.getKey().equalsIgnoreCase("src")) {
							rule.setSource(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("dest")) {
							rule.setDestination(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("kind")) {
							rule.setKind(entry1.getValue().toString());
						} else if (entry1.getKey().equalsIgnoreCase("seqNum")) {
							rule.setSeqNum(Integer.parseInt(entry1.getValue().toString()));
						}
					}
					rRules.add(rule);
				}
			}
		}
		return rRules;
	}

	/**
	 * systemClockType - read the system clock type from the configuration file
	 * @return 
	 * @returns type of Clock, Vector or Logical
	 */
	public static ClockType systemClockType() {

		Map<String, Object> config = readFile();
		ClockType type = null;
		for (Map.Entry<String, Object> entry : config.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("systemClockType")) {
				if (entry.getValue().toString().equalsIgnoreCase("vector"))
					type = ClockType.VECTOR;
				else
					type = ClockType.LOGICAL;
			}
		}
		return type;
	}
}