package edu.isi.bmkeg.utils.dao;

import java.util.List;

public class ReturnEnvelope<T> {

	private List<T> payload;
	private long queryHitCountMax;
	public List<T> getPayload() {
		return payload;
	}
	public void setPayload(List<T> payload) {
		this.payload = payload;
	}
	public long getQueryHitCountMax() {
		return queryHitCountMax;
	}
	public void setQueryHitCountMax(long queryHitCountMax) {
		this.queryHitCountMax = queryHitCountMax;
	}
	
}
