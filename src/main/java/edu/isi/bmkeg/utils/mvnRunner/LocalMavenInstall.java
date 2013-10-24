package edu.isi.bmkeg.utils.mvnRunner;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import edu.isi.bmkeg.utils.Converters;

public class LocalMavenInstall {
	
	public static String MVN_FILE = "apache-maven-3.0.4-bin.zip";
	public static String MVN_VERSION = "apache-maven-3.0.4";
		
	public static String runMavenCommand(String options) throws Exception {
		
		String out = ""; 
		
		String cmd = LocalMavenInstall.getMavenCmd();
		
		try {

			out = LocalMavenInstall.runMavenCommand(cmd, options);
		
		} catch(Exception e) {

			File mvnExec = LocalMavenInstall.getLocalMavenExec();
			out = LocalMavenInstall.runMavenCommand(mvnExec.getPath(), options);
			
		}
						
		return out;
		
	}
	
	private static String runMavenCommand(String cmd, String options) throws Exception {
		
		return LocalMavenInstall.runCommand(cmd + " " + options);
	
	}
	
	private static String runCommand(String cmd) throws Exception {
		
		String out = "";
		
		// Maven CLI interface does not work. Use the local exec function
		Runtime r = Runtime.getRuntime();
		Process p = r.exec(cmd);
		
		// If this fails, use a local, prepacked version of 
		// Maven that we unpack to a temp directory 
					
		InputStream in = p.getInputStream();
		BufferedInputStream buf = new BufferedInputStream(in);
		InputStreamReader inread = new InputStreamReader(buf);
		BufferedReader bufferedreader = new BufferedReader(inread);
        String line;
        while ((line = bufferedreader.readLine()) != null) {
        	out += line + "\n";
        }
        // Check for maven failure
        try {
        	if (p.waitFor() != 0) {
        		out += "exit value = " + p.exitValue() + "\n";
        	}
        } catch (InterruptedException e) {
        	out += "ERROR:\n" + e.getStackTrace().toString() + "\n";
        } finally {
        	// Close the InputStream
        	bufferedreader.close();
        	inread.close();
        	buf.close();
        	in.close();
		}
        
        return out;
		
	}

	/**
	 * Installs Maven in the current working location (i.e. the application directory)
	 * @return
	 * @throws Exception 
	 */
	public static File installMavenLocally() throws Exception {
		
		String dirPath = System.getProperty("user.dir");
		File dir = new File(dirPath);
		
		String mvnPath = ClassLoader.getSystemResource("edu/isi/bmkeg/utils/mvnRunner/" + MVN_FILE).getFile();
		File mvnZip = new File(mvnPath);
		
		Converters.unzipIt(mvnZip, dir);
		
		LocalMavenInstall.setMavenPermissions();
		
		File mvnExec = LocalMavenInstall.getLocalMavenExec();
		
		return mvnExec;

	}
	
	private static File getLocalMavenExec() throws Exception {

		String dirPath = System.getProperty("user.dir");
		File dir = new File(dirPath);
		File mvnExec = new File( dir + "/" + MVN_VERSION + "/bin/mvn");
		
		String osName = System.getProperty("os.name");
		if( osName.toLowerCase().indexOf("win") >= 0 ) {
			mvnExec = new File( dir + "/" + MVN_VERSION + "/bin/mvn.bat");
		}
			
		if( !mvnExec.exists() ) {
			throw new Exception("Local installation of Maven failed: " + mvnExec.getPath() + " does not exist");
		}
		
		return mvnExec;
		
	}

	public static void removeLocalMaven() throws Exception {

		String dirPath = System.getProperty("user.dir");
		File dir = new File(dirPath);
		File mvnDir = new File( dir + "/" + MVN_VERSION );
				
		mvnDir.delete();
		
	}
	
	private static void setMavenPermissions() throws Exception {

		File mvnExec = getLocalMavenExec();
		
		//
		// Make sure that permissions are set so that 
		// you are allowed to execute this command.
		//
		// You will need to be an administrator to run this.
		// 
		String osName = System.getProperty("os.name");
		if( osName.toLowerCase().indexOf("win") >= 0 ) {
			//LocalMavenInstall.runCommand(mvnExec.getPath());			
		} else {
			LocalMavenInstall.runCommand("chmod +x " + mvnExec.getPath());
		}
		
	}
	
	private static String getMavenCmd() throws Exception {

		String osName = System.getProperty("os.name");
		if( osName.toLowerCase().indexOf("win") >= 0 ) {
			return "mvn.bat";
		}
			
		return "mvn";
		
	}
	
	public static void main(String[] args) throws Exception {

		String flag = "-v";
		
		if( args.length > 0)
			flag = args[0];
		
		String out = "";
		
		if( flag.equals("-v") ) {
		
			System.out.println( LocalMavenInstall.runMavenCommand("-version") );
		
		} else if( flag.equals("-i") ) {
			
			System.out.print("Installing Maven");
			LocalMavenInstall.installMavenLocally();
		
		} else if( flag.equals("-r") ) {

			System.out.print("Removing Maven");
			LocalMavenInstall.removeLocalMaven();
		
		}			
		
	}
	
}
