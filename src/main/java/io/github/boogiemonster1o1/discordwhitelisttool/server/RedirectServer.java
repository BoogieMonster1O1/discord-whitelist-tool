package io.github.boogiemonster1o1.discordwhitelisttool.server;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.boogiemonster1o1.discordwhitelisttool.DiscordWhitelistTool;
import org.jetbrains.annotations.VisibleForTesting;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class RedirectServer {
	public static DisposableServer SERVER;

	public static void init(String host, int port, boolean loop) {
		SERVER = HttpServer.create()
				.host(host)
				.port(port)
				.handle((req, res) -> {
					var query = splitQuery(URI.create(req.uri()));
					var code = query.get("code");
					var state = query.get("state");
					if (code == null || state == null) {
						return res.status(200);
					}
					DiscordWhitelistTool.USER_LIST.addToken(state, code);
					return res.sendString(Mono.just("Authorized. You may now close this window."));
				})
				.bind().block();
		//noinspection ALL
		while (loop) {
		}
	}

	@VisibleForTesting
	public static void main(String[] args) {
		init("localhost", 8080, true);
	}

	public static Map<String, String> splitQuery(URI url) {
		Map<String, String> query_pairs = new LinkedHashMap<>();
		String query = url.getQuery();
		if (query == null) {
			return query_pairs;
		}
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			try {
				query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new UnsupportedOperationException(e);
			}
		}
		return query_pairs;
	}
}
