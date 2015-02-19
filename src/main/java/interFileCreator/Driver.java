package main.java.interFileCreator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import main.java.utils.Entity;
import main.java.utils.Filter;
import main.java.utils.JsonReader;

public class Driver {

	public static void main(String[] args) {
		String jsonDirectory = "/home/shrikant/NLPRE/shrikant-master-thesis/LocTextCorpus/annotations/";
		
		HashMap<String,String> orgIdMap = new HashMap<String, String>();
		populateOrgMap(orgIdMap);
		
		ArrayList<Document> docList = new ArrayList<Document>();
		HashMap<String,Integer> docIndex = new HashMap<String, Integer>();

		readNormalizedAnnotation(docList, docIndex);

		writeIntermediateFile(jsonDirectory, docList, docIndex, orgIdMap); 
	}

	private static void populateOrgMap(HashMap<String, String> orgIdMap) {
		orgIdMap.put("Arabidopsis", "3702");
		orgIdMap.put("Arabidopsis Thaliana", "3702");
		orgIdMap.put("Arabidopsis thaliana", "3702");
		orgIdMap.put("Saccharomyces cerevisiae", "4932");		
	}

	private static void writeIntermediateFile(String jsonDirectory, ArrayList<Document> docList, HashMap<String, Integer> docIndex, HashMap<String, String> orgIdMap) {
		try {
			FileWriter fw = new FileWriter("workDir/interFile.tsv");
			fw.write("PubMedId" + "\t" + "Entity" + "\t" + "EntityType" + "\t" + "NormalizedID" + "\t" + "title/abs" + "\t" + "Start" + "\t" + "End" + "\n");

			// Reading HTML files
			Filter files = new Filter();
			File[] fileList = files.finder(jsonDirectory, ".json");

			for(File newFile:fileList) {
				JsonReader reader = new JsonReader(newFile.getAbsolutePath());
				String pubmedId = newFile.getName().replace(".json", "");
				ArrayList<Entity> titleEntities = reader.getTitleEntities();
				ArrayList<Entity> absEntities = reader.getAbsEntities();

				writeToFile(pubmedId, titleEntities, absEntities, pubmedId, docList, docIndex, orgIdMap, fw);
			}
			fw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void readNormalizedAnnotation(ArrayList<Document> docList, HashMap<String, Integer> docIndex) {

		try {
			BufferedReader br = new BufferedReader(new FileReader("workDir/normalizationFile.tsv"));
			br.readLine(); // first line

			String line;
			while((line=br.readLine())!=null){
				String tokens[] = line.split("\\t+");
				String docId = tokens[2];

				Document currentDoc = null;
				if(docIndex.containsKey(docId)){
					currentDoc = docList.get(docIndex.get(docId));
				} else {
					currentDoc = new Document();
					currentDoc.setDocId(docId);
					docList.add(currentDoc);
					docIndex.put(docId, docList.size()-1);
				}

				String organismName = tokens[0].trim();
				currentDoc.addOrganism(organismName);

				String protId = tokens[1].trim();
				String protList = tokens[3].trim();
				String proteins[] = protList.split(",");

				for(int i=0; i<proteins.length; i++){
					String protein = proteins[i].trim();

					if(!protein.isEmpty() && !protein.equals("0") && 
							!protId.isEmpty() && !protId.equals("0")){
						currentDoc.addProtein(protein, protId);
					}
				}
				//				if(!tokens[4].trim().isEmpty() && !tokens[5].trim().isEmpty()) {
				if(tokens.length==6){
					String localizationEntity = tokens[4].trim();
					String localizationIds = tokens[5].trim();

					String locTokens[] = localizationEntity.split(",");
					String locIdTokens[] = localizationIds.split(";");

					if(locTokens.length != locIdTokens.length ){
						System.out.println("DocId: " + docId + " #LocTokenMisMatch\t" + localizationEntity + "\t" + localizationIds);
					} else {
						for(int i=0; i<locTokens.length;i++){
							String location = locTokens[i].trim();
							String locId = locIdTokens[i].trim();

							if(!location.isEmpty() &&
									!locId.isEmpty() &&
									!location.equals("0") &&
									!locId.equals("0")) {
								currentDoc.addLocation(location, locId);
							}
						}
					}
				}
			}
			br.close();

			FileWriter fw = new FileWriter("workDir/semiNormalized.tsv");
			for(Document doc:docList){
				Iterator<Entry<String,ArrayList<String>>> iter = doc.getProteins().entrySet().iterator();
				while(iter.hasNext()){
					Entry<String,ArrayList<String>> pair = iter.next();
					String proteinName = pair.getKey();;
					ArrayList<String> protIdList = pair.getValue();

					String idList = "";
					for(String id:protIdList){
						if(!idList.isEmpty()){
							idList += ",";
						}
						idList += id;
					}

					fw.write(doc.getDocId()+ "\t" + "Protein" + "\t" + proteinName + "\t" + idList + "\n");
				}

				Iterator<Entry<String,ArrayList<String>>> iter1 = doc.getLocations().entrySet().iterator();
				while(iter1.hasNext()){
					Entry<String,ArrayList<String>> pair = iter1.next();
					String locationName = pair.getKey();;
					ArrayList<String> locIdList = pair.getValue();

					String idList = "";
					for(String id:locIdList){
						if(!idList.isEmpty()){
							idList += ",";
						}
						idList += id;
					}

					fw.write(doc.getDocId()+ "\t" + "Location" + "\t" + locationName + "\t" + idList + "\n");
				}

				Iterator<Entry<String,String>> iter2 = doc.getOrganisms().entrySet().iterator();
				while(iter2.hasNext()){
					Entry<String,String> pair = iter2.next();
					String orgName = pair.getKey();;
					String orgId = pair.getValue();

					fw.write(doc.getDocId()+ "\t" + "Organism" + "\t" + orgName + "\t" + orgId + "\n");
				}
			}
			fw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static void writeToFile(String pubmedId,
			ArrayList<Entity> titleEntities, ArrayList<Entity> absEntities,
			String id, ArrayList<Document> docList, HashMap<String, Integer> docIndex, HashMap<String, String> orgIdMap, FileWriter fw) throws IOException {

		Document doc = null;
		if(docIndex.containsKey(pubmedId)) {
			doc = docList.get(docIndex.get(pubmedId));	
		}

		for(Entity ent:titleEntities){
			String writeString = "";
			writeString += id + "\t";
			writeString += ent.getText() + "\t";

			if(ent.getType()==Entity.EntityType.Protein){
				writeString += "Protein" + "\t";

				String proteinName = ent.getText();				
				if(doc!=null && doc.getProteins().containsKey(proteinName)) {
					ArrayList<String> protIdList = doc.getProteins().get(proteinName); 

					String idList = "";
					for(String pId:protIdList){
						if(!idList.isEmpty()){
							idList += ",";
						}
						idList += pId;
					}
					writeString += idList + "\t";

				} else {
					writeString += "TODO" + "\t";		
				}
			} else if(ent.getType()==Entity.EntityType.Location) {
				writeString += "Location" + "\t";

				String locName = ent.getText();				
				if(doc!=null && doc.getLocations().containsKey(locName)) {
					ArrayList<String> locIdList = doc.getLocations().get(locName); 

					String idList = "";
					for(String lId:locIdList){
						if(!idList.isEmpty()){
							idList += ",";
						}
						idList += lId;
					}
					writeString += idList + "\t";

				} else {
					writeString += "TODO" + "\t";		
				}
			} else if(ent.getType()==Entity.EntityType.Organism) {
				writeString += "Organism" + "\t";

				String orgName = ent.getText();				
				if(doc!=null && doc.getOrganisms().containsKey(orgName)) {
					writeString += doc.getOrganisms().get(orgName) + "\t";
				} else if(orgIdMap.containsKey(orgName)){
					writeString += orgIdMap.get(orgName) + "\t";
				} else {
					writeString += "TODO" + "\t";		
				}
			}
			// TODO: Comment stuff below
			writeString += "title" + "\t";
			writeString += ent.getStart() + "\t";
			writeString += ent.getEnd();

			fw.write(writeString + "\n");
		}

		for(Entity ent:absEntities){
			String writeString = "";
			writeString += id + "\t";
			writeString += ent.getText() + "\t";

			if(ent.getType()==Entity.EntityType.Protein){
				writeString += "Protein" + "\t";
				

				String proteinName = ent.getText();				
				if(doc!=null && doc.getProteins().containsKey(proteinName)) {
					ArrayList<String> protIdList = doc.getProteins().get(proteinName); 

					String idList = "";
					for(String pId:protIdList){
						if(!idList.isEmpty()){
							idList += ",";
						}
						idList += pId;
					}
					writeString += idList + "\t";

				} else {
					writeString += "TODO" + "\t";		
				}
			} else if(ent.getType()==Entity.EntityType.Location) {
				
				writeString += "Location" + "\t";

				String locName = ent.getText();				
				if(doc!=null && doc.getLocations().containsKey(locName)) {
					ArrayList<String> locIdList = doc.getLocations().get(locName); 

					String idList = "";
					for(String lId:locIdList){
						if(!idList.isEmpty()){
							idList += ",";
						}
						idList += lId;
					}
					writeString += idList + "\t";

				} else {
					writeString += "TODO" + "\t";		
				}
			} else if(ent.getType()==Entity.EntityType.Organism) {
				
				writeString += "Organism" + "\t";

				String orgName = ent.getText();				
				if(doc!=null && doc.getOrganisms().containsKey(orgName)) {
					writeString += doc.getOrganisms().get(orgName) + "\t";
				} else if(orgIdMap.containsKey(orgName)){
					writeString += orgIdMap.get(orgName)  + "\t";
				} else {
					writeString += "TODO" + "\t";		
				}
			}

			// TODO: Comment stuff below
			writeString += "abs" + "\t";
			writeString += ent.getStart() + "\t";
			writeString += ent.getEnd();

			fw.write(writeString + "\n");
		}
	}


}
