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

    private void bulkDelete(ArrayList<String> data, String sql) {
        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (String row : data) {
                pstmt.setString(1, row);
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

    public void updateDomains() {
        ArrayList<String> domains = this.fileLoader("resources/domains.txt");
        String sql = "INSERT INTO domains(domain, active) VALUES(?,?)";
        this.bulkInsert(domains, sql);
        ArrayList<String> toRemove = this.getDomains();
        toRemove.removeAll(domains);
        System.out.println(toRemove);
        this.bulkDelete(toRemove, "DELETE from domains WHERE domain=?");
    }

    public void updateServers() {
        ArrayList<String> servers = this.fileLoader("resources/servers.txt");
        String sql = "INSERT INTO servers(ip, active) VALUES(?,?)";
        this.bulkInsert(servers, sql);
        ArrayList<String> toRemove = this.getIPs();
        toRemove.removeAll(servers);
        System.out.println(toRemove);
        this.bulkDelete(toRemove, "DELETE FROM servers WHERE ip=?");
    }

    public ArrayList<String> getDomains() {
        String sql = "SELECT domain FROM domains";
        return this.getQuery(sql);
    }

    public ArrayList<String> getIPs() {
        String sql = "SELECT ip from servers";
        return this.getQuery(sql);
    }

    public ArrayList<String> getQuery(String sql) {
        ArrayList<String> result = new ArrayList<>();
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return result;
    }


    public void executeStatement(String sql) {
        try (Connection conn = this.connect()) {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean isActive(String sql) {
        boolean res = false;
        try (Connection conn = App.database.connect()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            res = rs.getBoolean("active");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return res;
    }
}
