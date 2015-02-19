package main.java.interFileCreator;
import java.util.ArrayList;
import java.util.TreeMap;


public class Document {
	
	public Document(){
		proteins = new TreeMap<String, ArrayList<String>>();
		locations = new TreeMap<String, ArrayList<String>>();
		organisms = new TreeMap<String, String>();
	}
	String docId;
	
	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}
	
	// mapping proteinName, list of ids
	TreeMap<String,ArrayList<String>> proteins;
	TreeMap<String,ArrayList<String>> locations;
	TreeMap<String,String> organisms;
	
	public void addProtein(String proteinName, String id){
		if(proteins.containsKey(proteinName)){
			ArrayList<String> idList = proteins.get(proteinName);
			if(!idList.contains(id)){
				idList.add(id);
				proteins.put(proteinName, idList);
			}
		} else {
			ArrayList<String> idList = new ArrayList<String>();
			idList.add(id);
			proteins.put(proteinName, idList);
		}
	}
	
	public void addLocation(String locationName, String term){
		if(locations.containsKey(locationName)){
			ArrayList<String> termList = locations.get(locationName);
			if(!termList.contains(term)){
				termList.add(term);
				locations.put(locationName, termList);
			}
		} else {
			ArrayList<String> termList = new ArrayList<String>();
			termList.add(term);
			locations.put(locationName, termList);
		}
	}
	
	public void addOrganism(String organismName){
		if(organismName.contains("human")){
			organisms.put(organismName, "9606");
		} else if(organismName.contains("arath")){
			organisms.put(organismName, "3702");
		}  else if(organismName.contains("arath")){
			organisms.put(organismName, "3702");
		}  else if(organismName.contains("yeast")){
			organisms.put(organismName, "4932");
		}
	}

	public TreeMap<String, ArrayList<String>> getProteins() {
		return proteins;
	}

	public void setProteins(TreeMap<String, ArrayList<String>> proteins) {
		this.proteins = proteins;
	}

	public TreeMap<String, ArrayList<String>> getLocations() {
		return locations;
	}

	public void setLocations(TreeMap<String, ArrayList<String>> locations) {
		this.locations = locations;
	}

	public TreeMap<String, String> getOrganisms() {
		return organisms;
	}

	public void setOrganisms(TreeMap<String, String> organisms) {
		this.organisms = organisms;
	}

}
