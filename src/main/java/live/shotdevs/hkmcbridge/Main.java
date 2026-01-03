package live.shotdevs.hkmcbridge;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    private MongoClient mongoClient;
    private MongoCollection<Document> collection;

    // YOUR DATABASE URI
    private final String MONGO_URI = "mongodb+srv://shibinhussainmk_db_user:4XZujvl0OnCKhdN5@musicbot.3sydv1a.mongodb.net/?retryWrites=true&w=majority&appName=musicBOT";

    @Override
    public void onEnable() {
        try {
            MongoClientURI clientURI = new MongoClientURI(MONGO_URI);
            mongoClient = new MongoClient(clientURI);
            MongoDatabase database = mongoClient.getDatabase("musicBOT");
            collection = database.getCollection("players");

            // 1. Reset everyone to "Offline" when server starts (in case of crash)
            collection.updateMany(new Document(), Updates.set("isOnline", false));

            // 2. Register Events
            Bukkit.getPluginManager().registerEvents(this, this);
            getLogger().info(ChatColor.GREEN + "✅ HKMC Bridge Connected & Syncing Online Status!");

        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "❌ MongoDB Error: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        // Mark everyone offline when plugin stops
        if (collection != null) {
            collection.updateMany(new Document(), Updates.set("isOnline", false));
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    // EVENT: Player Joins -> Set isOnline = true
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Document doc = collection.find(Filters.eq("username", player.getName())).first();

            if (doc == null) {
                // Create new player
                Document newPlayer = new Document("username", player.getName())
                        .append("kills", 0)
                        .append("deaths", 0)
                        .append("hearts", 10)
                        .append("balance", 0)
                        .append("isOnline", true); // NEW FIELD
                collection.insertOne(newPlayer);
            } else {
                // Update existing player
                collection.updateOne(Filters.eq("username", player.getName()), Updates.set("isOnline", true));
            }
        });
    }

    // EVENT: Player Quits -> Set isOnline = false
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            collection.updateOne(Filters.eq("username", player.getName()), Updates.set("isOnline", false));
        });
    }

    // EVENT: Player Dies (Keep this for stats)
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            collection.updateOne(Filters.eq("username", victim.getName()), Updates.inc("deaths", 1));
            if (killer != null) {
                collection.updateOne(Filters.eq("username", killer.getName()), Updates.inc("kills", 1));
            }
        });
    }
}
