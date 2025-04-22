package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class LocalApplicationServer {

    private int port;

    private static final Path RESOURCES_ROOT = Paths.get("resources");

    private static final HashMap<String, String> MEDIA_TYPES = new HashMap<>();

    private static final HashMap<String, byte[]> FILE_CACHE = new HashMap<>();

    private static final System.Logger logger = System.getLogger(LocalApplicationServer.class.getName());

    static {
        MEDIA_TYPES.put("html", "text/html");
        MEDIA_TYPES.put("css", "text/css");
        MEDIA_TYPES.put("js", "application/javascript");
        for (String file : Arrays.asList("index.html", "script.js", "styles.css")) {
            Path filePath = RESOURCES_ROOT.resolve(file);
            try {
                FILE_CACHE.put(file, Files.readAllBytes(filePath));
                logger.log(System.Logger.Level.INFO, String.format("Successfully read and cached contents of file %s", filePath));
            } catch (IOException exc) {
                logger.log(System.Logger.Level.WARNING, String.format("Could not read contents of file %s", filePath));
                continue;
            }
        }
    }



    public LocalApplicationServer(int port) {
        this.port = port;
    }

    public void launch() throws IOException {
        // Create server on specified port
        HttpServer server = HttpServer.create(new InetSocketAddress(this.port), 0);

        // Simply handle every request coming in
        server.createContext("/", exchange -> {
            String slashlessPath = exchange.getRequestURI().getPath().substring(1);
            logger.log(
                    System.Logger.Level.INFO,
                    String.format("Handling request to %s", exchange.getRequestURI().getPath())
            );

            String fileRequested = slashlessPath.isEmpty() ? "index.html" : slashlessPath;

            try {
                Path requestedFilePath = RESOURCES_ROOT.resolve(fileRequested);
                if (!Files.exists(requestedFilePath)) {
                    sendNotFound(exchange);
                    logger.log(
                            System.Logger.Level.ERROR,
                            String.format("Requested file %s not found", fileRequested)
                    );
                    return;
                }

                byte[] fileContents;
                if (FILE_CACHE.containsKey(fileRequested)) {
                    logger.log(
                            System.Logger.Level.INFO,
                            String.format("Using cached contents of requested file %s", fileRequested)
                    );
                    fileContents = FILE_CACHE.get(fileRequested);
                } else {
                    logger.log(
                            System.Logger.Level.INFO,
                            String.format("No cached contents for file %s found. Reading them now...", fileRequested)
                    );
                    fileContents = Files.readAllBytes(requestedFilePath);
                    logger.log(
                            System.Logger.Level.INFO,
                            String.format("Contents for file %s read", fileRequested)
                    );
                }

                String fileExtension = getFileExtension(fileRequested);
                String mediaType = MEDIA_TYPES.getOrDefault(fileExtension, "text/plain");

                exchange.getResponseHeaders().set("Content-Type", mediaType);
                exchange.sendResponseHeaders(200, fileContents.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(fileContents);
                }
                logger.log(
                        System.Logger.Level.INFO,
                        String.format("Successfully written response body for request to %s", exchange.getRequestURI().getPath())
                );
            } catch (Exception e) {
                logger.log(
                        System.Logger.Level.ERROR,
                        String.format("Request handling to %s failed", exchange.getRequestURI().getPath())
                );
                sendInternalServerError(exchange, e);
            }

        });

        server.start();
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        String notFound = "404 Not Found";
        byte[] notFoundBytes = notFound.getBytes();
        exchange.sendResponseHeaders(404, notFoundBytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(notFoundBytes);
        }
    }

    private void sendInternalServerError(HttpExchange exchange, Exception e) throws IOException {
        String internalServerError = "500 Internal Server Error: " + e.getMessage();
        byte[] internalServerErrorBytes = internalServerError.getBytes();
        exchange.sendResponseHeaders(500, internalServerErrorBytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(internalServerErrorBytes);
        }
    }

    private static String getFileExtension(String fileName) {
        int indexOfLastDot = fileName.lastIndexOf('.');
        return indexOfLastDot != -1 ? fileName.substring(indexOfLastDot + 1) : "";
    }
}
