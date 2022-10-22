package notifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class DB {

    DB() {
        File db = new File(App.config.dbURL);
        if (Files.notExists(Paths.get(App.config.dbURL).getParent())) {
            boolean created = db.getParentFile().mkdirs();
            if (!created) {
                throw new RuntimeException("Could not create database.");
            }
        }
    }

    public Connection connect() {
        String url = String.format("jdbc:sqlite:%s", App.config.dbURL);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public void createTables() {
        String backend_table = "CREATE TABLE IF NOT EXISTS domains (id integer PRIMARY KEY, domain text UNIQUE ON CONFLICT IGNORE, active integer);";
        String domain_table = "CREATE TABLE IF NOT EXISTS servers (id integer PRIMARY KEY, ip text UNIQUE ON CONFLICT IGNORE, active integer);";
        try (Connection conn = this.connect()) {
            Statement stmt = conn.createStatement();
            stmt.execute(backend_table);
            stmt.execute(domain_table);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void bulkInsert(ArrayList<String> data, String sql) {
        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (String row : data) {
                pstmt.setString(1, row);
                pstmt.setBoolean(2, true);
                pstmt.addBatch();
            }
            try {
                pstmt.executeBatch();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            conn.commit();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private ArrayList<String> fileLoader(String filename) {
        File file = new File(filename);
        ArrayList<String> data = new ArrayList<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                data.add(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return data;
    }

    public ArrayList<String> updateDomains() {
        ArrayList<String> domains = this.fileLoader("resources/domains.txt");
        String sql = "INSERT INTO domains(domain, active) VALUES(?,?)";
        this.bulkInsert(domains, sql);
        return domains;
    }

    public ArrayList<String> updateServers() {
        ArrayList<String> servers = this.fileLoader("resources/servers.txt");
        String sql = "INSERT INTO servers(ip, active) VALUES(?,?)";
        this.bulkInsert(servers, sql);
        return servers;
    }

    public void executeStatement(String sql) {
        try (Connection conn = this.connect()) {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean isActive(String sql) {
        boolean res = false;
        try (Connection conn = App.database.connect()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            res = rs.getBoolean("active");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return res;
    }
}
