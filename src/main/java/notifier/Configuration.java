package notifier;


import java.util.Objects;

public class Configuration {
    public final String botToken;
    public final String url;
    public final String chatId;
    public final String dbURL;

    Configuration() {
        this.botToken = System.getenv("BOT_TOKEN");
        this.url = String.format("https://api.telegram.org/bot%s/sendMessage", this.botToken);
        this.chatId = System.getenv("CHAT_ID");
        String dbURL_temp = System.getenv("DATABASE_URL");
        this.dbURL = Objects.requireNonNullElse(dbURL_temp, "database/database.db");
    }
}
