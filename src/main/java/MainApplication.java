import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MainApplication {

    public static void main(String[] args) throws IOException {

        System.out.println("Server starting...");

        // Créez un serveur HTTP sur le port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Créez un gestionnaire pour répondre aux requêtes HTTP
        server.createContext("/", new MyHandler());

        // Démarrez le serveur
        server.start();

        System.out.println("Server successfully started !");
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Définissez le code de réponse HTTP 200 (OK)
            exchange.sendResponseHeaders(200, 0);

            // Obtenez un flux de sortie pour envoyer la réponse
            OutputStream os = exchange.getResponseBody();

            // Écrivez votre page HTML dans le flux de sortie
            String htmlResponse = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<style>\n" +
                    "  /* CSS pour centrer le texte au milieu de la page */\n" +
                    "  body {\n" +
                    "    display: flex;\n" +
                    "    justify-content: center;\n" +
                    "    align-items: center;\n" +
                    "  }\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <h1>Salut depuis Java!</h1>\n" +
                    "</body>\n" +
                    "</html>    ";
            os.write(htmlResponse.getBytes());

            // Fermez le flux de sortie et complétez la réponse
            os.close();
        }
    }
}