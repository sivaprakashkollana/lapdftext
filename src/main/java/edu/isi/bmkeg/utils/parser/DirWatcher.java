
package edu.isi.bmkeg.utils.parser;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

public abstract class DirWatcher extends TimerTask {

    private File input;

    private File[] filesArray;

    private Map<File, Long> dir = new HashMap<File, Long>();

    private Set<File> checked = new HashSet<File>();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private DirFilterWatcher dfw;

    public  DirWatcher(java.io.File folder) {        
        this(folder, "");
    } 

    public  DirWatcher(java.io.File folder, String filter) {   
    	
        this.setInput(folder);
        dfw = new DirFilterWatcher(filter);
        filesArray = folder.listFiles(dfw);
        
        // transfer to the hashmap be used a reference and keep the
        // lastModfied value
        for(int i = 0; i < filesArray.length; i++) {
            dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
        }
    
    } 

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
	public File getInput() {
		return input;
	}

	public void setInput(File input) {
		this.input = input;
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public final void run() {        
    
    	HashSet<File> checkedFiles = new HashSet<File>();
        filesArray = getInput().listFiles(dfw);
        
        // scan the files and check for modification/addition
        for(int i = 0; i < filesArray.length; i++) {
            Long cTime = (Long)dir.get(filesArray[i]);
            long sTime = System.currentTimeMillis();
            checkedFiles.add(filesArray[i]);
            
            if (cTime == null) {
            	
                // new file
                dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
                onChange(filesArray[i], "added");
                
            } else if (cTime.longValue() != filesArray[i].lastModified() && 
                    sTime > cTime.longValue() + 30000 ){
                
            	// modified file
                dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
                onChange(filesArray[i], "modified");
                checked.remove(filesArray[i]);
            
            } else if (cTime.longValue() != filesArray[i].lastModified() ){
            
            	// modified file
                dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
                onChange(filesArray[i], "transiently-modified");

            } else if (cTime.longValue() == filesArray[i].lastModified() &&
                    !this.checked.contains(filesArray[i])){
            
            	// modified file
                dir.put(filesArray[i], new Long(filesArray[i].lastModified()));
                checked.add(filesArray[i]);
                onChange(filesArray[i], "completed");

            }
        
        }
        
        // now check for deleted files
        Set<File> ref = new HashSet<File>(dir.keySet());
        ref.removeAll(checkedFiles);
        Iterator<File> it = ref.iterator();
        while (it.hasNext()) {
            File deletedFile = (File)it.next();
            dir.remove(deletedFile);
            onChange(deletedFile, "deleted");
            checked.remove(deletedFile);
        }
        
    } 

    protected abstract void onChange(File file, String action);

}

class DirFilterWatcher implements FileFilter {

    private String filter;

    public  DirFilterWatcher() {        
        this.filter = "";
    } 

    public  DirFilterWatcher(String filter) {        
        this.filter = filter;
    } 

    public boolean accept(File file) {        
        if ("".equals(filter)) {
            return true;
        }
        return (file.getName().endsWith(filter));
    } 
 
}
