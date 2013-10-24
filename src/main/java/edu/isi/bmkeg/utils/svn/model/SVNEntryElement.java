package edu.isi.bmkeg.utils.svn.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="svnEntry")
public class SVNEntryElement {
		
	private String wholePath;
	
	private String author;
	
	private String name;
	
	private String type;

	private Long size;
	
	private List<SVNEntryElement> contents = new ArrayList<SVNEntryElement>();
	
	
	@XmlAttribute
	public String getWholePath() {
		return wholePath;
	}

	public void setWholePath(String wholePath) {
		this.wholePath = wholePath;
	}

	@XmlAttribute
	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

    @XmlElementWrapper( name="contents" )
    @XmlElement( name="svnEntry" )
	public List<SVNEntryElement> getContents() {
		return contents;
	}

	public void setContents(List<SVNEntryElement> contents) {
		this.contents = contents;
	}

	@XmlAttribute
	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}
		
}
