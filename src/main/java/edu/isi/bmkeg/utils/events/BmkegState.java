package edu.isi.bmkeg.utils.events;

import java.util.HashMap;

public class BmkegState {
	
	private String type = "";
	private HashMap data = new HashMap();
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public void setData(HashMap data) {
		this.data = data;
	}
	
	public HashMap getData() {
		return data;
	}

}
