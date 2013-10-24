package edu.isi.bmkeg.utils.springContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This class reads the bmkeg.properties file and puts the entries as
 * fields in this class. This allows us to use these property values from 
 * within java applications simply by instantiating the object or by calling 
 * the static methods or from Spring by using the following fragment
 * 
 * 	<bean id="propertyPlaceholderConfigurer"
 * 		class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
 * 		<property name="locations">
 * 			<list>
 * 				<value>#{evaluationContext.lookupVariable('homedir')}/bmkeg.properties</value>
 * 			</list>
 * 		</property>
 * 	</bean>
 * 	<bean id="bmkegProperties" class="edu.isi.bmkeg.utils.springContext.BmkegProperties">
 *		<property name="homeDirectory" value="#{evaluationContext.lookupVariable('homedir')}"/>
 * 		<property name="dbUrl" value="${bmkeg.dbUrl}"/>
 * 		<property name="dbUser" value="${bmkeg.dbUser}"/>	
 * 		<property name="dbPassword" value="${bmkeg.dbPassword}"/>	
 * 		... 
 * </bean>
 *  
 */
public class BmkegProperties {

	public static final String PROP_DBURL = "bmkeg.dbUrl";
	public static final String PROP_DBUSER = "bmkeg.dbUser";
	public static final String PROP_DBPASSWD = "bmkeg.dbPassword";
	public static final String PROP_DBDRIVER = "bmkeg.dbDriver";
	public static final String PROP_HOMEDIR = "bmkeg.homeDirectory";
	public static final String PROP_WORKINGDIR = "bmkeg.workingDirectory";
	public static final String PROP_PERSISTENCEUNIT = "bmkeg.persistenceUnitName";
	
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	private String dbDriver;
	private String homeDirectory;
	private String workingDirectory;
	private String persistenceUnitName;
	
	public static String readDbUrl() throws IOException  {
		BmkegProperties bmkegProperties = new BmkegProperties(true);
		return bmkegProperties.getDbUrl();
	}
	
	public static String readDbUser() throws IOException {
		BmkegProperties bmkegProperties = new BmkegProperties(true);
		return bmkegProperties.getDbUser();
	}	

	public static String readDbPassword() throws IOException {
		BmkegProperties bmkegProperties = new BmkegProperties(true);
		return bmkegProperties.getDbPassword();
	}
	
	public static String readHomeDirectory() throws IOException {
		BmkegProperties bmkegProperties = new BmkegProperties(true);
		return bmkegProperties.getHomeDirectory();
	}
	
	public static String readWorkingDirectory() throws IOException {
		BmkegProperties bmkegProperties = new BmkegProperties(true);
		return bmkegProperties.getWorkingDirectory();
	}

	public static String readPersistenceUnitName() throws IOException  {
		BmkegProperties bmkegProperties = new BmkegProperties(true);
		return bmkegProperties.getPersistenceUnitName();
	}

	/**
	 * This is the default constructor that is needed for Spring
	 */
	public BmkegProperties() {}

	/**
	 * This is a modified constructor for nonSpring use. 
	 */
	public BmkegProperties(boolean isTest) throws IOException {
		
		Properties properties = new Properties();
		if( isTest )
			properties.load(new FileInputStream("etc/bmkegtest.properties"));
		else 
			properties.load(new FileInputStream("etc/bmkeg.properties"));
	
	    this.setDbPassword((String) properties.get(PROP_DBPASSWD));
	    this.setDbUrl((String) properties.get(PROP_DBURL));
	    this.setDbUser((String) properties.get(PROP_DBUSER));
	    this.setDbDriver((String) properties.get(PROP_DBDRIVER));
	    this.setPersistenceUnitName((String) properties.get(PROP_PERSISTENCEUNIT));

	    String fileSeparator = System.getProperty("file.separator",".");
	    String homeDirectoryAddress = (String) properties.get(PROP_HOMEDIR);
		if( homeDirectoryAddress == null || !homeDirectoryAddress.endsWith(fileSeparator))
			homeDirectoryAddress += fileSeparator;

		this.setHomeDirectory(homeDirectoryAddress);

		try {
			this.setWorkingDirectory((String) properties.get(PROP_WORKINGDIR));
		} catch (Exception e) {
			// do nothing if nothing is there. 
		}
		
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbUser() {
		return dbUser;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setHomeDirectory(String workingDirectory) {
		this.homeDirectory = workingDirectory;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	public String getPersistenceUnitName() {
		return persistenceUnitName;
	}

	public void setDbDriver(String dbDriver) {
		this.dbDriver = dbDriver;
	}

	public String getDbDriver() {
		return dbDriver;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	
}
