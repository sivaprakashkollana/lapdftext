package edu.isi.bmkeg.utils.svn.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="svnRepository")
public class SVNRepositoryEntity {

    private String url;
    
    private String timeStamp;
	
	private SVNEntryElement root;	

	public void setUrl(String url) {
		this.url = url;
	}

	@XmlAttribute
	public String getUrl() {
		return url;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	@XmlAttribute
	public String getTimeStamp() {
		return timeStamp;
	}

	@XmlElement(name="root")
	public SVNEntryElement getRoot() {
		return root;
	}

	public void setRoot(SVNEntryElement root) {
		this.root = root;
	}
	
}
