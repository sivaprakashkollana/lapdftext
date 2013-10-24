package edu.isi.bmkeg.utils.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class allows you to parse text files using switches to 'turn on' and 'turn off' parsing 
 * in different parts (with Regular Expressions). The user may define an array of Regular Expressions
 * that act as patterns to pull out from the file automatically. The output is stored in a HashMap 
 * called 'extracts' 
 */
public class OnOffPatternParser extends ParserThread {

	private InputStream is;

	private String lineDelineator = "\n";
	private String separator = "-<|>-";

	private boolean onFlag;

	private ArrayList onPatterns;
	private ArrayList offPatterns;
	private ArrayList patternNames;

	private ArrayList matchPatterns;
	private HashMap extractPatterns;

	private String wholeFile = "";
	private String match = "";
	private HashMap extracts = new HashMap();

	public OnOffPatternParser(URL url) {
		try {
			InputStream is = url.openConnection().getInputStream();
			this.is = is;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public OnOffPatternParser(String address) {
		try {
			URL url = new URL(address);
			InputStream is = url.openConnection().getInputStream();
			this.is = is;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public OnOffPatternParser(File f) {
		try {
			this.is = new FileInputStream(f);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public OnOffPatternParser(File f, String[] onPatt,
			String[] offPatt, String[] onOffNames, String[] matchPatt,
			String[] extractNames, String[] extractPatt) {
		try {

			this.is = new FileInputStream(f);

			addOnOffPatterns(onPatt, offPatt, onOffNames);
			addMatchPatterns(matchPatt);
			addExtractPatterns(extractNames, extractPatt);

		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	
	public OnOffPatternParser(String address, String[] onPatt,
			String[] offPatt, String[] onOffNames, String[] matchPatt,
			String[] extractNames, String[] extractPatt) {
		try {
			URL url = new URL(address);
			InputStream is = url.openConnection().getInputStream();
			this.is = is;

			addOnOffPatterns(onPatt, offPatt, onOffNames);
			addMatchPatterns(matchPatt);
			addExtractPatterns(extractNames, extractPatt);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public OnOffPatternParser(InputStream is) {
		this.is = is;
	}

	public Object construct() {

		this.runInThread();

		return null;

	}
	
	public void runInThread() {
		
		wholeFile = getWholeFile(this.is);

		wholeFile = fixLineDelineation(wholeFile);

		wholeFile = this.stripExtraWhiteSpace(wholeFile);

		String[] lines = wholeFile.split(this.lineDelineator);
		HashMap map = new HashMap();
		String text = "";
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];

			int j = matchOnPatterns(line);
			int k = matchOffPatterns(line);

			if (j != -1) {
				this.onFlag = true;
			} else if (k != -1) {
				this.onFlag = false;
				match += text;
				if (text.length() > 0)
					map.put("text", text);
				text = "";
				String name = (String) this.patternNames.get(k);
				if (!this.getExtracts().containsKey(name))
					this.getExtracts().put(name, new ArrayList());
				ArrayList al = (ArrayList) this.getExtracts().get(name);
				al.add(map);
				map = new HashMap();
			}

			if (onFlag) {
				if (this.matchMatchPatterns(line)) {
					if (text.length() > 0)
						text += "\n";
					text += line;
				}
				map.putAll(this.matchExtractPatterns(line));

			}

		}
		
	}

	private String getWholeFile(InputStream is) {
		String wholeFile = "";
		try {

			if (is == null)
				return null;

			String thisLine;
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			while ((thisLine = br.readLine()) != null) {
				if (wholeFile.length() > 0)
					wholeFile += "\n";
				wholeFile += thisLine;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return wholeFile;
	}

	private String fixLineDelineation(String wholeFile) {

		if (this.lineDelineator.equals("\n"))
			return wholeFile;

		wholeFile.replaceAll("\n", " ");

		String[] newLines = wholeFile.split(this.lineDelineator);
		String newFile = "";
		for (int i = 0; i < newLines.length; i++) {
			newFile += newLines[i] + this.lineDelineator + "\n";
		}

		return newFile;

	}

	private String stripExtraWhiteSpace(String wholeFile) {

		wholeFile.replaceAll("\\s+", " ");

		return wholeFile;

	}

	private String stripDoubleLines(String wholeFile) {

		wholeFile.replaceAll("[\n]+", "\n");

		return wholeFile;

	}

	private int matchOnPatterns(String thisLine) {

		for (int i = 0; i < this.onPatterns.size(); i++) {
			String onPatt = (String) this.onPatterns.get(i);
			Pattern p = Pattern.compile(onPatt);
			Matcher m = p.matcher(thisLine);
			if (m.find()) {
				return i;
			}
		}
		return -1;

	}

	private int matchOffPatterns(String thisLine) {

		for (int i = 0; i < this.offPatterns.size(); i++) {
			String offPatt = (String) this.offPatterns.get(i);
			Pattern p = Pattern.compile(offPatt);
			Matcher m = p.matcher(thisLine);
			if (m.find()) {
				return i;
			}
		}
		return -1;

	}

	private boolean matchMatchPatterns(String thisLine) {

		Iterator it = matchPatterns.iterator();
		while (it.hasNext()) {
			String mPatt = (String) it.next();
			Pattern p = Pattern.compile(mPatt);
			Matcher m = p.matcher(thisLine);
			if (m.find()) {
				return true;
			}
		}
		return false;

	}

	private HashMap matchExtractPatterns(String thisLine) {

		HashMap map = new HashMap();
		Iterator it = extractPatterns.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String exPatt = (String) extractPatterns.get(key);

			Pattern p = Pattern.compile(exPatt);
			Matcher m = p.matcher(thisLine);

			if (m.find() && m.groupCount() > 0) {
				map.put(key, m.group(1));
			}

		}
		return map;

	}

	public void addOnOffPatterns(String[] onPatts, String[] offPatts,
			String[] pattNames) throws Exception {

		if (onPatts.length != offPatts.length
				|| onPatts.length != pattNames.length) {
			throw new Exception("On/Off Patterns & Names must be the same size");
		}

		this.onPatterns = new ArrayList();
		for (int i = 0; i < onPatts.length; i++) {
			this.onPatterns.add(onPatts[i]);
		}

		this.offPatterns = new ArrayList();
		for (int i = 0; i < offPatts.length; i++) {
			this.offPatterns.add(offPatts[i]);
		}

		this.patternNames = new ArrayList();
		for (int i = 0; i < pattNames.length; i++) {
			this.patternNames.add(pattNames[i]);
		}

	}

	public void addMatchPatterns(String[] patts) {
		this.matchPatterns = new ArrayList();
		for (int i = 0; i < patts.length; i++) {
			this.matchPatterns.add(patts[i]);
		}
	}

	public void addExtractPatterns(String[] names, String[] patts)
			throws Exception {
		if (names.length != patts.length)
			throw new Exception("must have equal number of patterns and names!");

		this.extractPatterns = new HashMap();
		for (int i = 0; i < patts.length; i++) {
			this.extractPatterns.put(names[i], patts[i]);
		}
	}

	public void setExtracts(HashMap extracts) {
		this.extracts = extracts;
	}

	public HashMap getExtracts() {
		return extracts;
	}

}
