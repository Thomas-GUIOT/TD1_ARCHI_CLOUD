import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.sql.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MainApplication {

    //    volumes:
    //      - thomasg-postgres-data:/var/lib/postgresql/data

    private static final String COUNTER_TABLE_NAME = "counter";
    private static final int COUNTER_USED_COLUMN_ID = 0;
    private static final String POSTGRES_USER = System.getenv("POSTGRES_USER");
    private static final String POSTGRES_PASSWORD = System.getenv("POSTGRES_PASSWORD");
    private static final String POSTGRES_DB = System.getenv("POSTGRES_DB");
    private static final String POSTEGRES_URL = System.getenv("POSTEGRES_URL");

    private static Connection connectionWithDb;
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {

        System.out.println("Server starting...");

        // Chargez le pilote JDBC PostgreSQL
        Class.forName("org.postgresql.Driver");

        connectionWithDb = DriverManager.getConnection("jdbc:postgresql:" + POSTEGRES_URL + "/" + POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD);

        createOrInitTable();


        // Créez un serveur HTTP sur le port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Créez un gestionnaire pour répondre aux requêtes HTTP
        server.createContext("/", new WelcomeHandler());
        server.createContext("/refresh/", new RequestHandler());

        // Démarrez le serveur
        server.start();

        System.out.println("Server successfully started !");
    }

    private static void createOrInitTable() throws SQLException {
        if(tableExists()){
            System.out.println("La table \"" + COUNTER_TABLE_NAME + "\" existe déjà");
        } else {
            System.out.println("La table \"" + COUNTER_TABLE_NAME + "\" n'existe pas");
            // Crée la table si elle n'existe pas
            createTable();

            // Ajoute un enregistrement initial avec id=0 et valeur=0
            insertInitialRecord();
        }
    }

    // Vérifie si une table existe dans la base de données
    private static boolean tableExists() throws SQLException {
        DatabaseMetaData metadata = connectionWithDb.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, COUNTER_TABLE_NAME, null)) {
            return tables.next();
        }
    }

    // Crée la table "compteur" avec les colonnes id et valeur
    private static void createTable() throws SQLException {
        String createTableSQL = "CREATE TABLE " + COUNTER_TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY," +
                "value INT)";
        Statement statement = connectionWithDb.createStatement();
        statement.executeUpdate(createTableSQL);
        statement.close();
        System.out.println("La table 'compteur' a été créée.");
    }

    // Ajoute un enregistrement initial avec id=0 et valeur=0
    private static void insertInitialRecord() throws SQLException {
        String insertSQL = "INSERT INTO " + COUNTER_TABLE_NAME + " (id, valeur) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connectionWithDb.prepareStatement(insertSQL)) {
            preparedStatement.setInt(1, COUNTER_USED_COLUMN_ID);
            preparedStatement.setInt(2, 0);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            System.out.println("Enregistrement initial ajouté à la table 'compteur'");
        }
    }

    private static void incrementCounterRecord() throws SQLException {
        String updateSQL = "UPDATE " + COUNTER_TABLE_NAME + " SET value = value + 1 WHERE id = " + COUNTER_TABLE_NAME;
        Statement statement = connectionWithDb.createStatement();
        statement.executeUpdate(updateSQL);
        statement.close();
        System.out.println("L'enregistrement de la table compteur a été incrementé de 1");
    }

    private static int getCounterRecord() throws SQLException {
        String selectSQL = "SELECT value FROM " + COUNTER_TABLE_NAME + " WHERE id = " + COUNTER_USED_COLUMN_ID;
        Statement statement = connectionWithDb.createStatement();

        return statement.executeQuery(selectSQL).getInt("value");
    }

    static class WelcomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            try {
                incrementCounterRecord();

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
                        "    flex-direction: column;\n" +
                        "  }\n" +
                        "</style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "  <h1>Salut depuis Java!</h1>\n" +
                        "<br>\n" +
                        "<br>\n" +
                        "<h3>Nombre de visite(s) aujourd'hui : <b id='nbVisites'  style='color:red'>" + getCounterRecord() + "</b></h3>\n" +
                        "<br> \n" +
                        "<input type='button' value='Actualiser' style='font-size:20px' id='refreshButton'> \n" +
                        "<script>\n" +
                        "        // Fonction pour effectuer la requête HTTP\n" +
                        "        function refreshValue() {" +
                        "           // Effectue une requête HTTP à l'adresse http://localhost:8080/refresh\n" +
                        "           fetch('http://localhost:8080/refresh')\n" +
                        "               .then(response => response.text()) // Transforme la réponse en texte brute\n" +
                        "               .then(data => {\n" +
                        "                   // Met la valeur dans la balise <b id=\"nbVisite\"></b>\n" +
                        "                   document.getElementById('nbVisites').textContent = data;\n" +
                        "               })\n" +
                        "               .catch(error => {\n" +
                        "                console.error('Une erreur s\\'est produite :', error);\n" +
                        "               });\n" +
                        "       }" +
                        "       // Attachez un gestionnaire d'événements au bouton pour déclencher la requête\n" +
                        "       document.getElementById('refreshButton').addEventListener('click', refreshValue);" +
                        "</script>" +
                        "</body>\n" +
                        "</html>";

                // Définissez le code de réponse HTTP 200 (OK)
                exchange.sendResponseHeaders(200, 0);

                os.write(htmlResponse.getBytes());

                // Fermez le flux de sortie et complétez la réponse
                os.close();

            } catch (SQLException e) {
                exchange.sendResponseHeaders(500, -1);
                System.err.println(e.getStackTrace());            }
        }
    }

    static class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            try {
                String returnValue = getCounterRecord() + "";

                exchange.sendResponseHeaders(200, 0);

                // Obtenez un flux de sortie pour envoyer la réponse
                OutputStream os = exchange.getResponseBody();

                os.write(returnValue.getBytes());

                // Fermez le flux de sortie et complétez la réponse
                os.close();
            } catch (SQLException e) {
                exchange.sendResponseHeaders(500, -1);
                System.err.println(e.getStackTrace());
            }
        }
    }
}
