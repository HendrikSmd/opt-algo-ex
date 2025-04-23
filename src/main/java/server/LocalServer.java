package server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.logging.Logger;

import static server.ServerUtil.*;

public class LocalServer {

    private final int port;

    private HttpServer server;

    private final HashMap<String, HashMap<HttpMethod, HttpHandler>> contexts = new HashMap<>();

    private static final Logger logger = Logger.getLogger(LocalServer.class.getName());

    public LocalServer(int port) {
        this.port = port;
    }

    public void init() throws IOException {
        // Create server on specified port
        this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
    }

    public boolean launch() {
        if (this.server == null) {
            return false;
        }
        server.start();
        return true;
    }

    public boolean addRequestHandler(String requestUrl, HttpMethod method, HttpHandler handler) {
        if (this.server == null) {
            return false;
        }

        HashMap<HttpMethod, HttpHandler> methodHandlers = null;
        if (this.contexts.containsKey(requestUrl)) {
            methodHandlers = this.contexts.get(requestUrl);
        } else {
            methodHandlers = new HashMap<>();
            this.contexts.put(requestUrl, methodHandlers);

            this.server.createContext(requestUrl, exchange -> {
               HttpMethod httpMethod = HttpMethod.valueOf(exchange.getRequestMethod());
               if (!contexts.containsKey(requestUrl)) {
                   sendNotFound(exchange);
               }

               HashMap<HttpMethod, HttpHandler> handlers = contexts.get(requestUrl);
               if (!handlers.containsKey(httpMethod)) {
                   sendNotFound(exchange);
               }
                try {
                    handlers.get(httpMethod).handle(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange, e);
                }
            });
        }

        if (methodHandlers.containsKey(method)) {
            return false;
        }

        methodHandlers.put(method, handler);
        return true;
    }


}
