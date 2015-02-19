package main.java.utils;

public class Relation {

	private Entity proteinEntity;
	private Entity nonProteinEntity;
	
	Relation(Entity protEntity, Entity nonProtEntity) {
		proteinEntity=protEntity;
		nonProteinEntity=nonProtEntity;
	}

	public Entity getProteinEntity() {
		return proteinEntity;
	}

	public Entity getNonProteinEntity() {
		return nonProteinEntity;
	}
}
