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

  private static String DEFAULT_TITLE_ABSTRACT_CONCATENATION_STR = "\n";

  private static String getUnormalizedId(EntityType entityType) {
    return entityType.namespacePrefix;
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
			System.out.println("6. [concatString]-Optional: string used for concatenating title and abstract. By default newline is used for concatenation \n\n");
			return;
		}
		String titleAbsConcatStr = DEFAULT_TITLE_ABSTRACT_CONCATENATION_STR;

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

		// TODO: Change this flag to true if Protein-Organism relations are to be considered as well
		boolean allowPORelations = false;

		// read normalized file
		ArrayList<Document> docList = new ArrayList<Document>();
		HashMap<String,Integer> docIndex = new HashMap<String, Integer>();
		readIntermediateFile(intermediateFilePath, docList,docIndex);

		// finding and iterating over HTML files in html directory
		Filter files = new Filter();
		File[] fileList = files.finder(htmlDirectory, ".html");

		for(File newFile:fileList) {

			// Read html file
			htmlParser parser = new htmlParser(newFile.getAbsolutePath());

			String pubmedId = "";
			String jsonFilePath = "";

			// construct the name of json file depending on the name of html file
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

			// Read json file
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

	/**
	 * Method to write json files in pubAnnotation format only for the title entities and relations
	 *
	 * @param pubmedId
	 * @param docList
	 * @param docIndex
	 * @param parser
	 * @param reader
	 * @param outputDirectory
	 * @param allowPORelations
	 */
	private static void writeTitleJsonFile(String pubmedId,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex,
			htmlParser parser, JsonReader reader, String outputDirectory,
			boolean allowPORelations) {

		JSONObject jsonObject = new JSONObject();

		String titleText = parser.getTitle();

		jsonObject.put("text", titleText);
		jsonObject.put("sourcedb", "PubMed");
		jsonObject.put("sourceid",pubmedId);
		jsonObject.put("divid", 0);

		int entCounter = 1;

		// map for storing entity identifier with their start position, to be used for relation objects
		HashMap<Integer,String> idMap = new HashMap<Integer,String>();

		JSONArray denotationArr = new JSONArray();

		// iterate over entities found in tagtog json files
		for(Entity ent:reader.getTitleEntities()){
			int start = ent.getStart();
			int end = ent.getEnd();

			entCounter = addEntityObject(start, end, ent, pubmedId, idMap,
					docList, docIndex, entCounter, denotationArr);
		}

		jsonObject.put("denotations", denotationArr);

		// Writing Relations Now
		int relCounter=1;
		JSONArray relationArr = new JSONArray();

		// iterate over relations found in tagtog json files
		for(Relation rel:reader.getTitleRelations()){
			Entity protEntity = rel.getProteinEntity();
			Entity nonProtEntity = rel.getNonProteinEntity();

			if(!allowPORelations &&
					nonProtEntity.getType()==EntityType.Organism){
				continue;
			}
			int protEntStart = protEntity.getStart();
			int nonProtEntStart = nonProtEntity.getStart();

			relCounter = addRelationObject(protEntStart, nonProtEntStart, nonProtEntity,
					relCounter, idMap, relationArr);
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

	/**
	 * Method to write json files in pubAnnotation format only for abstract entities and relations
	 *
	 * @param pubmedId
	 * @param docList
	 * @param docIndex
	 * @param parser
	 * @param reader
	 * @param outputDirectory
	 * @param titleAbsConcatStr
	 * @param allowPORelations
	 */
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

		// map for storing entity identifier with their start position, to be used for relation objects
		HashMap<Integer,String> idMap = new HashMap<Integer,String>();

		JSONArray denotationArr = new JSONArray();

		// iterate over entities found in tagtog json files
		for(Entity ent:reader.getAbsEntities()){
			int start = ent.getStart();
			int end = ent.getEnd();

			entCounter = addEntityObject(start, end, ent, pubmedId, idMap,
					docList, docIndex, entCounter, denotationArr);
		}
		jsonObject.put("denotations", denotationArr);

		// Write Relations Now
		int relCounter=1;
		JSONArray relationArr = new JSONArray();

		// iterate over relations found in tagtog json files
		for(Relation rel:reader.getAbsRelations()){
			Entity protEntity = rel.getProteinEntity();
			Entity nonProtEntity = rel.getNonProteinEntity();

			if(!allowPORelations &&
					nonProtEntity.getType()==EntityType.Organism){
				continue;
			}
			int protEntStart = protEntity.getStart();
			int nonProtEntStart = nonProtEntity.getStart();

			relCounter = addRelationObject(protEntStart, nonProtEntStart, nonProtEntity, relCounter, idMap, relationArr);
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

	/**
	 * Method to write json files in PubAnnotation format - title and
	 * abstract is concatenated with a concatString
	 *
	 * @param pubmedId
	 * @param docList
	 * @param docIndex
	 * @param parser
	 * @param reader
	 * @param outputDirectory
	 * @param titleAbsConcatStr
	 * @param allowPORelations
	 */
	private static void writeJsonFile(String pubmedId,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex,
			htmlParser parser, JsonReader reader, String outputDirectory,
			String titleAbsConcatStr, boolean allowPORelations) {

		JSONObject jsonObject = new JSONObject();

		String concatenatedText = parser.getTitle() + titleAbsConcatStr + parser.getAbstract();

		// calculate offset to be added for abstract entities
		int offSetAddition = parser.getTitle().length()+titleAbsConcatStr.length();

		jsonObject.put("text", concatenatedText);
		jsonObject.put("sourcedb", "PubMed");
		jsonObject.put("sourceid",pubmedId);
		jsonObject.put("div_id", 0);

		int entCounter = 1;

		HashMap<Integer,String> idMap = new HashMap<Integer,String>();

		JSONArray denotationArr = new JSONArray();

		// iterate over title entities found in tagtog json files
		for(Entity ent:reader.getTitleEntities()){
			int start = ent.getStart();
			int end = ent.getEnd();

			entCounter = addEntityObject(start, end, ent, pubmedId, idMap,
					docList, docIndex, entCounter, denotationArr);
		}

		// iterate over abstract entities found in tagtog json files
		for(Entity ent:reader.getAbsEntities()){
			int start = ent.getStart() + offSetAddition;
			int end = ent.getEnd() + offSetAddition;

			entCounter = addEntityObject(start, end, ent, pubmedId, idMap,
					docList, docIndex, entCounter, denotationArr);
		}
		jsonObject.put("denotations", denotationArr);

		// Write Relations Now
		int relCounter=1;
		JSONArray relationArr = new JSONArray();

		// iterate over title relations found in tagtog json files
		for(Relation rel:reader.getTitleRelations()){
			Entity protEntity = rel.getProteinEntity();
			Entity nonProtEntity = rel.getNonProteinEntity();

			if(!allowPORelations &&
					nonProtEntity.getType()==EntityType.Organism){
				continue;
			}
			int protEntStart = protEntity.getStart();
			int nonProtEntStart = nonProtEntity.getStart();


			relCounter = addRelationObject(protEntStart, nonProtEntStart, nonProtEntity,
					relCounter, idMap, relationArr);
		}

		// iterate over abstract relations found in tagtog json files
		for(Relation rel:reader.getAbsRelations()){
			Entity protEntity = rel.getProteinEntity();
			Entity nonProtEntity = rel.getNonProteinEntity();

			if(!allowPORelations &&
					nonProtEntity.getType()==EntityType.Organism){
				continue;
			}

			int protEntStart = protEntity.getStart()+offSetAddition;
			int nonProtEntStart = nonProtEntity.getStart() + offSetAddition;

			relCounter = addRelationObject(protEntStart, nonProtEntStart, nonProtEntity, relCounter, idMap, relationArr);
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

	/**
	 * Creates a relation json object and puts it in relation array
	 *
	 * @param protEntStart
	 * @param nonProtEntStart
	 * @param nonProtEntity
	 * @param relCounter
	 * @param idMap
	 * @param relationArr
	 * @return
	 */
	private static int addRelationObject(int protEntStart, int nonProtEntStart,
			Entity nonProtEntity, int relCounter,
			HashMap<Integer, String> idMap, JSONArray relationArr) {

		JSONObject relObj = new JSONObject();
		String identifier = "R" + relCounter++;

		relObj.put("id", identifier);

		// retrieving entity identifier from the map
		relObj.put("subj", idMap.get(protEntStart));

		if(nonProtEntity.getType()==EntityType.Location){
			relObj.put("pred", "localizeTo");
		} else {
			relObj.put("pred", "belongsTo");
		}

		// retrieving entity identifier from the map
		relObj.put("obj", idMap.get(nonProtEntStart));
		relationArr.add(relObj);
		return relCounter;
	}

	/**
	 * Creates a entity object and puts it in denotation array
	 *
	 * @param start
	 * @param end
	 * @param ent
	 * @param pubmedId
	 * @param idMap
	 * @param docList
	 * @param docIndex
	 * @param entCounter
	 * @param denotationArr
	 * @return
	 */
	private static int addEntityObject(int start, int end, Entity ent,
			String pubmedId, HashMap<Integer, String> idMap,
			ArrayList<Document> docList, HashMap<String, Integer> docIndex,
			int entCounter, JSONArray denotationArr) {

		Document doc = docList.get(docIndex.get(pubmedId));

		// searches normalized id for this tagtog json entity
		// in the list of normalized ids in intermediate file
		String normalizedId = searchNormalizedId(doc, ent);

		// Repeating objects for multiple normalizable entities
		String normalizedIdTokens[] = normalizedId.split(","); // Do not split it if you do not want multiple objects for multiple normalizable entities

		for(int i=0; i<normalizedIdTokens.length; i++){
			JSONObject entObj = new JSONObject();
			String identifier = "T" + entCounter++;
			entObj.put("id", identifier);

			JSONObject spanObj = new JSONObject();

			// add only first identifier in case of multiple objects
			if(!idMap.containsKey(start)) {
				idMap.put(start, identifier);
			}

			spanObj.put("begin", start);
			spanObj.put("end", end );

			entObj.put("span", spanObj);

			String id = normalizedIdTokens[i];
			entObj.put("obj", id);
			denotationArr.add(entObj);
		}

		return entCounter;
	}

	/**
	 * Search normalized identifier for the jsonEntity ent in the list of normalized ids in intermediate file
	 *
	 * @param doc
	 * @param ent
	 * @return normalized identifier url or set of url's in case of multiple normalized id
	 */
	private static String searchNormalizedId(Document doc, Entity jsonEnt) {
		String retString = "";

		ArrayList<Entity> entList = null;
		if(jsonEnt.getPart()==EntityPart.title) {
			entList = doc.getTitleEntities();
		} else {
			entList = doc.getAbsEntities();
		}

		// iterating over entity list found in intermediate file
		for(Entity ent:entList){
			if(ent.getStart()==jsonEnt.getStart() && ent.getEnd()==jsonEnt.getEnd()){
				retString = ent.getNormalizedIdentifier();

				// if still not normalized
				if(retString.equals("TODO") ||
						retString.toLowerCase().contains("protein") ||
						retString.toLowerCase().contains("organism") ||
						retString.toLowerCase().contains("location")){

                  retString = EntityType.Protein.namespacePrefix + getUnormalizedId(ent.getType());
				} else {
					if(ent.getType()==EntityType.Protein){

						// if multiple normalized ids are present
						if(retString.contains(",")) {
							String tokens[] = retString.split(",");

							String concatenatedUrls = "";
							for(int i=0; i<tokens.length; i++){
								if(!concatenatedUrls.isEmpty()) {
									concatenatedUrls += ",";
								}
								concatenatedUrls += EntityType.Protein.namespacePrefix + tokens[i];
							}

							// return concatenated urls
							retString = concatenatedUrls;
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

        retString = retString.replaceAll("\\s+", ""); //clean spaces
		return retString;
	}

	/**
	 *  Reads intermediate file and stores all entity information in docList
	 * @param intermediateFilePath
	 * @param docList
	 * @param docIndex
	 */
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
