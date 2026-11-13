package ug.dankk;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

public final class App {
    private static final URI DASHBOARD = URI.create("http://127.0.0.1:8765/");

    private App() {
    }

    public static void main(String[] args) {
        try {
            RecyclingDatabase database = new RecyclingDatabase();
            database.initialize();
            RecyclingHttpServer server = new RecyclingHttpServer(database);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown-server"));

            System.out.println("DAN KK Recycling Centre is running at " + DASHBOARD);
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(DASHBOARD);
                } else {
                    System.out.println("Open this address in your browser: " + DASHBOARD);
                }
            } catch (Exception browserError) {
                System.out.println("Could not open a browser automatically. Open: " + DASHBOARD);
            }
            new CountDownLatch(1).await();
        } catch (Exception exception) {
            System.err.println("The recycling system could not start: " + exception.getMessage());
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
