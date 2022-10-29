package notifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Ping {
    private final ArrayList<String> ips;
    private final int workers;
    private final int batch_size;
    private final Pattern pattern;

    Ping(ArrayList<String> ips, int workers, int batch_size) {
        this.ips = ips;
        this.workers = workers;
        this.batch_size = batch_size;
        this.pattern = Pattern.compile(".*name = (.*)\n");
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void pingLoop() {
        ExecutorService pool = Executors.newFixedThreadPool(this.workers);
        List<Callable<Void>> tasks = new ArrayList<>();
        while (true) {
            for (String ip : ips) {
                tasks.add(() -> {
                    Thread.sleep(500);
                    ping(ip);
                    return null;
                });
            }
            if (tasks.size() >= this.batch_size) {
                try {
                    pool.invokeAll(tasks);
                } catch (Exception e) {
                    System.out.print(e.getMessage());
                } finally {
                    tasks.clear();
                }
            }
        }
    }

    private void ping(String ip) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String hostname = this.hostname(ip);
        processBuilder.command("ping", "-c1", "-W", "0.1", ip);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String text = reader.lines().collect(Collectors.joining());
            boolean is_active = this.isActive(ip);
            boolean zero_received = text.contains("0 packets received");
            if (zero_received && is_active) {
                String message = String.format("\uD83D\uDEA8*ALERT*\uD83D\uDEA8\n`%s`\nIS DOWN\n*IP*: `%s`", hostname, ip);
                App.sendMessage(message);
                this.setInactive(ip);
            }
            if (!zero_received && !is_active) {
                String message = String.format("\uD83C\uDF3F*RELIEF*\uD83C\uDF3F\\n`%s`\nIS UP\n*IP*: `%s`", hostname, ip);
                App.sendMessage(message);
                this.setActive(ip);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(hostname);
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
            matcher.find();
            hostname = matcher.group(1);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return hostname;
    }

    private boolean isActive(String ip) {
        String sql = String.format("SELECT active FROM servers WHERE ip = '%s'", ip);
        return App.database.isActive(sql);
    }

    private void setActive(String ip) {
        String sql = String.format("UPDATE servers SET active = 1 WHERE ip = '%s'", ip);
        App.database.executeStatement(sql);
    }

    private void setInactive(String ip) {
        String sql = String.format("UPDATE servers SET active = 0 WHERE ip = '%s'", ip);
        App.database.executeStatement(sql);
    }
}
