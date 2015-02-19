package main.java.utils;

public class Entity implements Comparable<Entity>{
	public enum EntityType{
		Protein,
		Location,
		Organism
	};
	
	public enum EntityPart{
		title,
		abs
	};
	private int start; // starting position
	private int end; // ending pos + 1
	private String text;
	private EntityType type;
	private EntityPart part; 	
	private String normalizedIdentifier;
	
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public EntityType getType() {
		return type;
	}
	public void setType(EntityType type) {
		this.type = type;
	}
	public EntityPart getPart() {
		return part;
	}
	public void setPart(EntityPart part) {
		this.part = part;
	}
	@Override
	public int compareTo(Entity arg0) {
		// TODO Auto-generated method stub
		return (int)(start-arg0.getStart());	
	}
	public String getNormalizedIdentifier() {
		return normalizedIdentifier;
	}
	public void setNormalizedIdentifier(String normalizedIdentifier) {
		this.normalizedIdentifier = normalizedIdentifier;
	}
}
