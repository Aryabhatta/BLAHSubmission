package main.java.jsonFilesCreator;

import java.util.ArrayList;

import main.java.utils.Entity;

public class Document {
	
	Document(String id){
		titleEntities = new ArrayList<Entity>();
		absEntities = new ArrayList<Entity>();
		
		pubmedId = id;
	}
	String pubmedId;
	ArrayList<Entity> titleEntities;
	ArrayList<Entity> absEntities;
	
	public String getPubmedId() {
		return pubmedId;
	}
	public void setPubmedId(String pubmedId) {
		this.pubmedId = pubmedId;
	}
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
	public void addTitleEntity(Entity ent){
		titleEntities.add(ent);
	}
	public void addAbstractEntity(Entity ent){
		absEntities.add(ent);
	}
}
