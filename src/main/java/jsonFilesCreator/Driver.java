package main.java.jsonFilesCreator;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import main.java.utils.Entity;
import main.java.utils.Entity.EntityPart;
import main.java.utils.Entity.EntityType;
import main.java.utils.Filter;
import main.java.utils.JsonReader;
import main.java.utils.Relation;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Driver {

	public static void main(String[] args) {
		
		String titleAbsConcatStr = " ";
		String htmlDirectory = "/home/shrikant/NLPRE/shrikant-master-thesis/LocTextCorpus/html/";
		String jsonDirectory = "/home/shrikant/NLPRE/shrikant-master-thesis/LocTextCorpus/annotations/";
		String outputDirectory = "workDir/jsonFiles/";
		String intermediateFilePath = "";
		boolean allowPORelations = false;

		// read normalized file
		ArrayList<Document> docList = new ArrayList<Document>();
		HashMap<String,Integer> docIndex = new HashMap<String, Integer>();
		readIntermediateFile(intermediateFilePath, docList,docIndex);
		
		// Reading HTML files
		Filter files = new Filter();
		File[] fileList = files.finder(htmlDirectory, ".html");

		for(File newFile:fileList) {
			
			htmlParser parser = new htmlParser(newFile.getAbsolutePath());
			
			String pubmedId = newFile.getName().replace(".html", "");
			
			String jsonFilePath = jsonDirectory + pubmedId + ".json";
			JsonReader reader = new JsonReader(jsonFilePath);			
			
			writeJsonFile(pubmedId, docList, docIndex,parser,reader,outputDirectory, titleAbsConcatStr, allowPORelations);
			break;
		}
	}

	private static void writeJsonFile(String pubmedId,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex,
			htmlParser parser, JsonReader reader, String outputDirectory,
			String titleAbsConcatStr, boolean allowPORelations) {
		
		JSONObject jsonObject = new JSONObject();
		
		String concatenatedText = parser.getTitle() + titleAbsConcatStr + parser.getAbstract();
		int offSetAddition = parser.getTitle().length()+titleAbsConcatStr.length();
		
		jsonObject.put("text", concatenatedText);
		jsonObject.put("sourcedb", "PubMed");
		jsonObject.put("sourceid",pubmedId);
		
		int entCounter = 1;
		
		HashMap<Integer,String> idMap = new HashMap<Integer,String>();
		
		JSONArray denotationArr = new JSONArray();
		for(Entity ent:reader.getTitleEntities()){
			JSONObject entObj = new JSONObject();
			String identifier = "T" + entCounter++;
			entObj.put("id", identifier);
			
			JSONObject spanObj = new JSONObject();
			int start = ent.getStart();
			int end = ent.getEnd();
			
			idMap.put(start, identifier);
			
			spanObj.put("begin", start);
			spanObj.put("end", end );
			
			entObj.put("span", spanObj);
			
			String normalizedId = searchNormalizedId(pubmedId, docList, docIndex, "title", start, end);
			entObj.put("obj", normalizedId);
			denotationArr.add(entObj);
		}
		
		for(Entity ent:reader.getAbsEntities()){
			JSONObject entObj = new JSONObject();
			String identifier = "T" + entCounter++;
			entObj.put("id", identifier);
			
			JSONObject spanObj = new JSONObject();
			int start = ent.getStart() + offSetAddition;
			int end = ent.getEnd() + offSetAddition;
			
			idMap.put(start, identifier);
			
			spanObj.put("begin", start);
			spanObj.put("end", end );
			
			entObj.put("span", spanObj);

			String normalizedId = searchNormalizedId(pubmedId, docList, docIndex, "abs", ent.getStart(), ent.getEnd());
			entObj.put("obj", normalizedId);
			denotationArr.add(entObj);
		}
		jsonObject.put("denotations", denotationArr);
		
		// Write Relations Now
		int relCounter=1;
		JSONArray relationArr = new JSONArray();
		
		for(Relation rel:reader.getTitleRelations()){
			Entity protEntity = rel.getProteinEntity();
			Entity nonProtEntity = rel.getNonProteinEntity();
			
			if(!allowPORelations && 
					nonProtEntity.getType()==EntityType.Organism){
				continue;
			}
			
			JSONObject relObj = new JSONObject();
			String identifier = "R" + relCounter++;
			
			relObj.put("id", identifier);
			
			int entStart = protEntity.getStart();
			relObj.put("subj", idMap.get(entStart));
			
			if(nonProtEntity.getType()==EntityType.Location){
				relObj.put("pred", "localizeTo");
			} else {
				relObj.put("pred", "belongsTo");
			}
			
			relObj.put("obj", idMap.get(nonProtEntity.getStart()));
			relationArr.add(relObj);
		}
		
		for(Relation rel:reader.getAbsRelations()){
			Entity protEntity = rel.getProteinEntity();
			Entity nonProtEntity = rel.getNonProteinEntity();
			
			if(!allowPORelations && 
					nonProtEntity.getType()==EntityType.Organism){
				continue;
			}
			
			JSONObject relObj = new JSONObject();
			String identifier = "R" + relCounter++;
			
			relObj.put("id", identifier);
			
			int protEntStart = protEntity.getStart()+offSetAddition;
			relObj.put("subj", idMap.get(protEntStart));
			
			if(nonProtEntity.getType()==EntityType.Location){
				relObj.put("pred", "localizeTo");
			} else {
				relObj.put("pred", "belongsTo");
			}
			
			int nonProtEntStart = nonProtEntity.getStart() + offSetAddition;					
			relObj.put("obj", idMap.get(nonProtEntStart));
			relationArr.add(relObj);
		}
		jsonObject.put("relations", relationArr);
	
		String outputFilePath = outputDirectory + pubmedId + ".json";
		try {
			FileWriter fw = new FileWriter(outputFilePath);
			fw.write(jsonObject.toJSONString());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String searchNormalizedId(String pubmedId,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex,
			String part, int start, int end) {
		Document doc = docList.get(docIndex.get(pubmedId));
		String retString = "";
		
		ArrayList<Entity> entList = null;
		if(part.equals("title")) {
			entList = doc.getTitleEntities();
		} else {
			entList = doc.getAbsEntities();
		}
		
		for(Entity ent:entList){
			if(ent.getStart()==start && ent.getEnd()==end){
				retString = ent.getNormalizedIdentifier();
				if(retString.equals("TODO") || retString.contains(",")){
					if(ent.getType()==EntityType.Protein){
						retString = "Protein";
					} else if(ent.getType()==EntityType.Location){
						retString = "Location";
					} else if(ent.getType()==EntityType.Organism){
						retString = "Organism";
					}
				} else {
					if(ent.getType()==EntityType.Protein){
						retString = "http://identifiers.org/uniprot/" + retString;
					} else if(ent.getType()==EntityType.Location){
						String tokens[] = retString.split("\\s+");
						retString = "http://identifiers.org/go/" + tokens[0]; // first term should be go term
					} else if(ent.getType()==EntityType.Organism){
						retString = "http://identifiers.org/taxonomy/" + retString;
					}
				}
			}
		}
		
		return retString;
	}

	private static void readIntermediateFile(String intermediateFilePath,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex) {
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("workDir/interFile.tsv"));
			br.readLine(); // first line
			
			String line;
			while((line=br.readLine())!=null){
				String tokens[] = line.split("\\t+");
			
				String docId = tokens[0];

				Document currentDoc = null;
				if(docIndex.containsKey(docId)){
					currentDoc = docList.get(docIndex.get(docId));
				} else {
					currentDoc = new Document(docId);
					docList.add(currentDoc);
					docIndex.put(docId, docList.size()-1);
				}
				
				Entity ent = new Entity();
				ent.setText(tokens[1]);
				
				if(tokens[2].equals("Location")){
					ent.setType(EntityType.Location);
				} else if(tokens[2].equals("Organism")){
					ent.setType(EntityType.Organism);					
				} else if(tokens[2].equals("Protein")){
					ent.setType(EntityType.Protein);
				}
				
				ent.setNormalizedIdentifier(tokens[3]);
				ent.setStart(Integer.parseInt(tokens[5]));
				ent.setEnd(Integer.parseInt(tokens[6]));
				
				if(tokens[4].equals("title")) {
					ent.setPart(EntityPart.title);
					currentDoc.addTitleEntity(ent);
				} else if(tokens[4].equals("abs")) {
					ent.setPart(EntityPart.abs);
					currentDoc.addAbstractEntity(ent);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
