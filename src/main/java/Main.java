import server.HttpMethod;
import server.LocalServer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            ApplicationAPI.run(8080);
            logger.log(Level.INFO, "API up and running");
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not run application API");
            System.exit(-1);
        }
    }

}