package pl.devsite.icestreamer;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ItemFactory {
	
	private static final String SEP = File.separator;
	/*
	(([a-zA-Z]:)|\\)?([\\/][\w\.\- ]+)+
	*/
	private static final Pattern PATTERN = Pattern.compile(
			"(([a-zA-Z]:)|"+SEP+")?("+SEP+"[\\w\\.\\- ]+)+"
	);

	public Item create(String something) {		
		Matcher m = PATTERN.matcher(something);
		if (m.find() && m.start() == 0 && m.end()>1) {
			// because of filenames with national characters,
			// just ensure that first part of the string
			// matches
			return new FileItem(something);
		}
		return null;
	}
}
