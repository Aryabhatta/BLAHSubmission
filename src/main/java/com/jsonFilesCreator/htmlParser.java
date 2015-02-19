package main.java.com.jsonFilesCreator;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class htmlParser {
	private String absFilePath;
	private File input;
	private Document doc;
	htmlParser(String path){
		absFilePath = path;
		input = new File(absFilePath);
		try {
			doc = Jsoup.parse(input,"UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getTitle(){
		Element title = doc.getElementById("s1h1");
		return title.text();
	}
	
	public String getAbstract() {
		Element abs = doc.getElementById("s2p1");
		return abs.text();
	}
}
