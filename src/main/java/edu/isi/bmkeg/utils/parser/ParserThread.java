package edu.isi.bmkeg.utils.parser;

import edu.isi.bmkeg.utils.SwingWorker;

import java.io.File;
import javax.swing.ProgressMonitor;
import javax.swing.event.EventListenerList;

/**
 * <p>
 * Abstract class to hold all methods concerned with generic parsing functions.
 * These include multithreading, progress monitoring and callbacks
 * </p>
 * <p>
 * <strong>usage:</strong><br>
 * <code>
 * </code>
 * </p>
 * <p>
 * Use the method <code>parseCompleted(ParserEvent evt)</code> to gain acess to
 * the gp object via the <code>evt.getSource()</code> method The contents of the
 * parse are available via <code>get***</code> methods.
 * </p>
 * 
 * @author Gully APC Burns
 * @version 1.0
 */
public abstract class ParserThread extends SwingWorker {
	//
	// Listeners and Events for callbacks
	//
	//
	// Swingworker components
	//

	private File file;

	protected EventListenerList listenerList = new EventListenerList();

	protected int lengthOfTask;

	protected int onePercent;

	protected int current = 0;

	protected String statMessage;

	protected ProgressMonitor progress;

	protected boolean done = false;

	protected boolean canceled = false;

	protected boolean broken = false;

	public void addParseEventListener(ParserEventListener listener) {
		listenerList.add(ParserEventListener.class, listener);
	}

	/**
	 * Gets all listeners registered for Parser events
	 * 
	 * @return ParserEventListener[]
	 */
	public ParserEventListener[] getParseEventListeners() {
		Object[] listeners = listenerList.getListenerList();
		ParserEventListener[] vel = new ParserEventListener[listeners.length / 2];
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ParserEventListener.class) {
				vel[i] = (ParserEventListener) listeners[i + 1];
			}
		}
		if (vel.length == 0) {
			return null;
		} else {
			return vel;
		}
	}

	/**
	 * Permits listeners to unregister for Parse events
	 * 
	 * @param listener
	 *            ParserEventListener
	 */
	public void removeParserEventListener(ParserEventListener listener) {
		listenerList.remove(ParserEventListener.class, listener);

	}

	/**
	 * Fires Parse Events in all registered listeners
	 * 
	 * @param evt
	 *            ParserEvent
	 */
	public void fireParserEvent(ParserEvent evt) throws Exception {
		Object[] listeners = listenerList.getListenerList();
		// Each listener occupies two elements - the first is the listener class
		// and the second is the listener instance
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ParserEventListener.class) {
				ParserEventListener listener = (ParserEventListener) listeners[i + 1];
				listener.parseCompleted(evt);
			}
		}
	}

	/**
	 * Is the process done?
	 * 
	 * @return boolean
	 * @uml.property name="done"
	 */
	public boolean isDone() {
		return this.done;
	}

	/**
	 * Has the process been cancelled?
	 * 
	 * @return boolean
	 */
	public boolean getCancelled() {
		return this.canceled;
	}

	/**
	 * Set the cancelation state of the parser
	 * @throws Exception 
	 */
	public void finished() throws Exception {
		this.done = true;
		this.fireParserEvent(new ParserEvent(this, true));
	}

	public void parseFile(File file) {

		this.file = file;

		this.start();

	}

	public File getFile() {
		if (broken)
			return null;
		return this.file;
	}

	public void setFile(File file) {
		if (!broken)
			this.file = file;
	}
}
