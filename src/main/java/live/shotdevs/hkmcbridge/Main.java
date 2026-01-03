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
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    private MongoClient mongoClient;
    private MongoCollection<Document> collection;

    // YOUR DATABASE URI (I copied this from your server.js)
    private final String MONGO_URI = "mongodb+srv://shibinhussainmk_db_user:4XZujvl0OnCKhdN5@musicbot.3sydv1a.mongodb.net/?retryWrites=true&w=majority&appName=musicBOT";

    @Override
    public void onEnable() {
        try {
            // 1. Connect to MongoDB (Async to avoid lag, but Sync for startup is fine)
            MongoClientURI clientURI = new MongoClientURI(MONGO_URI);
            mongoClient = new MongoClient(clientURI);
            
            // 2. Select Database & Collection
            MongoDatabase database = mongoClient.getDatabase("musicBOT"); // DB Name
            collection = database.getCollection("players"); // Collection Name

            // 3. Register Events
            Bukkit.getPluginManager().registerEvents(this, this);
            
            getLogger().info(ChatColor.GREEN + "✅ HKMC Bridge Connected to MongoDB!");
            
        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "❌ Could not connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (mongoClient != null) {
            mongoClient.close();
            getLogger().info("MongoDB Connection Closed.");
        }
    }

    // EVENT: When a player joins, create their profile if it doesn't exist
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Run database check Asynchronously (So server doesn't freeze)
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Document doc = collection.find(Filters.eq("username", player.getName())).first();

                if (doc == null) {
                    // Create new player entry
                    Document newPlayer = new Document("username", player.getName())
                            .append("kills", 0)
                            .append("deaths", 0)
                            .append("hearts", 10) // Default Lifesteal Hearts
                            .append("balance", 0);
                    collection.insertOne(newPlayer);
                    getLogger().info("Created database entry for " + player.getName());
                }
            } catch (Exception e) {
                getLogger().warning("Failed to save player data: " + e.getMessage());
            }
        });
    }

    // EVENT: When a player dies
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Run updates Asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // 1. Add 1 Death to Victim
                collection.updateOne(Filters.eq("username", victim.getName()), Updates.inc("deaths", 1));

                // 2. If there was a killer, Add 1 Kill to Killer
                if (killer != null) {
                    collection.updateOne(Filters.eq("username", killer.getName()), Updates.inc("kills", 1));
                    // Optional: You could sync Hearts here too if you have a way to read them
                }
            } catch (Exception e) {
                getLogger().warning("Failed to update stats on death: " + e.getMessage());
            }
        });
    }
}
