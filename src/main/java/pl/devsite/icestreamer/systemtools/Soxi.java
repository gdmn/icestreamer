package pl.devsite.icestreamer.systemtools;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import pl.devsite.icestreamer.tags.Tags;
import pl.devsite.system.SystemProcessCallback;
import pl.devsite.system.SystemProcessInterface;
import pl.devsite.system.SystemProcessWrapper;

/**
 * Packages: sox and libsox-fmt-all needed!
 * @author dmn
 */
class Soxi implements SystemProcessInterface<String>, SystemProcessCallback<String> {

	private static final Logger logger = Logger.getLogger(Soxi.class.getName());
	private final String command = "soxi";
	private SystemProcessWrapper proc;
	private boolean collected = false;
	private String resultValue;
	private final StringBuilder buffer = new StringBuilder();
	private String additionalOptions;
	
	public static Map<String, String> getTags(String fileName) {
		String soxiResult = Soxi.query(fileName);
		if (soxiResult == null) {
			return Collections.singletonMap(Tags.PATH, fileName);
		}

		Stream<String> a = Arrays.asList(soxiResult.split("\n")).stream()
				.filter(line -> !line.trim().isEmpty())
				.filter(line -> line.indexOf('=') > -1);
		Stream<String[]> b = a.map(line -> new String[]{line.substring(0, line.indexOf('=')).toLowerCase(), line.substring(line.indexOf('=') + 1)});
		HashMap<String, String> result = b.collect(HashMap::new, (m, v) -> m.put(v[0], v[1]), HashMap::putAll);

		result.put(Tags.PATH, fileName);
		result.put("icestreamer-update", Long.toString(new File(fileName).lastModified()));
		return result;
	}
	
	public static boolean needsUpdate(String fileName, String icestreamerUpdate) {
		return icestreamerUpdate == null ||
				!icestreamerUpdate.equals(Long.toString(new File(fileName).lastModified()));
	}
	
	public static String query(String fileName) {
		try {
			File f = new File(fileName);
			if (!f.canRead()) {
				return null;
			}
			Soxi soxi = Soxi.getInstance();
			soxi.setAdditionalOptions("-D");//seconds
			String seconds = soxi.query(fileName, soxi);
			String length = null;
			
			if (seconds == null) {
				return null;
			}

			{
				int dot = seconds.indexOf('.');
				if (dot > -1) {
					seconds = seconds.substring(0, dot);
				}
				
				try {
					int secondsInt = Integer.parseInt(seconds);
					int m = secondsInt / 60;
					int s = secondsInt % 60;
					length = m + ":" + (s<10 ? "0" + s : s);
				} catch (NumberFormatException e) {}
			}

			soxi = Soxi.getInstance();
			soxi.setAdditionalOptions("-a");//tags
			return ("Length=" + length + '\n'
					+ "Seconds=" + seconds + '\n' 
					+ soxi.query(fileName, soxi)).trim();
		} catch (InstantiationException ex) {
			logger.log(Level.SEVERE, "", ex);
			return null;
		}
	}

	@Override
	public synchronized void results(String value) {
		resultValue = value;
		collected = true;
		notify();
	}

	private Soxi() throws InstantiationException {
		if (command == null || command.isEmpty()) {
			throw new InstantiationException("External command not found");
		}
	}

	private static Soxi getInstance() throws InstantiationException {
		Soxi result = new Soxi();
		return result;
	}

	private void setAdditionalOptions(String additionalOptions) {
		this.additionalOptions = additionalOptions;
	}

	@Override
	public void execute(String options, final SystemProcessCallback<String> callback) {
		try {
			proc = new SystemProcessWrapper(command, new String[]{additionalOptions, options});
			proc.addPropertyChangeListener(null, new PropertyChangeListener() {

				boolean done = false;

				@Override
				public void propertyChange(PropertyChangeEvent pce) {
					if (!SystemProcessWrapper.STD_OUT.equals(pce.getPropertyName())) {
						return;
					}
					Object value = pce.getNewValue();
					String s = value == null ? null : value.toString();
					if (s == null) {
						done = true;
						String result = buffer.length() > 0 ? buffer.substring(0, buffer.length() - 1) : buffer.toString();
						callback.results(result);
					} else if (s.isEmpty()) {
						return;
					} else {
						buffer.append(s).append('\n');
					}
				}
			});
			synchronized (this) {
				wait(5000);
			}
			if (!collected) {
				proc.kill();
			}
		} catch (IOException | InterruptedException ex) {
			Logger.getLogger(Soxi.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public String query(String options, final SystemProcessCallback<String> callback) {
		execute(options, callback);
		if (!collected) {
			return null;
		} else {
			return resultValue;
		}
	}
}
