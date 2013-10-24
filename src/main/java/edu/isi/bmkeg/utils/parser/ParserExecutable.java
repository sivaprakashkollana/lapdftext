package edu.isi.bmkeg.utils.parser;

import java.io.File;
import java.util.*;
import javax.swing.event.EventListenerList;

public class ParserExecutable implements ParserEventListener {

	protected EventListenerList listenerList = new EventListenerList();

    protected ParserThread p;

    protected LinkedHashSet queue = new LinkedHashSet();

    protected boolean overwrite = false;

    protected HashMap folderCount = new HashMap();

    protected File iFolder = null;

    protected String iSuffix = null;

    protected File oFolder = null;

    protected String oSuffix = null;

    protected Iterator it;

    public  ParserExecutable(java.io.File iFolder, String iSuffix) {        
        this.iFolder = iFolder;
        this.iSuffix = iSuffix;
    } 

    public  ParserExecutable(java.io.File iFolder, String iSuffix, java.io.File oFolder, String oSuffix) {        
        this.iFolder = iFolder;
        this.iSuffix = iSuffix;
        this.oFolder = oFolder;
        this.oSuffix = oSuffix;
    } 

    protected void printFileCount() {        
        Object[] keys = this.folderCount.keySet().toArray();
        Arrays.sort(keys);
        for( int i = 0; i<keys.length; i++) {
            
            System.out.println(keys[i] + " - " + this.folderCount.get(keys[i]));
            
        }        
    } 

    protected File getNextFile() {        
    
    	while (it.hasNext()) {
            
            File f = (File) it.next();
            File output = this.setUpOutputFile(f);
            
            if( overwrite || output == null )
                return f;
            
            if ( !output.exists() ||
                    output.length() == 0 )
                return f;
            
        }
        
        return null;
        
    } 

    public void parseFile(File f) throws Exception {        
        if( this.p == null ) {
            System.out.print("Set up parser");
            System.exit(-1);
        }
        
        this.p.addParseEventListener(this);
        this.p.parseFile(f);
    } 

    public void parseFilesInQueue() throws Exception {        

        it = this.queue.iterator();
        File f = this.getNextFile();
        if( f != null )
            this.parseFile(f);
        else 
            this.done();
        
    } 

    public void loadAllFiles(boolean recurse) {        
        this.loadAllFilesInFolder(this.iFolder, recurse, this.iSuffix);
    } 

    protected void loadAllFilesInFolder(File folder, boolean recurse, String suffix) {               
        File[] files = folder.listFiles();
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (recurse && f.isDirectory()) {
                this.loadAllFilesInFolder(f, true, suffix);
            } else if (f.getAbsolutePath().toLowerCase().endsWith(suffix)) {
                this.queue.add(f);
                count++;
            }
        }
        this.folderCount.put( folder.getAbsolutePath(), new Integer(count) );
    
    } 

    public void parseCompleted(ParserEvent evt) throws Exception {        

    	Object o = evt.getSource();
        ParserThread p = (ParserThread) o;
        
        try {
            
            File f = p.getFile();
            System.out.println(f.getPath());
            
            File output = setUpOutputFile(f);
            if( output != null ){
                System.out.println(output.getPath());
            }
            
        } catch (Exception e) {
            
            e.printStackTrace();
                  
        }
        
        if (it.hasNext()) {
            File nextF = this.getNextFile();
            this.parseFile(nextF);
        }
        
    } 

    protected String setSuffixRegex(String in) {        
        String out = in.replaceAll("\\.","\\\\.");
        out += "$";
        return out;
    } 

    protected File setUpOutputFile(File f) {        

        if( oFolder == null ) {
            return null;
        }
        
        
        String pth = f.getAbsolutePath().replaceAll(setSuffixRegex(iSuffix),
                oSuffix);
        
        pth = oFolder + pth.substring(iFolder.getAbsolutePath().length(),
                pth.length());
        
        File output = new File(pth);
        File dir = output.getParentFile();
        Vector dirsToCreate = new Vector();
        
        while( !dir.exists() ) {
            dirsToCreate.add(dir);
            dir = dir.getParentFile();
        }
        
        for( int i=dirsToCreate.size()-1; i>=0; i--) {
            File newDir = (File) dirsToCreate.get(i);
            newDir.mkdir();
        }
        
        return output;
        
    } 

    protected void setUpParser() {        
       
        System.out.print("Must overload setUpParser() function");
        System.exit(-1);
        
    } 

    protected void done() {        
        // your code here
    } 

    public void addParseEventListener(ParserEventListener listener) {        
        listenerList.add(ParserEventListener.class, listener);
    } 

    public ParserEventListener[] getParseEventListeners() {        
        Object[] listeners = listenerList.getListenerList();
        ParserEventListener[] vel = new ParserEventListener[listeners.length / 2];
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ParserEventListener.class) {
                vel[i] = (ParserEventListener) listeners[i + 1];
            }
        }
        if (vel.length == 0) {
            return null;
        } else {
            return vel;
        }
    } 

/**
 * Permits listeners to unregister for Parse events
 */
    public void removeParserEventListener(ParserEventListener listener) {        
        listenerList.remove(ParserEventListener.class, listener);
    } 

    public void fireParserEvent(ParserEvent evt) throws Exception {        
        
        Object[] listeners = listenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ParserEventListener.class) {
                ParserEventListener listener = (ParserEventListener) listeners[i + 1];
                listener.parseCompleted(evt);
            }
        }
    } 
 }
