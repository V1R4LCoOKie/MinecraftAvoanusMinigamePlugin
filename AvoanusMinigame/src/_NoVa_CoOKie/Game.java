package _NoVa_CoOKie;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

/**
 * @author _NoVa_CoOKie
 */
public class Game {

	private class Enemy implements Comparable<Enemy> {
		String summon;
		int difficulty;
		
		public Enemy(String summon, int difficulty) {
			this.summon = summon;
			this.difficulty = difficulty;
		}
		
		@Override
		public int compareTo(Enemy other) {
			return difficulty - other.difficulty;
		}
	}
	
/*	private class PlayerWithScore implements Comparable<PlayerWithScore> {

		Player player;
		public int score = 0;
		
		public PlayerWithScore(Player p) {
			player = p;
		}
		
		@Override
		public int compareTo(PlayerWithScore other) {
			return other.score - score;
		}
	}*/
	
	Plugin mainPlugin;
	
	// Customizable Settings
	private int minDifficulty;
	private int maxDifficulty;
	private int difficultyRate;
	//private int maxEnemies;
	private int maxSpawnPerTry;
	private long spawnFrequency; // in ticks
	private int minWaveSize;
	private int maxWaveSize;
	/*private int pointsPerPlayerKill;
	private int pointsPerKill;
	private int pointsLostPerDeath;*/
	private Location[] spawns;
	private Enemy[] enemyPool;
	
	// Current game state
	private int numEnemies = 0;
	private int numEnemiesLeftToSpawn;
	private int waveNumber = 0;
	private int difficulty = minDifficulty;
//	private PlayerWithScore[] players;
	private Player[] players;
	private int numLivingPlayers;
	private Player[] livingPlayers;
	private Entity[] enemies;
	
	Random randomGen;
	
	public Game(Plugin plugin, ConfigurationSection config, Player[] players) {
		minDifficulty = config.getInt("minDifficulty");
		maxDifficulty = config.getInt("maxDifficulty");
		difficultyRate = config.getInt("difficultyRate");
		/*maxEnemies = config.getInt("maxEnemies");
		enemies = new Entity[maxEnemies];*/
		maxSpawnPerTry = config.getInt("maxSpawnPerTry");
		spawnFrequency = config.getLong("spawnFrequency");
		minWaveSize = config.getInt("minWaveSize");
		maxWaveSize = config.getInt("maxWaveSize");
		enemies = new Entity[maxWaveSize];
		/*pointsPerPlayerKill = config.getInt("pointsPerPlayerKill");
		pointsPerKill = config.getInt("pointsPerKill");
		pointsLostPerDeath = config.getInt("pointsLostPerDeath");*/
		spawns = new Location[config.getInt("numSpawns")];
		for (int i = 0; i < config.getInt("numSpawns"); ++i)
			spawns[i] = config.getLocation("Spawn" + (i + 1));
		/*this.players = new PlayerWithScore[players.length];
		for (int i = 0; i < players.length; ++i)
			this.players[i] = new PlayerWithScore(players[i]);*/
		this.players = new Player[players.length];
		numLivingPlayers = players.length;
		livingPlayers = new Player[players.length];
		for (int i = 0; i < players.length; ++i) {
			this.players[i] = players[i];
			livingPlayers[i] = players[i];
		}
		enemyPool = new Enemy[config.getInt("enemyPoolSize")];
		for (int i = 1; i < config.getInt("enemyPoolSize") + 1; ++i)
			enemyPool[i - 1] = new Enemy(config.getString("Enemy" + i + ".summon"), 
									 config.getInt("Enemy" + i + ".difficulty"));
		java.util.Arrays.sort(enemyPool);
		randomGen = new Random();
		newWave();
		mainPlugin = plugin;
	}
	
	public long getWaitTime() {return spawnFrequency;}
	
	public void newWave() {
		numEnemiesLeftToSpawn = randomGen.nextInt(maxWaveSize - minWaveSize) + minWaveSize;
		++waveNumber;
		for (Player p : players)
			p.sendMessage(ChatColor.DARK_GRAY + "WAVE " + waveNumber + "! " + numEnemiesLeftToSpawn + " enemies incoming...");
		difficulty += difficultyRate;
	}
	
	public int spawnEnemies(CommandSender sender) {
		int x = randomGen.nextInt(maxSpawnPerTry) + 1;
		x = numEnemiesLeftToSpawn - x < 0 ? numEnemiesLeftToSpawn : x;
		x = x > spawns.length ? spawns.length : x;
		
		Location[] tempSpawns = new Location[spawns.length];
		for (int i = 0; i < tempSpawns.length; ++i)
			tempSpawns[i] = spawns[i];
		
		for (int i = 0; i < x; ++i) {
			int spawnIndex = randomGen.nextInt(spawns.length - i);
			Location spawn = tempSpawns[spawnIndex];
			for (int j = spawnIndex; j < spawns.length - i - 1; ++j)
				tempSpawns[j] = tempSpawns[j + 1];
			Bukkit.dispatchCommand(sender, randomEnemy().summon.replaceFirst("~", String.valueOf(spawn.getX()))
																.replaceFirst("~", String.valueOf(spawn.getY()))
					   											.replaceFirst("~", String.valueOf(spawn.getZ())));
			for (Entity ent : spawn.getChunk().getEntities())
				if (ent.getLocation().distance(spawn) == 0) {
					boolean flag = false;
					for (Entity knownEnemy : enemies)
						if (knownEnemy == ent) {
							flag = true;
							break;
						}
					if (flag) continue;
					enemies[numEnemies++] = ent;
					--numEnemiesLeftToSpawn;
					break;
				}
		}
		return x;
	}
	
	private Enemy randomEnemy() {
		int i = 0;
		while (i < enemyPool.length && enemyPool[i].difficulty <= difficulty) ++i;
		--i;
		if (i == 0) return enemyPool[0];
		int x = randomGen.nextInt(i*4);
		if (x > i)
			return enemyPool[i];
		return enemyPool[x];
	}

	public void notifyOfDeath(EntityDeathEvent e) {
		if (e.getEntityType() == EntityType.PLAYER) {
			//for (PlayerWithScore p : players) {
			for (Player p : players) {
				/*if ((Player) e.getEntity().getKiller() == p) {
					//for (PlayerWithScore otherp : players)
					for (Player otherp : players)
						otherp.sendMessage(ChatColor.RED + p.getName() + " KILLED " + e.getEntity().getName() + "!");// + ChatColor.GOLD + " +" + pointsPerPlayerKill);
					//p.score += pointsPerPlayerKill;
				}*/
				if ((Player) e.getEntity() == p) {
					//for (PlayerWithScore otherp : players)
					for (Player otherp : players)
						otherp.sendMessage(ChatColor.RED + e.getEntity().getName() + " DIED.");//  -" + pointsLostPerDeath);
					for (int i = 0; i < numLivingPlayers; ++i)
						if (livingPlayers[i] == e.getEntity()) {
							Player temp = livingPlayers[i];
							for (int j = i; j < numLivingPlayers - 1; ++j)
								livingPlayers[j] = livingPlayers[j + 1];
							livingPlayers[numLivingPlayers - 1] = temp;
							--numLivingPlayers;
							if (numLivingPlayers <= 1) {
								mainPlugin.getServer().dispatchCommand(mainPlugin.getServer().getConsoleSender(), "stopgame");
							}
						}
					//p.score -= pointsLostPerDeath;
				}
			}
		}
		else if (e.getEntity().getKiller() == null) {
			for (int i = 0; i < numEnemies; ++i)
				if (enemies[i] == e.getEntity()) {
					enemies[i] = null;
					for (int j = i; j < numEnemies - 1; ++j)
						enemies[j] = enemies[j + 1];
					--numEnemies;
					if (numEnemies == 0) {
						newWave();
						difficulty += difficultyRate;
						difficulty = difficulty > maxDifficulty ? maxDifficulty : difficulty;
					}
				}
		}
		else {
			for (int i = 0; i < numEnemies; ++i)
				if (enemies[i] == e.getEntity()) {
					//for (PlayerWithScore p : players) {
					for (Player p : players) {
						p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + e.getEntity().getKiller().getName() + ChatColor.WHITE + " KILLED " + ChatColor.RED + e.getEntity().getName() + ChatColor.RESET + "!");// + ChatColor.GOLD + " +" + pointsPerKill);
						//if (p.getName().equalsIgnoreCase(e.getEntity().getKiller().getName()))
							//p.score += pointsPerKill;
					}
					enemies[i] = null;
					for (int j = i; j < numEnemies - 1; ++j)
						enemies[j] = enemies[j + 1];
					--numEnemies;
					if (numEnemies == 0) {
						newWave();
						difficulty += difficultyRate;
						difficulty = difficulty > maxDifficulty ? maxDifficulty : difficulty;
					}
				}
		}
		return;
	}

	public void sendWinner() {
		for (Player p : players) {
			p.sendMessage(ChatColor.GOLD + "1st: " + ChatColor.BOLD + livingPlayers[0].getName());
			if (livingPlayers.length > 1) p.sendMessage(ChatColor.GRAY + "2nd: " + ChatColor.BOLD + livingPlayers[1].getName());
			if (livingPlayers.length > 2) p.sendMessage(ChatColor.DARK_AQUA + "3rd: " + ChatColor.BOLD + livingPlayers[2].getName());
		}
	}
	
	/*
	public void sendScores() {
		java.util.Arrays.sort(players);
		for (PlayerWithScore p : players) {
			p.player.sendMessage(ChatColor.GOLD + "1st:" + ChatColor.BOLD + players[0].player.getName() + ": " + players[0].score);
			if (players.length > 1) p.player.sendMessage(ChatColor.GRAY + "2nd" + ChatColor.BOLD + players[1].player.getName() + ": " + players[1].score);
			if (players.length > 2) p.player.sendMessage(ChatColor.DARK_AQUA + "3rd" + ChatColor.BOLD + players[2].player.getName() + ": " + players[2].score);
		}
	}*/
}