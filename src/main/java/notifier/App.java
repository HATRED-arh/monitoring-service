/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package notifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;


public class App {
    public static final DB database = new DB("database/database.db");
    public static final Configuration config = new Configuration();
    public static final HttpClient client = HttpClient.newHttpClient();
    public static void sendMessage(String text) {
        String body = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\", \"parse_mode\": \"MarkdownV2\"}", App.config.chatId, text);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(App.config.url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    public static void main(String[] args) {
        database.createTables();
        ArrayList<String> domains = database.updateDomains();
        ArrayList<String> ips = database.updateServers();
        Healthcheck healthcheck = new Healthcheck(domains, 4, 40);
        Ping ping = new Ping(ips, 4, 40);
        Thread healthcheck_thread = new Thread(healthcheck::healthcheckLoop);
        Thread pingonator_thread = new Thread(ping::pingLoop);
        healthcheck_thread.start();
        pingonator_thread.start();
    }
}