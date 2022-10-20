package notifier;


public class Configuration {
    public final String botToken;
    public final String url;
    public final String chatId;
    Configuration(){
        this.botToken = System.getenv("BOT_TOKEN");
        this.url = String.format("https://api.telegram.org/bot%s/sendMessage", this.botToken);
        this.chatId = System.getenv("CHAT_ID");
    }
}
