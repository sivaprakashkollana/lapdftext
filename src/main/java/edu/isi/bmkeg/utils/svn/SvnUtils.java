package edu.isi.bmkeg.utils.svn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import edu.isi.bmkeg.utils.TextUtils;
import edu.isi.bmkeg.utils.svn.model.SVNEntryElement;
import edu.isi.bmkeg.utils.svn.model.SVNRepositoryEntity;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;

public class SvnUtils implements ISVNStatusHandler {
		
	private SVNClientManager ourClientManager;
	 
	private HashMap<String,SVNEntryElement> lookup = new HashMap<String,SVNEntryElement>();

	private SVNRepository repository = null;
	private SVNRepositoryEntity repEntity = null;
	
	private static Logger logger = Logger.getLogger(SvnUtils.class);
	
	private long version;
	private boolean changed = false;
	
	private String url;
	private String login;
	private String password;
	private File localCopy;

	public SvnUtils(String url, String login, String password, File localCopy) throws Exception {

		this.url = url;
		this.login = login;
		this.password = password;
		this.localCopy = localCopy;
		
		if( !localCopy.exists() ) {
			throw new Exception(this.localCopy.getPath() + " does not exist");
		}
		
		if( !localCopy.isDirectory() ) {
			throw new Exception(this.localCopy.getPath() + " is not a directory");			
		}
			
		this.setupLibrary();
		
		ISVNEventHandler commonEventHandler;
		
		repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(login, password);
        repository.setAuthenticationManager(authManager);
        
        version = repository.getLatestRevision();

        SVNEntryElement root = new SVNEntryElement();
    	root.setName("root");
    	root.setType("root");
		
		listEntries(root, "");
		
		this.setRepEntity(new SVNRepositoryEntity());
		this.getRepEntity().setUrl(url);
					
		this.getRepEntity().setRoot(root);
		
		String timeStamp = (new Date()).toString();
		this.getRepEntity().setTimeStamp(timeStamp);
			
		ourClientManager = SVNClientManager.newInstance();
	
	}	
		
    private void setupLibrary() {
        /*
         * For using over http:// and https://
         */
        DAVRepositoryFactory.setup();
        /*
         * For using over svn:// and svn+xxx://
         */
        SVNRepositoryFactoryImpl.setup();
        
        /*
         * For using over file:///
         */
        FSRepositoryFactory.setup();
    }
    
    /*
     * Gets the contents of the directory specified by path at the latest
     * revision (for this purpose -1 is used here as the revision number to
     * mean HEAD-revision) getDir returns a Collection of SVNDirEntry
     * elements. SVNDirEntry represents information about the directory
     * entry. Here this information is used to get the entry name, the name
     * of the person who last changed this entry, the number of the revision
     * when it was last changed and the entry type to determine whether it's
     * a directory or a file. If it's a directory listEntries steps into a
     * next recursion to display the contents of this directory. The third
     * parameter of getDir is null and means that a user is not interested
     * in directory properties. The fourth one is null, too - the user
     * doesn't provide its own Collection instance and uses the one returned
     * by getDir.
     */
    private void listEntries(SVNEntryElement parent, String wholePath) throws Exception {
    	
		Collection entries = repository.getDir(wholePath, -1, null, (Collection) null);
		Iterator iterator = entries.iterator();
		while (iterator.hasNext()) {
		    SVNDirEntry entry = (SVNDirEntry) iterator.next();
		    
		    SVNEntryElement e = new SVNEntryElement();
		    e.setWholePath(wholePath + "/" + entry.getName());
	    	e.setAuthor(entry.getAuthor());
	    	e.setName(entry.getName());
	    	e.setSize(entry.getSize());
		    lookup.put(wholePath + "/" + entry.getName(), e);
	    	parent.getContents().add(e);
	    	
		    if(entry.getKind() == SVNNodeKind.FILE ) {
		    
		    	e.setType("file");
			    
		    } else if (entry.getKind() == SVNNodeKind.DIR) {

		    	e.setType("directory");
   	
		    	listEntries(e, (wholePath.equals("")) ? entry.getName()
		                : wholePath + "/" + entry.getName());

		    }
		    
		}
			
	}
    
    public void getEntry(String filePath, File output) throws Exception {
    	
    	if( !lookup.containsKey(filePath) ) {
    		throw new Exception(filePath + " is outside of the scope of this SvnUtils object");
    	}
    	
    	 SVNNodeKind nodeKind = repository.checkPath( filePath , -1 );
    	 
         if ( nodeKind == SVNNodeKind.NONE ) {
             throw new Exception( "There is no entry at '" + url + "/" + filePath + "'." );
         } else if ( nodeKind == SVNNodeKind.DIR ) {
        	 throw new Exception( "The entry at '" + url + "/" + filePath + "' is a directory while a file was expected." );
         }
    	
         Map<String,String> fileProperties = new HashMap<String,String>( );
         ByteArrayOutputStream baos = new ByteArrayOutputStream( );
         repository.getFile( filePath , -1 , fileProperties , baos );
         
         String mimeType = ( String ) fileProperties.get( SVNProperty.MIME_TYPE );
         boolean isTextType = SVNProperty.isTextMimeType( mimeType );

         Iterator iterator = fileProperties.keySet( ).iterator( );
         while ( iterator.hasNext( ) ) {
             String propertyName = ( String ) iterator.next( );
             String propertyValue = ( String ) fileProperties.get( propertyName );
             System.out.println( "File property: " + propertyName + "=" + propertyValue );
         }
         
         FileOutputStream fos = new FileOutputStream(output);
         fos.write(baos.toByteArray());
         
	}
        
    public void saveAsXml(File xmlFile) throws Exception {

    	FileWriter fw = new FileWriter(xmlFile);
    	StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(this.getRepEntity(), writer);		
		String str = writer.toString();
		fw.write(str);

    }

    public void loadXml(File xmlFile) throws Exception {

    	String xml = TextUtils.readFileToString(xmlFile);
	    
		StringReader reader = new StringReader(xml);
		this.setRepEntity(XmlBindingTools.parseXML(reader, SVNRepositoryEntity.class));

    }

	public SVNRepositoryEntity getRepEntity() {
		return repEntity;
	}

	public void setRepEntity(SVNRepositoryEntity repEntity) {
		this.repEntity = repEntity;
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public long checkout() throws SVNException {

		SVNRevision revision = SVNRevision.create(this.repository.getLatestRevision());

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient( );
	    updateClient.setIgnoreExternals( false );
	    return updateClient.doCheckout( this.repository.getLocation(), this.localCopy, revision , revision , true );
	   
	}

	public long update() throws SVNException {

		SVNRevision revision = SVNRevision.create(this.repository.getLatestRevision());

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient( );
	    updateClient.setIgnoreExternals( false );
	    return updateClient.doUpdate( this.localCopy, revision, true );
	   
	}

	public SVNCommitInfo commit(File[] wcPaths, String commitMessage) throws SVNException {

		SVNCommitClient commitClient = ourClientManager.getCommitClient( );
	    return commitClient.doCommit( wcPaths, true, commitMessage, false, true);
	   
	}

	public boolean checkForChange() throws SVNException {
	
		SVNStatusClient checker = ourClientManager.getStatusClient();
		long status = checker.doStatus(localCopy, true, true, true, false, this);

		return changed;
		
	}

	public void handleStatus(SVNStatus status) throws SVNException {

		System.out.println(status.getFile().getName() + ":" + status.getContentsStatus());
		
		if( status.getContentsStatus() == SVNStatusType.STATUS_MODIFIED || 
				status.getContentsStatus() == SVNStatusType.STATUS_ADDED ||
				status.getContentsStatus() == SVNStatusType.STATUS_DELETED )
			this.changed = true;
		
	}
	
/*	private void saveSVNRepositoryEntity() throws Exception{
		logger.debug("** persisting the model...");

		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory(PERSISTENCE_UNIT);

		EntityManager em = emf.createEntityManager();

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		try {
			em.persist(this.repEntity);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("** Error: " + e.getMessage());
			tx.rollback();
			throw e;
		} finally {
			logger.info("** Closing Entity Manager.");
			em.close();
		}
	
	}
	
	public List<SVNRepositoryEntity> listSVNRepositoryEntities() {
		
		logger.debug("** list all SVNRepositoryEntities...");

		EntityManagerFactory entityManagerFactory = Persistence
				.createEntityManagerFactory(PERSISTENCE_UNIT);

		EntityManager em = entityManagerFactory.createEntityManager();

		Query findAllQuery = em.createQuery("select sre from SVNRepositoryEntity as sre");
		List<SVNRepositoryEntity> reps = findAllQuery.getResultList();
		
		if (reps != null)
			logger.debug("** Found " + reps.size() + " repositories:");
		
		return reps;
		
	}
	
	public SVNRepositoryEntity displaySVNRepositoryEntity(Long id) {
		
		logger.debug("** list all SVNRepositoryEntities...");

		EntityManagerFactory entityManagerFactory = Persistence
				.createEntityManagerFactory(PERSISTENCE_UNIT);

		EntityManager em = entityManagerFactory.createEntityManager();

		Query getQuery = em.createQuery("select sre from SVNRepositoryEntity as sre where sre.id=" + id);
		List<SVNRepositoryEntity> reps = getQuery.getResultList();
		
		if (reps != null)
			logger.debug("** Found " + reps.size() + " repositories:");
		
		return reps.get(0);
		
	}

	public void setRepEntity(SVNRepositoryEntity repEntity) {
		this.repEntity = repEntity;
	}

	public SVNRepositoryEntity getRepEntity() {
		return repEntity;
	}

/*	public List<Article> getArticles() {
		
		logger.debug("** getArticles called...");

		EntityManagerFactory entityManagerFactory = Persistence
				.createEntityManagerFactory(PERSISTENCE_UNIT);

		logger.debug("1");

		EntityManager em = entityManagerFactory.createEntityManager();

		logger.debug("2");

		Query findAllQuery = em.createNamedQuery("articles.findAll");
		List<Article> articles = findAllQuery.getResultList();

		logger.debug("3");

		if (articles != null)
			logger.debug("** Found " + articles.size() + " records:");

		logger.debug("4");

		return articles;
	}

	public void addUpdateArticle(Article article) throws Exception {
		logger.debug("** addUpdateArticle called...");

		Journal j = article.getJournal();
		
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory(PERSISTENCE_UNIT);

		EntityManager em = emf.createEntityManager();

		// When passing Boolean and Number values from the Flash client to a
		// Java object, Java interprets null values as the default values for
		// primitive types; for example, 0 for double, float, long, int, short,
		// byte.
		if (article.getId() == null
				|| article.getId() == 0) {
			// New article is created
			article.setId(null);
			//article.setCreated(new Timestamp(new Date().getTime()));
		} else {
			// Existing article is updated - do nothing.
		}

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		try {
			em.persist(article);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("** Error: " + e.getMessage());
			tx.rollback();
			throw new Exception(e.getMessage());
		} finally {
			logger.info("** Closing Entity Manager.");
			em.close();
		}
	}

	public void deleteArticle(Long articleId) {
		logger.debug("** deleteArticle called...");

		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory(PERSISTENCE_UNIT);

		EntityManager em = emf.createEntityManager();

		Query q = em.createNamedQuery("articles.byId");
		q.setParameter("articleId", articleId);
		Article article = (Article) q.getSingleResult();

		if (article != null) {
			EntityTransaction tx = em.getTransaction();
			tx.begin();
			try {
				em.remove(article);
				tx.commit();
			} catch (Exception e) {
				logger.error("** Error: " + e.getMessage());
				tx.rollback();
			} finally {
				logger.info("** Closing Entity Manager.");
				em.close();
			}
		}
	} */
}
