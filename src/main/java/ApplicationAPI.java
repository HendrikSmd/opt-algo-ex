import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.HttpMethod;
import server.LocalServer;
import server.ServerUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationAPI {

    private static final Path RESOURCES_ROOT = Paths.get("src/main/webapp");

    private static final HashMap<String, String> MEDIA_TYPES = new HashMap<>();

    private static final HashMap<String, byte[]> FILE_CACHE = new HashMap<>();

    private static final Logger logger = Logger.getLogger(LocalServer.class.getName());

    static {
        MEDIA_TYPES.put("html", "text/html");
        MEDIA_TYPES.put("css", "text/css");
        MEDIA_TYPES.put("js", "application/javascript");
        for (String file : Arrays.asList("index.html", "script.js", "styles.css")) {
            Path filePath = RESOURCES_ROOT.resolve(file);
            try {
                FILE_CACHE.put(file, Files.readAllBytes(filePath));
                logger.log(Level.INFO, String.format("Successfully read and cached contents of file %s", filePath));
            } catch (IOException exc) {
                logger.log(Level.WARNING, String.format("Could not read contents of file %s", filePath));
            }
        }
    }

    public static void run(int port) throws IOException {
        LocalServer server = new LocalServer(port);
        server.init();
        server.addRequestHandler("/", HttpMethod.GET, returnResourceFileHandler("index.html"));
        server.addRequestHandler("/script.js", HttpMethod.GET, returnResourceFileHandler("script.js"));
        server.addRequestHandler("/styles.css", HttpMethod.GET, returnResourceFileHandler("styles.css"));
        server.launch();
    }

    private static Optional<byte[]> readResourceFile(String fileName) throws IOException {
        Path requestedFilePath = RESOURCES_ROOT.resolve(fileName);
        if (!Files.exists(requestedFilePath)) {
            return Optional.empty();
        }

        if (FILE_CACHE.containsKey(fileName)) {
            logger.log(Level.INFO, String.format("Found requested file %s in cache. Returning cached contents", requestedFilePath));
            return Optional.of(FILE_CACHE.get(fileName));
        }


        byte[] fileContents = Files.readAllBytes(requestedFilePath);
        return Optional.of(fileContents);
    }

    private static HttpHandler returnResourceFileHandler(String fileName) {
        return exchange -> {
            try {
                Optional<byte[]> fileContentsOpt = readResourceFile(fileName);
                if (!fileContentsOpt.isPresent()) {
                    ServerUtil.sendNotFound(exchange);
                    return;
                }
                byte[] fileContents = fileContentsOpt.get();

                String fileExtension = getFileExtension(fileName);
                exchange.getResponseHeaders().set("Content-Type", MEDIA_TYPES.getOrDefault(fileExtension, "text/plain"));
                exchange.sendResponseHeaders(200, fileContents.length);

                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(fileContents);
                }
            } catch (Exception exc) {
                ServerUtil.sendInternalServerError(exchange, exc);
            }
        };
    }

    private static String getFileExtension(String fileName) {
        int indexOfLastDot = fileName.lastIndexOf('.');
        return indexOfLastDot != -1 ? fileName.substring(indexOfLastDot + 1) : "";
    }


}
