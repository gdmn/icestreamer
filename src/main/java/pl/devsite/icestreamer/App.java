package pl.devsite.icestreamer;

import pl.devsite.icestreamer.render.RenderPlain;
import pl.devsite.icestreamer.render.RenderFactory;
import pl.devsite.icestreamer.render.JsonTransformer;
import pl.devsite.icestreamer.item.Item;
import pl.devsite.icestreamer.tags.TagsService;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import pl.devsite.icestreamer.item.Items;
import pl.devsite.icestreamer.tags.Tags;
import spark.Request;
import spark.Response;
import static spark.Spark.*;

// http://sparkjava.com/documentation.html
public class App {

	private static final Logger logger = Logger.getLogger(App.class.getName());
	Items allItems = new Items();
	int defaultPort = 0;
	String defaultName = "localhost";
	private static final String VERSION = "iceserver-api-1.0";

	void run() {
		int maxThreads = 8;
		int minThreads = -1;
		int timeOutMillis = -1;
		threadPool(maxThreads, minThreads, timeOutMillis);

		port(defaultPort);

		staticFileLocation("/static");

		get("/", (request, response) -> {
			response.redirect("/gui");
			return null;
		});

		get("/status", (request, response) -> {
			response.type("text/plain");
			response.header("Access-Control-Allow-Origin", "*");
			return status(request, response);
		});

		get("/version", (request, response) -> {
			response.type("text/plain");
			return VERSION;
		});

		get("/stream/:hashcode", (request, response) -> {
			Integer h = parseHashcode(request.params(":hashcode"));
			if (!isIcy(request)) {
				response.redirect(request.pathInfo().replace("/stream/", "/download/"));
				return null;
			}
			if (h != null) {
				Item item = allItems.get(h);
				if (item != null) {
					RenderFactory.create(queriedUserAgent(request), response, item).send();
					halt();
					return null;
				}
			}
			halt(404);
			return null;
		});

		get("/download/:hashcode", (request, response) -> {
			Integer h = parseHashcode(request.params(":hashcode"));
			if (h != null) {
				Item item = allItems.get(h);
				if (item != null) {
					new RenderPlain(response, item).send();
					halt();
					return null;
				}
			}
			halt(404);
			return null;
		});

		get("/info/:hashcode", (request, response) -> {
			response.type("text/plain");
			Integer h = parseHashcode(request.params(":hashcode"));
			if (h != null) {
				response.type("application/json");
				Item item = allItems.get(h);
				if (item != null) {
					Map<String, String> result = item.getTags();
					return new JsonTransformer().render(result);
				}
			}
			halt(404);
			return null;
		});

		get("/clear", (request, response) -> {
			TagsService.getInstance().clear();
			response.redirect("/status");
			return null;
		});

		get("/clean", (request, response) -> {
			TagsService.getInstance().clean();
			response.redirect("/status");
			return null;
		});

		get("/list", (request, response) -> {
			String host = queriedHost(request);
			String filter = queriedSearch(request);
			if (filter != null) {
				if (!(filter.contains("*") || filter.contains("?"))) {
					filter = ".*" + filter + ".*";
				}
			}
			List<Tags> items = filterAndSort(TagsService.getInstance().values(), filter);

			if ("application/json".equals(request.headers("Accept"))) {
				response.type("application/json");
				Map<String, Object> result = new HashMap<>();
				result.put("total", items.size());
				result.put("data", items);
				result.put("success", true);
				result.put("message", "OK");
				return new JsonTransformer().render(result);
			} else {
				//ItemFactory factory = new ItemFactory();
				//List<Item> itemsList = items.parallelStream().map(tags -> factory.create(tags)).collect(Collectors.toList());
				return render(response, request.queryParams("format"), items, host);
			}
		});

		post("/", (request, response) -> {
			String host = queriedHost(request);

			String body = request.body();
			List<Tags> tagsList = stringsToTagsCollection(body);
			logger.log(Level.INFO, "New size: {0}", TagsService.getInstance().size());
			return render(response, request.queryParams("format"), tagsList, host);
		});
	}

	private List<Tags> filterAndSort(Collection<Tags> input, String regex) {
		List<Tags> tagsList;
		tagsList = new ArrayList<>(input);
		if (regex != null && !regex.isEmpty()) {
			Pattern pattern = Pattern.compile(regex);
			for (Iterator<Tags> it = tagsList.iterator(); it.hasNext();) {
				Tags item = it.next();
				if (!item.matches(pattern)) {
					it.remove();
				}
			}
		}

		Collections.sort(tagsList);
		return tagsList;
	}

	private List<Item> stringsToItemCollection(String lines) {
		Items items = new Items();
		return items.feed(parseLines(lines));
	}

	private List<Tags> stringsToTagsCollection(String lines) {
		List<Item> itemCollection = stringsToItemCollection(lines);
		List<Tags> tagsCollection = itemCollection.parallelStream().map(item -> TagsService.getInstance().getTags(item)).collect(Collectors.toList());
		return tagsCollection;
	}

	private void init(String[] args) throws IOException {
		int i = 0;
		defaultPort = 0;
		boolean isThereServerAlready = false;
		boolean isPortBusy;
		String defaultDatabase = "icestreamer.db";

		while (i < args.length) {
			if ("-p".equalsIgnoreCase(args[i]) || "--port".equalsIgnoreCase(args[i])) {
				String pString = args[++i];
				defaultPort = Integer.parseInt(pString);
			}
			if ("-n".equalsIgnoreCase(args[i]) || "--name".equalsIgnoreCase(args[i])) {
				defaultName = args[++i];
			}
			if ("-d".equalsIgnoreCase(args[i]) || "--database".equalsIgnoreCase(args[i])) {
				defaultDatabase = args[++i];
			}
			i++;
		}

		if (defaultPort == 0) {
			defaultPort = findFreePort();
			isPortBusy = false;
		} else {
			isPortBusy = isPortBusy(defaultPort);
			isThereServerAlready = isThereServer(defaultPort);
		}

		int bytesAvailable = -1;
		try {
			bytesAvailable = System.in.available();
			logger.log(Level.FINE, "System.in.available(): {0}", bytesAvailable);
		} catch (IOException ex) {
			Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		}

		if (bytesAvailable > -1) {
			Thread t = new Thread(() -> {
				Scanner sc = new Scanner(System.in);
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					logger.log(Level.FINE, "System.in: {0}", line);
					try {
						System.out.print(renderTagsRaw(stringsToTagsCollection(line), defaultName + ":" + defaultPort));
					} catch (Exception e) {
						logger.log(Level.WARNING, null, e);
					}
					System.out.flush();
				}
				logger.log(Level.INFO, "System.in exhausted");
			});

			if (!isThereServerAlready && isPortBusy) {
				logger.log(Level.SEVERE, "Port is not free but there is not compatibile icestreamer running");
				System.exit(1);
			}

			if (!isThereServerAlready) {
				logger.log(Level.INFO, "Reading System.in in daemon mode");
				t.setDaemon(true);
				t.start();
			} else {
				logger.log(Level.INFO, "Reading System.in and passing data to the server");

				Scanner sc = new Scanner(System.in);
				StringBuilder lines = new StringBuilder();
				int lineCounter = 0;
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					lines.append(line).append("\n");
					lineCounter++;
				}
				logger.log(Level.INFO, "System.in exhausted, {0} line(s) read", lineCounter);
				SparkTestUtil util = new SparkTestUtil(defaultPort);
				SparkTestUtil.UrlResponse urlResponse;
				try {
					urlResponse = util.doMethod("POST", "/", lines.toString());
					logger.log(Level.INFO, "Response code {1}, body length {0}", new Object[]{urlResponse.body.length(), urlResponse.status});
					System.out.println(urlResponse.body);
				} catch (Exception ex) {
					logger.log(Level.SEVERE, null, ex);
				}

				System.exit(0);
			}
		}

		TagsService.initialize(defaultDatabase);
		logger.log(Level.INFO, "Tags database ''{0}'' opened, {1} items found", new Object[]{defaultDatabase, allItems.size()});
	}

	boolean isIcy(Request request) {
		for (String h : request.headers()) {
			if ("Icy-MetaData".equalsIgnoreCase(h)) {
				String icy = request.headers(h);
				return "1".equals(icy);
			}
		}
		return false;
	}

	private String queriedUserAgent(Request request) {
		for (String h : request.headers()) {
			if ("User-Agent".equalsIgnoreCase(h)) {
				return request.headers(h);
			}
		}
		return null;
	}

	private String queriedHost(Request request) {
		Object queryParamPort = request.queryParams("port");
		Object queryParamName = request.queryParams("name");

		if (queryParamName != null || queryParamPort != null) {
			queryParamName = queryParamName != null ? queryParamName : defaultName;
			queryParamPort = queryParamPort != null ? queryParamPort : defaultPort;
			return String.valueOf(queryParamName) + ":" + String.valueOf(queryParamPort);
		}

		String queriedHost = request.host();
		if (queriedHost != null && !queriedHost.isEmpty()) {
			return queriedHost;
		}

		return defaultName + ":" + defaultPort;
	}

	private String queriedSearch(Request request) {
		Object querySearch = request.queryParams("s");
		return querySearch != null ? String.valueOf(querySearch) : null;
	}

	private Integer parseHashcode(String hash) {
		if (hash.startsWith("h") && hash.length() > 1) {
			hash = hash.substring(1);
			return Integer.parseUnsignedInt(hash, 16);
		}
		return null;
	}

	private String[] parseLines(String body) {
		String[] bodyLines;
		if (body.indexOf(0) >= 0) {
			bodyLines = body.split("\0");
		} else {
			bodyLines = body.replaceAll("\r", "").split("\n");
		}
		return bodyLines;
	}

	public static void main(String[] args) throws Exception {
		App app = new App();
		app.init(args);
		app.run();
	}

	App() {
	}

	private boolean isPortBusy(int number) {
		try {
			ServerSocket socket = new ServerSocket(number);
			socket.close();
			return false;
		} catch (IOException ex) {
		}
		return true;
	}

	private boolean isThereServer(int defaultPort) {
		SparkTestUtil util = new SparkTestUtil(defaultPort);
		SparkTestUtil.UrlResponse urlResponse;
		try {
			urlResponse = util.doMethod("GET", "/version", null);
			logger.log(Level.FINE, "Response code {1}, body length {0}", new Object[]{urlResponse.body.length(), urlResponse.status});
			logger.log(Level.INFO, "{0} == {1} = {2}", new Object[]{VERSION, urlResponse.body, VERSION.equals(urlResponse.body)});
			return VERSION.equals(urlResponse.body);
		} catch (Exception ex) {
			logger.log(Level.INFO, "Request failed: {0}", ex.getMessage());
		}
		return false;
	}

	private int findFreePort() {
		int number = 6680;
		do {
			try {
				ServerSocket socket = new ServerSocket(number);
				socket.close();
				logger.log(Level.INFO, "Serving on {0}", socket.getLocalPort());
				return socket.getLocalPort();
			} catch (IOException ex) {
				logger.log(Level.WARNING, "Port {0} seems busy", number);
			}
			number++;
		} while (true);
	}

	private String status(Request request, Response response) {
		StringBuilder result = new StringBuilder();
		result.append("Items: ").append(allItems.size()).append("\n")
				.append("IP: ").append(request.ip()).append("\n")
				.append("Host: ").append(request.host()).append("\n")
				.append("Port: ").append(request.port()).append("\n");
		result.append("\n");
		return result.toString();
	}

	private String render(Response response, String format, Collection<Tags> list, String host) {
		if ("m3u".equalsIgnoreCase(format)) {
			response.type("audio/x-mpegurl");
			return renderTagsM3u(list, host);
		} else if ("names".equalsIgnoreCase(format)) {
			response.type("text/plain");
			return renderTagsPaths(list, host);
		} else {
			response.type("text/plain");
			return renderTagsRaw(list, host);
		}
	}

	private String renderTagsPaths(Collection<Tags> list, String host) {
		StringBuilder result = new StringBuilder();
		list.stream().forEach((i) -> {
			result.append(i.getPath()).append("\n");
		});
		return result.toString();
	}

	private String renderTagsRaw(Collection<Tags> list, String host) {
		StringBuilder result = new StringBuilder();
		list.stream().forEach((i) -> {
			result.append("http://").append(host).append("/stream/");
			result.append(i.getHhashcode()).append("\n");
		});
		return result.toString();
	}

	private String renderTagsM3u(Collection<Tags> list, String host) {
		StringBuilder result = new StringBuilder();
		result.append("#EXTM3U").append("\n");
		list.stream().forEach((i) -> {

			result.append("#EXTINF:").append(i.getSeconds()).append(", ").append(i.getArtistAndTitle()).append("\n");
			result.append("http://").append(host).append("/stream/");
			result.append(i.getHhashcode()).append("\n");
		});
		return result.toString();
	}
}
