package notifier;

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

public class Healthcheck {
    private final HttpClient client;
    private final ArrayList<String> domains;
    private final int workers;

    Healthcheck(ArrayList<String> domains, int workers) {
        this.domains = domains;
        this.workers = workers;
        this.client = HttpClient.newHttpClient();
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
        URI uri = URI.create(String.format("https://%s/healthcheck", domain));
        HttpRequest request = HttpRequest.newBuilder(uri).GET()
                .timeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<String> response = null;
        boolean is_active = this.isActive(domain);
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.printf("%s %s%n", domain, response.statusCode());
        } catch (HttpTimeoutException e) {
            if (is_active) {
                String text = String.format("\uD83D\uDEAB*CAUTION*\uD83D\uDEAB️\n`%s`\nTIMED OUT", domain);
                App.sendMessage(text);
                this.setInactive(domain);
            }
        } catch (ConnectException e) {
            if (is_active) {
                String text = String.format("\uD83D\uDEAB*CAUTION*\uD83D\uDEAB️\n`%s`\nCONNECTION ERROR", domain);
                App.sendMessage(text);
                this.setInactive(domain);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (response == null) {
            return;
        }
        if (response.statusCode() == 200 && !is_active) {
            String text = String.format("🌀*GOOD NEWS*🌀\n`%s`\nIS UP", domain);
            App.sendMessage(text);
            this.setActive(domain);
        }
        if (response.statusCode() == 500 && is_active) {
            String text = String.format("⚠️*WARNING*⚠️\n`%s`\nDATABASE IS DISCONNECTED", domain);
            App.sendMessage(text);
            this.setInactive(domain);
        }
        if (response.statusCode() == 404 && is_active) {
            String text = String.format("⚠️*WARNING*⚠️\n`%s`\nBACKEND INACCESSIBLE", domain);
            App.sendMessage(text);
            this.setInactive(domain);
        }
    }


    private boolean isActive(String domain) {
        String sql = String.format("SELECT active FROM domains WHERE domain = '%s'", domain);
        return App.database.isActive(sql);
    }

    private void setActive(String domain) {
        String sql = String.format("UPDATE domains SET active = 1 WHERE domain = '%s'", domain);
        App.database.executeStatement(sql);
    }

    private void setInactive(String domain) {
        String sql = String.format("UPDATE domains SET active = 0 WHERE domain = '%s'", domain);
        App.database.executeStatement(sql);
    }
}
