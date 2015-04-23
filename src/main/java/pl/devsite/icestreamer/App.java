package pl.devsite.icestreamer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import spark.Request;
import spark.Response;
import static spark.Spark.*;

public class App {

	Items allItems = new Items();
	int defaultPort = 0;
	String defaultName = "localhost";

	void run() {
		port(defaultPort);

		get("/", (request, response) -> {
			response.redirect("/status");
			return null;
		});

		get("/status", (request, response) -> {
			response.type("text/plain");
			return status(request, response);
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
				Item item = allItems.get(h);
				if (item != null) {
					return item.toString();
				}
			}
			halt(404);
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

			Collection<Item> items = filterAndSort(allItems.items.values(), filter);

			return render(response, request.queryParams("format"), items, host);
		});

		post("/", (request, response) -> {
			String host = queriedHost(request);

			String body = request.body();
			Items result = serve(body);
			System.out.println("Size: " + allItems.items.size());
			return render(response, request.queryParams("format"), result.getList(), host);
		});
	}

	Collection<Item> filterAndSort(Collection<Item> input, String regex) {
		List<Item> items;
		items = new ArrayList(input);
		if (regex != null && !regex.isEmpty()) {
			Pattern pattern = Pattern.compile(regex);
			for (Iterator<Item> it = items.iterator(); it.hasNext();) {
				Item item = it.next();
				if (!item.matches(pattern)) {
					it.remove();
				}
			}
		}

		Collections.sort(items);

		return items;
	}

	Items serve(String lines) {
		Items items = new Items();
		items.feed(parseLines(lines));
		allItems.merge(items);
		return items;
	}

	void init(String[] args) throws IOException {
		int i = 0;
		while (i < args.length) {
			if ("-p".equalsIgnoreCase(args[i]) || "--port".equalsIgnoreCase(args[i])) {
				String pString = args[++i];
				defaultPort = Integer.parseInt(pString);
			}
			if ("-n".equalsIgnoreCase(args[i]) || "--name".equalsIgnoreCase(args[i])) {
				defaultName = args[++i];
			}
			i++;
		}
		if (defaultPort == 0) {
			defaultPort = findFreePort();
		}

		Thread t = new Thread(() -> {
			Scanner sc = new Scanner(System.in);
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				System.out.print(renderList(serve(line).getList(), defaultName + ":" + defaultPort));
				System.out.flush();
			}
		});
		t.setDaemon(true);
		t.start();
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

	String queriedUserAgent(Request request) {
		for (String h : request.headers()) {
			if ("User-Agent".equalsIgnoreCase(h)) {
				return request.headers(h);
			}
		}
		return null;
	}

	String queriedHost(Request request) {
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

	String queriedSearch(Request request) {
		Object querySearch = request.queryParams("s");
		return querySearch != null ? String.valueOf(querySearch) : null;
	}

	Integer parseHashcode(String hash) {
		if (hash.startsWith("h") && hash.length() > 1) {
			hash = hash.substring(1);
			return Integer.parseUnsignedInt(hash, 16);
		}
		return null;
	}

	String[] parseLines(String body) {
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

	private int findFreePort() {
		int number = 6680;
		do {
			try {
				ServerSocket socket = new ServerSocket(number);
				socket.close();
				System.out.println("Serving on " + socket.getLocalPort());
				return socket.getLocalPort();
			} catch (IOException ex) {
				System.out.println("Port " + number + " seems busy");
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

	String render(Response response, String format, Collection<Item> list, String host) {
		if ("m3u".equalsIgnoreCase(format)) {
			response.type("audio/x-mpegurl");
			return renderM3U(list, host);
		} else if ("names".equalsIgnoreCase(format)) {
			response.type("text/plain");
			return renderItemStrings(list, host);
		} else {
			response.type("text/plain");
			return renderList(list, host);
		}
	}

	private String renderItemStrings(Collection<Item> list, String host) {
		StringBuilder result = new StringBuilder();
		list.stream().forEach((i) -> {
			result.append(i.toString()).append("\n");
		});
		return result.toString();
	}

	private String renderList(Collection<Item> list, String host) {
		StringBuilder result = new StringBuilder();
		list.stream().forEach((i) -> {
			result.append("http://").append(host).append("/stream/");
			result.append("h").append(Integer.toHexString(i.hashCode())).append("\n");
		});
		return result.toString();
	}

	private String renderM3U(Collection<Item> list, String host) {
		StringBuilder result = new StringBuilder();
		result.append("#EXTM3U").append("\n");
		list.stream().forEach((i) -> {
			result.append("#EXTINF:").append("-1").append(", ").append(i.toString()).append("\n");
			result.append("http://").append(host).append("/stream/");
			result.append("h").append(Integer.toHexString(i.hashCode())).append("\n");
		});
		return result.toString();
	}
}
