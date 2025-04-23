package server;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

public class ServerUtil {

    public static void sendNotFound(HttpExchange exchange) throws IOException {
        String notFound = "404 Not Found";
        byte[] notFoundBytes = notFound.getBytes();
        exchange.sendResponseHeaders(404, notFoundBytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(notFoundBytes);
        }
    }

    public static void sendInternalServerError(HttpExchange exchange, Exception e) throws IOException {
        String internalServerError = "500 Internal Server Error: " + e.getMessage();
        byte[] internalServerErrorBytes = internalServerError.getBytes();
        exchange.sendResponseHeaders(500, internalServerErrorBytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(internalServerErrorBytes);
        }
    }

}
