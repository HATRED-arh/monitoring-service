package notifier;

import notifier.interfaces.MessageSender;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Healthcheck extends MessageSender {
    private final ArrayList<String> domains;
    private final int workers;

    Healthcheck(ArrayList<String> domains, int workers) {
        super(HttpClient.newHttpClient());
        this.domains = domains;
        this.workers = workers;
    }

    public void healthcheckLoop() {
        ExecutorService pool = Executors.newFixedThreadPool(this.workers);
        ArrayList<Callable<Void>> tasks = new ArrayList<>();
        for (String domain : domains) {
            tasks.add(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {

                }
                healthcheck(domain);
                return null;
            });
        }
        while (!pool.isShutdown()) {
            try {
                pool.invokeAll(tasks);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
    }


    private void healthcheck(String domain) {
        URI uri = URI.create(String.format("https://%s/api/healthcheck", domain));
        HttpRequest request = HttpRequest.newBuilder(uri).GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<Void> response = null;
        boolean is_active = this.isActive(domain);
        try {
            response = this.client.send(request, HttpResponse.BodyHandlers.discarding());
            System.out.printf("%s %s%n", domain, response.statusCode());
        } catch (HttpTimeoutException e) {
            if (is_active) {
                String text = String.format("\uD83D\uDEAB*CAUTION*\uD83D\uDEAB️\n`%s`\nTIMED OUT", domain);
                this.sendMessage(text);
                this.setState(domain, false);
            }
        } catch (ConnectException e) {
            if (is_active) {
                String text = String.format("\uD83D\uDEAB*CAUTION*\uD83D\uDEAB️\n`%s`\nCONNECTION ERROR", domain);
                this.sendMessage(text);
                this.setState(domain, false);
            }
        } catch (SSLHandshakeException e) {
            System.err.println("Could not find certificate: " + domain);
            if (is_active) {
                this.setState(domain, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (response == null) {
            return;
        }
        if (response.statusCode() == 200 && !is_active) {
            String text = String.format("🌀*GOOD NEWS*🌀\n`%s`\nIS UP", domain);
            this.sendMessage(text);
            this.setState(domain, true);
        }
        if (response.statusCode() == 500 && is_active) {
            String text = String.format("⚠️*WARNING*⚠️\n`%s`\nDATABASE IS DISCONNECTED", domain);
            this.sendMessage(text);
            this.setState(domain, false);
        }
        if (response.statusCode() == 404 && is_active) {
            String text = String.format("⚠️*WARNING*⚠️\n`%s`\nBACKEND INACCESSIBLE", domain);
            this.sendMessage(text);
            this.setState(domain, false);
        }
    }
    
    private boolean isActive(String domain) {
        String sql = String.format("SELECT active FROM domains WHERE domain = '%s'", domain);
        return App.database.isActive(sql);
    }

    private void setState(String ip, boolean active) {
        String sql = String.format("UPDATE servers SET active = %d WHERE ip = '%s'", Boolean.compare(active, false), ip);
        App.database.executeStatement(sql);
    }
}
