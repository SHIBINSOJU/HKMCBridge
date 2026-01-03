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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
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

            // Reset everyone to "Offline" when server starts
            collection.updateMany(new Document(), Updates.set("isOnline", false));

            Bukkit.getPluginManager().registerEvents(this, this);
            getLogger().info(ChatColor.GREEN + "✅ HKMC Bridge Connected & Syncing Real-Time Stats!");

        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "❌ MongoDB Error: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (collection != null) {
            collection.updateMany(new Document(), Updates.set("isOnline", false));
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    // --- EVENTS ---

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Document doc = collection.find(Filters.eq("username", player.getName())).first();
            
            // Get current health (formatted to 1 decimal place)
            double currentHealth = Math.round(player.getHealth() * 10.0) / 10.0;

            if (doc == null) {
                Document newPlayer = new Document("username", player.getName())
                        .append("kills", 0)
                        .append("deaths", 0)
                        .append("hearts", currentHealth) // Save actual health
                        .append("balance", 0)
                        .append("isOnline", true);
                collection.insertOne(newPlayer);
            } else {
                collection.updateOne(Filters.eq("username", player.getName()), 
                    Updates.combine(
                        Updates.set("isOnline", true),
                        Updates.set("hearts", currentHealth) // Update health on join
                    ));
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            collection.updateOne(Filters.eq("username", player.getName()), Updates.set("isOnline", false));
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // Set victim hearts to 0
            collection.updateOne(Filters.eq("username", victim.getName()), 
                Updates.combine(
                    Updates.inc("deaths", 1),
                    Updates.set("hearts", 0.0)
                ));

            if (killer != null) {
                collection.updateOne(Filters.eq("username", killer.getName()), Updates.inc("kills", 1));
            }
        });
    }

    // NEW: Update Database when player takes DAMAGE
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            
            // Calculate what health WILL be after the hit
            double newHealth = player.getHealth() - event.getFinalDamage();
            if (newHealth < 0) newHealth = 0;

            final double saveHealth = Math.round(newHealth * 10.0) / 10.0;

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                collection.updateOne(Filters.eq("username", player.getName()), 
                        Updates.set("hearts", saveHealth));
            });
        }
    }

    // NEW: Update Database when player HEALS
    @EventHandler
    public void onHeal(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            double newHealth = player.getHealth() + event.getAmount();
            if (newHealth > player.getMaxHealth()) newHealth = player.getMaxHealth();

            final double saveHealth = Math.round(newHealth * 10.0) / 10.0;

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                collection.updateOne(Filters.eq("username", player.getName()), 
                        Updates.set("hearts", saveHealth));
            });
        }
    }
}
