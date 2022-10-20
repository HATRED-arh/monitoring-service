package notifier;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Healthcheck {
    private final HttpClient client;
    private final ArrayList<String> domains;
    private final int workers;
    private final int batch_size;

    Healthcheck(ArrayList<String> domains, int workers, int batch_size) {
        this.domains = domains;
        this.workers = workers;
        this.batch_size = batch_size;
        this.client = HttpClient.newHttpClient();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void healthcheckLoop() {
        ExecutorService pool = Executors.newFixedThreadPool(this.workers);
        List<Callable<Void>> tasks = new ArrayList<>();
        while (true) {
            for (String domain : this.domains) {
                tasks.add(() -> {
                    Thread.sleep(500);
                    healthcheck(domain);
                    return null;
                });
                if (tasks.size() >= this.batch_size) {
                    try {
                        pool.invokeAll(tasks);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        tasks.clear();
                    }
                }
            }
        }
    }

    private void healthcheck(String domain) {
        URI uri = URI.create(String.format("https://%s/api/healthcheck", domain));
        HttpRequest request = HttpRequest.newBuilder(uri).GET()
                .timeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.printf("%s %s%n", domain, response.statusCode());
        } catch (HttpTimeoutException e) {
            if (this.isActive(domain)) {
                String text = String.format("\uD83D\uDEAB*CAUTION*\uD83D\uDEAB️\n`%s`\nTIMED OUT", domain);
                App.sendMessage(text);
                this.setInactive(domain);
            }
        } catch (ConnectException e) {
            if (this.isActive(domain)) {
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
        if (response.statusCode() == 200 && !this.isActive(domain)) {
            String text = String.format("🌀*操作可能*🌀\n`%s`\nIS UP", domain);
            App.sendMessage(text);
            this.setActive(domain);
        }
        if (response.statusCode() == 500 && this.isActive(domain)) {
            String text = String.format("⚠️*WARNING*⚠️\n`%s`\nDATABASE IS DISCONNECTED", domain);
            App.sendMessage(text);
            this.setInactive(domain);
        }
        if (response.statusCode() == 404 && this.isActive(domain)) {
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
