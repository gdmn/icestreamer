package pl.devsite.icestreamer;

import lombok.extern.slf4j.Slf4j;
import pl.devsite.icestreamer.render.RenderPlain;
import pl.devsite.icestreamer.render.RenderFactory;
import pl.devsite.icestreamer.render.JsonTransformer;
import pl.devsite.icestreamer.item.Item;
import pl.devsite.icestreamer.tags.TagsService;
import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import pl.devsite.icestreamer.item.Items;
import pl.devsite.icestreamer.tags.Tags;
import spark.Request;
import spark.Response;
import static spark.Spark.*;

// http://sparkjava.com/documentation.html

@Slf4j
public class App {

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
			long t1 = System.currentTimeMillis();
			List<Tags> items = TagsService.getInstance().filterAndSort(filter);
			long t2 = System.currentTimeMillis();

			if ("application/json".equals(request.headers("Accept"))) {
				response.type("application/json");
				Map<String, Object> result = new HashMap<>();
				result.put("total", items.size());
				result.put("data", items);
				result.put("icestreamer-query-time", Long.toString(t2 - t1));
				result.put("success", true);
				result.put("message", "OK");
				return new JsonTransformer().render(result);
			} else {
				return render(response, request.queryParams("format"), items, host);
			}
		});

		post("/", (request, response) -> {
			String host = queriedHost(request);

			String body = request.body();
			List<Tags> tagsList = stringsToTagsCollection(body);
			log.info("New size: {}", TagsService.getInstance().size());
			return render(response, request.queryParams("format"), tagsList, host);
		});
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
			log.debug("System.in.available(): {}", bytesAvailable);
		} catch (IOException ex) {
			log.error("", ex);
		}

		if (bytesAvailable > -1) {
			Thread t = new Thread(() -> {
				Scanner sc = new Scanner(System.in);
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					log.debug("System.in: {}", line);
					try {
						System.out.print(renderTagsRaw(stringsToTagsCollection(line), defaultName + ":" + defaultPort));
					} catch (Exception e) {
						log.warn(null, e);
					}
					System.out.flush();
				}
				log.info("System.in exhausted");
			});

			if (!isThereServerAlready && isPortBusy) {
				log.error("Port is not free but there is not compatibile icestreamer running");
				System.exit(1);
			}

			if (!isThereServerAlready) {
				log.info("Reading System.in in daemon mode");
				t.setDaemon(true);
				t.start();
			} else {
				log.info("Reading System.in and passing data to the server");

				Scanner sc = new Scanner(System.in);
				StringBuilder lines = new StringBuilder();
				int lineCounter = 0;
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					lines.append(line).append("\n");
					lineCounter++;
				}
				log.info("System.in exhausted, {} line(s) read", lineCounter);
				SparkTestUtil util = new SparkTestUtil(defaultPort);
				SparkTestUtil.UrlResponse urlResponse;
				try {
					urlResponse = util.doMethod("POST", "/", lines.toString());
					log.info("Response code {}, body length {}", new Object[]{urlResponse.body.length(), urlResponse.status});
					System.out.println(urlResponse.body);
				} catch (Exception ex) {
					log.error(null, ex);
				}

				System.exit(0);
			}
		}

		TagsService.initialize(defaultDatabase);
		log.info("Tags database ''{}'' opened, {} items found", new Object[]{defaultDatabase, allItems.size()});
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
			log.debug("Response code {}, body length {}", new Object[]{urlResponse.body.length(), urlResponse.status});
			log.info("{} == {} = {}", new Object[]{VERSION, urlResponse.body, VERSION.equals(urlResponse.body)});
			return VERSION.equals(urlResponse.body);
		} catch (Exception ex) {
			log.info("Request failed: {}", ex.getMessage());
		}
		return false;
	}

	private int findFreePort() {
		int number = 6680;
		do {
			try {
				ServerSocket socket = new ServerSocket(number);
				socket.close();
				log.info("Serving on {}", socket.getLocalPort());
				return socket.getLocalPort();
			} catch (IOException ex) {
				log.warn("Port {} seems busy", number);
			}
			number++;
		} while (true);
	}

	private String status(Request request, Response response) {
		StringBuilder result = new StringBuilder();

		int mb = 1024*1024;
		Runtime runtime = Runtime.getRuntime();

		result.append("Items: ").append(allItems.size()).append("\n")
				.append("IP: ").append(request.ip()).append("\n")
				.append("Host: ").append(request.host()).append("\n")
				.append("Port: ").append(request.port()).append("\n")
				.append("\n")
				.append("Search query cache: ").append(TagsService.getInstance().searchCacheSize()).append("\n")
				.append("Threads count: ").append(Thread.getAllStackTraces().size()).append("\n")
				.append("Used memory: ").append((runtime.totalMemory() - runtime.freeMemory()) / mb).append("MB \n")
				.append("Free memory: ").append(runtime.freeMemory() / mb).append("MB \n")
				.append("Total memory: ").append(runtime.totalMemory() / mb).append("MB \n")
				.append("Max memory: ").append(runtime.maxMemory() / mb).append("MB \n")
				
				;
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
