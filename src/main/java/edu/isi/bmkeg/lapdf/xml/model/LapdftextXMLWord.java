package edu.isi.bmkeg.lapdf.xml.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="word")
public class LapdftextXMLWord extends LapdftextXMLRectangle implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String t;

	@XmlAttribute	
	public String getT() {
		return t;
	}

	public void setT(String t) {
		this.t = t;
	}	
	
}
