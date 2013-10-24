package edu.isi.bmkeg.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import com.google.common.io.Files;

public class TextUtils {

	private static int BUFFER = 2048;

	/*
	 * From http://www.javapractices.com/topic/TopicAction.do?Id=42
	 */
	static public String readFileToString(File aFile) throws Exception {

		StringBuilder contents = new StringBuilder();
		File tempUnzippedDirectory = null;

		// If the file is already in an archive, 
		// extract it to a temporary directory
		// and process that.
		if (aFile.getPath().contains(".jar!")
				|| aFile.getPath().contains(".zip!")) {

			tempUnzippedDirectory = Files.createTempDir();

			String dAddr = tempUnzippedDirectory.getAbsolutePath();

			String wholePath = aFile.getPath();
			String jarPath = wholePath.substring(0, wholePath.indexOf("!"));
			if (jarPath.startsWith("file:"))
				jarPath = jarPath.substring(5);

			String entryPath = wholePath.substring(wholePath.indexOf("!") + 2,
					wholePath.length());

			entryPath = entryPath.replaceAll("\\\\", "/");

			JarFile jarFile = new JarFile(jarPath);
			ZipEntry entry = (ZipEntry) jarFile.getEntry(entryPath);

			BufferedInputStream is = new BufferedInputStream(
					jarFile.getInputStream(entry));
			int currentByte;

			// establish buffer for writing file
			byte data[] = new byte[BUFFER];

			// write the current file to disk
			FileOutputStream fos = new FileOutputStream(dAddr + "/"
					+ aFile.getName());
			BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

			// read and write until last byte is encountered
			while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
				dest.write(data, 0, currentByte);
			}
			dest.flush();
			dest.close();
			is.close();

			aFile = new File(dAddr + "/" + aFile.getName());

		}

		BufferedReader input = new BufferedReader(new FileReader(aFile));
		try {
			String line = null; // not declared within while loop
			/*
			 * readLine is a bit quirky : it returns the content of a line MINUS
			 * the newline. it returns null only for the END of the stream. it
			 * returns an empty String if two newlines appear in a row.
			 */
			while ((line = input.readLine()) != null) {
				contents.append(line);
				contents.append(System.getProperty("line.separator"));
			}
		} finally {
			input.close();
		}

		// If we had to unzip the archive, clean it up here.
		if (tempUnzippedDirectory != null) {
			Converters.recursivelyDeleteFiles(tempUnzippedDirectory);
		}

		return contents.toString();

	}

	public static String runStripPattern(String wholeFile, String pattStr) {

		Pattern patt = Pattern.compile(pattStr, Pattern.CASE_INSENSITIVE
				| Pattern.DOTALL | Pattern.MULTILINE);

		StringBuffer myStringBuffer = new StringBuffer();
		Matcher m = patt.matcher(wholeFile);
		while (m.find()) {
			m.appendReplacement(myStringBuffer, "");
		}
		m.appendTail(myStringBuffer);

		String newFile = myStringBuffer.toString();

		return newFile;

	}

	public static String runExtractPattern(String wholeFile, String pattStr) {

		Pattern patt = Pattern.compile(pattStr, Pattern.CASE_INSENSITIVE
				| Pattern.DOTALL | Pattern.MULTILINE);

		StringBuffer myStringBuffer = new StringBuffer();
		Matcher m = patt.matcher(wholeFile);
		while (m.find()) {
			m.appendReplacement(myStringBuffer, "");
		}
		m.appendTail(myStringBuffer);

		String newFile = myStringBuffer.toString();

		return newFile;

	}

}
