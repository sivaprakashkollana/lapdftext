package edu.isi.bmkeg.utils.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public class JarUtilities {

    JarFile jarFile;

    public File extractFileFromJar(String fileName) throws Exception {        
        ClassLoader cl = this.getClass().getClassLoader();
        
        String userDir = System.getProperty( "user.dir" );
        
        File testFile = new File(userDir + "/" + fileName);
        
        if( testFile.exists() ) {
            return testFile;
        }
        
        File testDir = testFile.getParentFile();
        
        if( !testDir.exists() ) {
            testDir.mkdir();
        }
        
        InputStream in = this.getIOStreamFromJar(fileName);
        OutputStream out = new FileOutputStream(userDir + "/" + fileName);
        int c;
        while ((c = in.read()) != -1)
            out.write(c);
                
        in.close();
        out.close();
        
        return testFile;
        
    } 

    private InputStream getIOStreamFromJar(String fileName) throws Exception {        
        InputStream ioStream = null;
        
        ClassLoader cl = this.getClass().getClassLoader();
                        
        String testFileURL = cl.getResource(fileName).getFile();
        String jarFileURL = testFileURL.substring(0, testFileURL.indexOf("!"));
        
        File f = new File(new URI(jarFileURL));
        this.jarFile = new JarFile(f);
        
        Enumeration entries = jarFile.entries();
        ZipEntry entry = null;    
        while (entries.hasMoreElements()) {
            entry = (ZipEntry) entries.nextElement();
            if( fileName.equals(entry.getName())) {
                
                ioStream = jarFile.getInputStream(entry);
                break;
        
            }
        }
        
        return ioStream;
        
    } 

    public Document extractDOMFromJar(String xmlFileName) throws Exception {        
          
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        InputStream is = this.getIOStreamFromJar(xmlFileName);
        
        Document document = null;
        if(is != null) {
            document = builder.parse( is );        
        }
        
        return document;
    
    } 
 }
