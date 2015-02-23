package main.java.com.jsonFilesCreator;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import main.java.com.utils.Entity;
import main.java.com.utils.Filter;
import main.java.com.utils.JsonReader;
import main.java.com.utils.Relation;
import main.java.com.utils.Entity.EntityPart;
import main.java.com.utils.Entity.EntityType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Main {

  private static String getUnormalizedId(EntityType entityType) {
    return "";
  }

	public static void main(String[] args) {
/**/
		if(args.length < 5) {
			System.out.println("Usage: ");
			System.out.println("java -jar jsonFilesCreator-1.0.one-jar.jar [pathToHTMLfiles] [pathToJSONfiles] [intermediateFilePath] [outputDirectory] [outputType] [concatString]\n");
			System.out.println("1. [pathToHTMLfiles]-Mandatory: Path to folder containing html files");
			System.out.println("2. [pathToJSONfiles]-Mandatory: Path to folder containing json files");
			System.out.println("3. [intermediateFilePath]-Mandatory: Path to interFile.tsv");
			System.out.println("4. [outputDirectory]-Mandatory: Path to folder where final json files would be written");
			System.out.println("5. [outputType]-Mandatory: Type of files to be created");
			System.out.println("\t if outputType=1, separate json files for title and abstract");
			System.out.println("\t if outputType=2, combined file by concatenating title and abstract");
			System.out.println("\t if outputType=3, all possible files (separate and combined) are produced");
			System.out.println("6. [concatString]-Optional: string used for concatenating title and abstract. By default space is used for concatenation \n\n");
			return;
		}
		String titleAbsConcatStr = "\n";

		String htmlDirectory = args[0];
		if(htmlDirectory.charAt(htmlDirectory.length()-1)!='/') {
			htmlDirectory += "/";
		}

		String jsonDirectory = args[1];
		if(jsonDirectory.charAt(jsonDirectory.length()-1)!='/') {
			jsonDirectory += "/";
		}

		String intermediateFilePath = args[2];

		String outputDirectory = args[3];
		if(outputDirectory.charAt(outputDirectory.length()-1)!='/') {
			outputDirectory += "/";
		}

		int outputType = Integer.parseInt(args[4]);

		if(args.length==6) {
			titleAbsConcatStr = args[5];
		}
/**/

/*
		String titleAbsConcatStr = " ";
		String htmlDirectory = "/home/shrikant/NLPRE/shrikant-master-thesis/LocTextCorpus/html/";
		String jsonDirectory = "/home/shrikant/NLPRE/shrikant-master-thesis/LocTextCorpus/annotations/";
		String intermediateFilePath = "workDir/interFile.tsv";
		String outputDirectory = "workDir/jsonFiles/";
		int outputType=3;
*/

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

			String pubmedId = "";
			String jsonFilePath = "";

			if(newFile.getName().contains(".plain.html")) {
				pubmedId = newFile.getName().substring(newFile.getName().lastIndexOf('-')+1, newFile.getName().indexOf(".plain.html"));
				jsonFilePath = jsonDirectory + newFile.getName().replace(".plain.html", ".ann.json");

				//skip PMC files
				if(pubmedId.contains("PMC")) {
					continue;
				}
			} else {
				pubmedId = newFile.getName().replace(".html", "");
				jsonFilePath = jsonDirectory + pubmedId + ".json";
			}

			JsonReader reader = new JsonReader(jsonFilePath);

			if(outputType==1) {
				writeTitleJsonFile(pubmedId, docList, docIndex,parser,reader,outputDirectory, allowPORelations);
				writeAbstractJsonFile(pubmedId, docList, docIndex,parser,reader,outputDirectory, titleAbsConcatStr, allowPORelations);
			} else if(outputType==2) {
				writeJsonFile(pubmedId, docList, docIndex,parser,reader,outputDirectory, titleAbsConcatStr, allowPORelations);
			} else if(outputType==3) {
				writeTitleJsonFile(pubmedId, docList, docIndex,parser,reader,outputDirectory, allowPORelations);
				writeAbstractJsonFile(pubmedId, docList, docIndex,parser,reader,outputDirectory, titleAbsConcatStr, allowPORelations);
				writeJsonFile(pubmedId, docList, docIndex,parser,reader,outputDirectory, titleAbsConcatStr, allowPORelations);
			} else {
				System.err.println("Invalid output type. See Usage. Valid options are 1,2 or 3");
			}
		}
	}

	private static void writeTitleJsonFile(String pubmedId,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex,
			htmlParser parser, JsonReader reader, String outputDirectory,
			boolean allowPORelations) {

		JSONObject jsonObject = new JSONObject();

		String titleText = parser.getTitle();

		jsonObject.put("text", titleText);
		jsonObject.put("sourcedb", "PubMed");
		jsonObject.put("sourceid",pubmedId);
		jsonObject.put("div_id", 0);

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

		jsonObject.put("relations", relationArr);

		String outputFilePath = outputDirectory + pubmedId + "-title.json";
		try {
			FileWriter fw = new FileWriter(outputFilePath);
			fw.write(jsonObject.toJSONString());
			fw.close();
		} catch (IOException e) {
			System.err.println("Error writing file: " + outputFilePath);
		}
	}

	private static void writeAbstractJsonFile(String pubmedId,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex,
			htmlParser parser, JsonReader reader, String outputDirectory,
			String titleAbsConcatStr, boolean allowPORelations) {

		JSONObject jsonObject = new JSONObject();

		String abstractText = parser.getAbstract();

		jsonObject.put("text", abstractText);
		jsonObject.put("sourcedb", "PubMed");
		jsonObject.put("sourceid",pubmedId);
		jsonObject.put("div_id", 0);

		int entCounter = 1;

		HashMap<Integer,String> idMap = new HashMap<Integer,String>();

		JSONArray denotationArr = new JSONArray();
		for(Entity ent:reader.getAbsEntities()){
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

			String normalizedId = searchNormalizedId(pubmedId, docList, docIndex, "abs", ent.getStart(), ent.getEnd());
			entObj.put("obj", normalizedId);
			denotationArr.add(entObj);
		}
		jsonObject.put("denotations", denotationArr);

		// Write Relations Now
		int relCounter=1;
		JSONArray relationArr = new JSONArray();
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

			int protEntStart = protEntity.getStart();
			relObj.put("subj", idMap.get(protEntStart));

			if(nonProtEntity.getType()==EntityType.Location){
				relObj.put("pred", "localizeTo");
			} else {
				relObj.put("pred", "belongsTo");
			}

			int nonProtEntStart = nonProtEntity.getStart();
			relObj.put("obj", idMap.get(nonProtEntStart));
			relationArr.add(relObj);
		}
		jsonObject.put("relations", relationArr);

		String outputFilePath = outputDirectory + pubmedId + "-abstract.json";
		try {
			FileWriter fw = new FileWriter(outputFilePath);
			fw.write(jsonObject.toJSONString());
			fw.close();
		} catch (IOException e) {
			System.err.println("Error writing file: " + outputFilePath);
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
		jsonObject.put("div_id", 0);

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
			System.err.println("Error writing file: " + outputFilePath);
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
				if(retString.equals("TODO") ||
						retString.toLowerCase().contains("protein") ||
						retString.toLowerCase().contains("organism") ||
						retString.toLowerCase().contains("location")){

                  retString = getUnormalizedId(ent.getType());
				} else {
					if(ent.getType()==EntityType.Protein){
						if(retString.contains(",")) {
							String tokens[] = retString.split(",");

							String concatenatedUrls = "";
							for(int i=0; i<tokens.length; i++){
								if(!concatenatedUrls.isEmpty()) {
									concatenatedUrls += ",";
								}
								concatenatedUrls += EntityType.Protein.namespacePrefix + tokens[i];
							}
						} else {
							retString = EntityType.Protein.namespacePrefix + retString;
						}
					} else if(ent.getType()==EntityType.Location){
						String tokens[] = retString.split("\\s+");
						if(tokens[0].substring(0,3).equals("GO:")) {
							retString = EntityType.Location.namespacePrefix + tokens[0]; // first term should be go term
						} else {
                          retString = getUnormalizedId(EntityType.Location);
						}
					} else if(ent.getType()==EntityType.Organism){
						retString = EntityType.Organism.namespacePrefix + retString;
					}
				}
			}
		}

		return retString;
	}

	private static void readIntermediateFile(String intermediateFilePath,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex) {

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(intermediateFilePath));
		} catch (FileNotFoundException e) {
			System.err.println("File: " + intermediateFilePath + " not found !!");
			return;
		}

		try {
			br.readLine(); // first line
		} catch (IOException e) {
			System.err.println("Error reading file: " + intermediateFilePath);
			return;
		}

		String line;
		try {
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

				ent.setNormalizedIdentifier(tokens[3].trim());
				ent.setStart(Integer.parseInt(tokens[5].trim()));
				ent.setEnd(Integer.parseInt(tokens[6].trim()));

				if(tokens[4].equals("title")) {
					ent.setPart(EntityPart.title);
					currentDoc.addTitleEntity(ent);
				} else if(tokens[4].equals("abs")) {
					ent.setPart(EntityPart.abs);
					currentDoc.addAbstractEntity(ent);
				}
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			System.err.println("Error reading file: " + intermediateFilePath);
			return;
		}
	}
}
