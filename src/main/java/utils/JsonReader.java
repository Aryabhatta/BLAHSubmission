package main.java.utils;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import main.java.utils.Entity.EntityType;

public class JsonReader {

	public JsonReader(String fileName){
		
		currentFileName=fileName;
		
		titleEntities = new ArrayList<Entity>();
		absEntities = new ArrayList<Entity>();
		
		titleEntityMap = new HashMap<Integer, Entity>();
		absEntityMap = new HashMap<Integer, Entity>();
		
		titleRelations = new ArrayList<Relation>();
		absRelations = new ArrayList<Relation>();
		parseFile();
	}

	private void parseFile() {
		FileReader reader;

		try {
			reader = new FileReader(currentFileName);

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
			JSONArray entArr = (JSONArray) jsonObject.get("entities");

			// reading entities into ArrayList
			for(int i=0; i<entArr.size(); i++) {
				JSONObject entObj = (JSONObject) entArr.get(i);
				Entity newEnt = new Entity();

				String classId = (String)entObj.get("classId");
				// not considering connection
				if(classId.equals("e_4")) {
					continue;
				}

				String textPart = (String) entObj.get("part"); 

				JSONArray offArr = (JSONArray) entObj.get("offsets");
				JSONObject offObj = (JSONObject) offArr.get(0);

				Long startOffSet = (long)offObj.get("start");
				newEnt.setStart(startOffSet.intValue());
				newEnt.setText((String) offObj.get("text"));

				// since beginIndex is inclusive and endIndex is exclusive
				newEnt.setEnd(newEnt.getStart()+newEnt.getText().length());

				if(textPart.equals("s1h1")) {
					newEnt.setPart(Entity.EntityPart.title);

					if(classId.equals("e_1")) {
						newEnt.setType(Entity.EntityType.Protein);
					} else if(classId.equals("e_2")) {
						newEnt.setType(Entity.EntityType.Location);
					} else if(classId.equals("e_3")) {
						newEnt.setType(Entity.EntityType.Organism);
					}
					titleEntityMap.put(newEnt.getStart(), newEnt);
					titleEntities.add(newEnt);
				} else {
					newEnt.setPart(Entity.EntityPart.abs);
					if(classId.equals("e_1")) {
						newEnt.setType(Entity.EntityType.Protein);
					} else if(classId.equals("e_2")) {
						newEnt.setType(Entity.EntityType.Location);
					} else if(classId.equals("e_3")) {
						newEnt.setType(Entity.EntityType.Organism);
					}
					absEntityMap.put(newEnt.getStart(), newEnt);
					absEntities.add(newEnt);
				}
			}
			
			// sort titleEntities and absEntities as per start
			// Sort the ArrayLists
			Collections.sort(titleEntities);
			Collections.sort(absEntities);
			
			// reading relations now
			JSONArray relArr = (JSONArray) jsonObject.get("relations");
			for(int i=0; i < relArr.size();i++){
				JSONObject relObj = (JSONObject) relArr.get(i);
				JSONArray entArrRel = (JSONArray) relObj.get("entities"); 
				
				String ent1 = (String) entArrRel.get(0);
				String ent2 = (String) entArrRel.get(1);
				
				String tokens1[] = ent1.split("\\|");
				
				String part1 = tokens1[0];
				String token[] = tokens1[1].split(",");
				
				int start1 =  Integer.parseInt(token[0]);					
				String tokens2[] = ent2.split("\\|");
				
				token = tokens2[1].split(",");
				int start2 =  Integer.parseInt(token[0]);
				
				if(part1.equals("s1h1")) { //need not check part 2 since cross part relations are not allowed at the moment
					Entity protEntity = null;
					Entity nonProtEntity = null;
					
					if(!titleEntityMap.containsKey(start1) || !titleEntityMap.containsKey(start2)) {
						System.out.println("Error !!! Entity not found");
					}
					
					Entity firstEnt = titleEntityMap.get(start1);
					Entity secondEnt = titleEntityMap.get(start2);
					
					if(firstEnt.getType()==EntityType.Protein) {
						protEntity = firstEnt;
						nonProtEntity = secondEnt;
					} else {
						protEntity = secondEnt;
						nonProtEntity = firstEnt;
					}
					titleRelations.add(new Relation(protEntity,nonProtEntity));
				} else {
					Entity protEntity = null;
					Entity nonProtEntity = null;
					
					if(!absEntityMap.containsKey(start1) || !absEntityMap.containsKey(start2)) {
						System.out.println("Error !!! Entity not found");
					}
					
					Entity firstEnt = absEntityMap.get(start1);
					Entity secondEnt = absEntityMap.get(start2);
					
					if(firstEnt.getType()==EntityType.Protein) {
						protEntity = firstEnt;
						nonProtEntity = secondEnt;
					} else {
						protEntity = secondEnt;
						nonProtEntity = firstEnt;
					}
					absRelations.add(new Relation(protEntity,nonProtEntity));
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String currentFileName;
	
	private ArrayList<Entity> titleEntities;
	private ArrayList<Entity> absEntities;
	
	private HashMap<Integer,Entity> titleEntityMap;
	private HashMap<Integer,Entity> absEntityMap;
	
	private ArrayList<Relation> titleRelations;
	private ArrayList<Relation> absRelations;
	
	public ArrayList<Entity> getTitleEntities() {
		return titleEntities;
	}
	public void setTitleEntities(ArrayList<Entity> titleEntities) {
		this.titleEntities = titleEntities;
	}
	public ArrayList<Entity> getAbsEntities() {
		return absEntities;
	}
	public void setAbsEntities(ArrayList<Entity> absEntities) {
		this.absEntities = absEntities;
	}

	public ArrayList<Relation> getTitleRelations() {
		return titleRelations;
	}

	public void setTitleRelations(ArrayList<Relation> titleRelations) {
		this.titleRelations = titleRelations;
	}

	public ArrayList<Relation> getAbsRelations() {
		return absRelations;
	}

	public void setAbsRelations(ArrayList<Relation> absRelations) {
		this.absRelations = absRelations;
	}
}
