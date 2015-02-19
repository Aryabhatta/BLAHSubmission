package main.java.utils;

import java.io.File;
import java.io.FilenameFilter;

public class Filter {
    public File[] finder( String dirName, final String Extension){
    	File dir = new File(dirName);

    	return dir.listFiles(new FilenameFilter() { 
    	         public boolean accept(File dir, String filename)
    	              { return filename.endsWith(Extension); }
    	} );
    }
}
