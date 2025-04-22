import server.LocalApplicationServer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        LocalApplicationServer server = new LocalApplicationServer(8080);
        server.launch();
    }

}