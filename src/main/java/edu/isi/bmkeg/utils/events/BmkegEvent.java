package edu.isi.bmkeg.utils.events;

public class BmkegEvent {
	
	private BmkegState state = null;
	
	public static String CANCEL = "cancel";
	public static String LIST_COMPLETED = "listCompleted";
	public static String UPDATE_COMPLETED = "updateCompleted";
	public static String INSERT_COMPLETED = "insertCompleted";
	public static String DISPLAY_COMPLETED = "displayCompleted";
	public static String DELETE_COMPLETED = "deleteCompleted";
	
	public void setState(BmkegState state) {
		this.state = state;
	}

	public BmkegState getState() {
		return state;
	}

}
