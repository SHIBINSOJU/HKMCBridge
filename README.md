# 🌉 HKMCBridge

**HKMCBridge** is a custom Minecraft (Spigot/Paper) plugin developed by **ShotDevs**. It acts as a real-time data bridge, synchronizing player statistics and status from the Minecraft server directly to a MongoDB database.

This plugin powers the **HKMC Leaderboard** website.

## ✨ Features

* **Live Online Status:** Instantly updates `isOnline: true/false` in the database when players join or quit.
* **Real-Time Stats:** Tracks Kills, Deaths, and K/D Ratio automatically.
* **Live Health Sync:** Listens for damage and regen events to save exact player health (Hearts) to the web dashboard instantly.
* **Zero Lag:** All database operations run asynchronously to ensure the server TPS remains perfect.

## 🛠️ Technology Stack

* **Java 17+** (Spigot API)
* **MongoDB** (Data Storage)
* **Maven** (Dependency Management)

## 📦 Installation & Setup

Since this is a custom plugin, you must compile it yourself or download the latest release.

### 1. Prerequisites
* A Minecraft Server (Paper/Spigot 1.16+).
* A MongoDB Database.

### 2. Configuration
Currently, the Database Connection String is located in `Main.java`.
1.  Open `src/main/java/live/shotdevs/hkmcbridge/Main.java`.
2.  Update the `MONGO_URI` variable with your MongoDB connection string.

### 3. Building the Plugin
Run the following command in the root directory:
`mvn clean package`

The compiled .jar file will appear in the target/ folder.
4. Deploying
 * Stop your Minecraft server.
 * Upload HKMCBridge-1.0.jar to the /plugins folder.
 * Start the server.
📊 Database Schema
The plugin creates a collection named players. Here is the data structure it saves:

`{
  "_id": "object_id...",
  "username": "Rizx",
  "kills": 154,
  "deaths": 12,
  "hearts": 9.5,
  "balance": 0,
  "isOnline": true
}`


🚀 Commands & Permissions
This plugin is fully automated. There are no commands or permissions required for players.
 * Stats are tracked automatically for everyone.
 * Admin Logs: The console will show "✅ HKMC Bridge Connected" on startup.
👨‍💻 Developer
Built with ❤️ by ShotDevs
 * Lead Developer: Shibin Hussain MK 
 * Website: shotdevs.live
Private Repository - Do not distribute without permission.
