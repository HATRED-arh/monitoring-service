package notifier;

import notifier.interfaces.MessageSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Ping extends MessageSender {
    private final ArrayList<String> ips;
    private final int workers;
    private final Pattern pattern;

    Ping(ArrayList<String> ips, int workers) {
        super(HttpClient.newHttpClient());
        this.ips = ips;
        this.workers = workers;
        this.pattern = Pattern.compile(".*name = (.*)\n");
    }

    public void pingLoop() {
        ExecutorService pool = Executors.newFixedThreadPool(this.workers);
        ArrayList<Callable<Void>> tasks = new ArrayList<>();
        for (String ip : this.ips) {
            tasks.add(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {

                }
                ping(ip);
                return null;
            });
        }
        while (!pool.isShutdown()) {
            try {
                pool.invokeAll(tasks);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void ping(String ip) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String hostname = this.hostname(ip);
        processBuilder.command("ping", "-c1", "-W", "15", ip);
        try {
            Process process = processBuilder.start();
            InputStreamReader inputStream = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(inputStream);
            String text = reader.lines().collect(Collectors.joining());
            boolean is_active = this.isActive(ip);
            boolean contains_error = text.contains("0 packets received");
            if (contains_error && is_active) {
                String message = String.format("\uD83D\uDEA8*ALERT*\uD83D\uDEA8\n`%s`\nIS DOWN\n*IP*: `%s`", hostname, ip);
                this.sendMessage(message);
                this.setState(ip, false);
            }
            if (!contains_error && !is_active) {
                String message = String.format("\uD83C\uDF3F*RELIEF*\uD83C\uDF3F\\n`%s`\nIS UP\n*IP*: `%s`", hostname, ip);
                this.sendMessage(message);
                this.setState(ip, true);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(ip + " : " + hostname);
    }

    private String hostname(String ip) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("nslookup", ip);
        String hostname = "";
        try {
            Process process = processBuilder.start();
            InputStreamReader inputStream = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(inputStream);
            String text = reader.lines().collect(Collectors.joining("\n"));
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                hostname = matcher.group(1);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return hostname;
    }

    private boolean isActive(String ip) {
        String sql = String.format("SELECT active FROM servers WHERE ip = '%s'", ip);
        return App.database.isActive(sql);
    }

    private void setState(String ip, boolean active) {
        String sql = String.format("UPDATE servers SET active = %d WHERE ip = '%s'", Boolean.compare(active, false), ip);
        App.database.executeStatement(sql);
    }
}
