package pl.devsite.system;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class SystemProcessWrapper extends SystemProcess {

	public static final String ERR_OUT = "err";
	public static final String STD_OUT = "std";
	private final Object listenerLock = new Object();
	private PrintStream processPrintIn;
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public SystemProcessWrapper(String processPath, String processOptions) throws IOException {
		super(processPath, processOptions);
		initializeStreams();
	}

	public SystemProcessWrapper(String processPath, String[] processOptions) throws IOException {
		super(processPath, processOptions);
		initializeStreams();
	}

	private void initializeStreams() {
		new LineCapture(getProcessStd(), STD_OUT).start();
		new LineCapture(getProcessErr(), ERR_OUT).start();
		processPrintIn = new PrintStream(getProcessIn());
	}

	private String printAndWait(String property, String input, String expected) throws InterruptedException {
		synchronized (listenerLock) {
			OneLineListener listener = new OneLineListener(input, expected);
			try {
				propertyChangeSupport.addPropertyChangeListener(listener);
				String result = listener.getResult();
				return result;
			} finally {
				propertyChangeSupport.removePropertyChangeListener(listener);
			}
		}
	}

	public String printAndWait(String input, String expected) throws InterruptedException {
		return printAndWait(null, input, expected);
	}

	public SystemProcessWrapper print(String value) {
		processPrintIn.print(value);
		return this;
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (propertyName == null) {
			propertyChangeSupport.addPropertyChangeListener(listener);
		}
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	private class OneLineListener implements PropertyChangeListener {

		private final String waitFor, input;
		private String result;

		public OneLineListener(String input, String waitFor) {
			this.waitFor = waitFor;
			this.input = input;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			synchronized (this) {
				String value = evt.getNewValue().toString();
				if (waitFor == null || value.startsWith(waitFor)) {
					result = evt.getNewValue().toString();
					this.notify();
				}
			}
		}

		public String getResult() throws InterruptedException {
			if (input != null) {
				try {
					flush();
				} catch (IOException ex) {
				}
			}
			int counter = 0;
			synchronized (this) {
				while (result == null && ++counter < 10) {
					this.wait(1000);
				}
			}
			return result;
		}
	}

	private class LineCapture extends Thread {

		private final InputStream in;
		private final String propertyName;

		LineCapture(InputStream in, String prefix) {
			this.in = in;
			this.propertyName = prefix;
		}

		@Override
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				String line;
				do {
					line = reader.readLine();
					propertyChangeSupport.firePropertyChange(propertyName, null, line);
				} while (line != null);
			} catch (IOException ex) {
				Logger.getLogger(SystemProcessWrapper.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
