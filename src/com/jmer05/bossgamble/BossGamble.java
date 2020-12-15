package com.jmer05.bossgamble;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import io.netty.util.internal.ThreadLocalRandom;
import net.milkbowl.vault.economy.Economy;

public class BossGamble extends JavaPlugin implements Listener {

	ConsoleCommandSender console = getServer().getConsoleSender();

	private String GamblerName;
	private Inventory Bar;

	@Override
	public void onEnable() {

		getServer().getPluginManager().registerEvents(this, this);

		this.saveDefaultConfig();
		console.sendMessage(ChatColor.GREEN + "[BossGamble] " + ChatColor.YELLOW + "Enabled");

		GamblerName = getConfig().getString("npc-name").replaceAll("&", "ï¿½");

		setupEconomy();

	}

	@Override
	public void onDisable() {
		console.sendMessage(ChatColor.GREEN + "[BossGamble] " + ChatColor.YELLOW + "Disabled");
	}

	public static Economy eco = null;
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            eco = economyProvider.getProvider();
        }

        return (eco != null);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("gambler")) {

			Player p = null;
			if(sender instanceof Player) {
				p = (Player) sender;
			} else {
				console.sendMessage(ChatColor.GREEN + "Only in-game players can use BossGamble commands");
				return true;
			}

			if(args.length == 0) {
				if(p.hasPermission("BossGamble.admin")) p.sendMessage(ChatColor.DARK_GREEN + "/gambler create " + ChatColor.DARK_AQUA + "<name> " + ChatColor.GREEN + "Create a gambler");
				if(p.hasPermission("BossGamble.admin")) p.sendMessage(ChatColor.DARK_GREEN + "/gambler remove " + ChatColor.DARK_AQUA + "<name> " + ChatColor.GREEN + "Remove a gambler");
				if(p.hasPermission("BossGamble.player")) p.sendMessage(ChatColor.DARK_GREEN + "/gambler open " + ChatColor.GREEN + "Open a bar");
				return false;
			}

			switch(args[0].toLowerCase()) {
				case "create":{

					if(p.hasPermission("BossGamble.admin") == false) {
						p.sendMessage(ChatColor.RED + "You dont have permission to use this command");
						return true;
					}

					if(args.length < 2) {
						p.sendMessage(ChatColor.RED + "Please spicify an id for your gambler");
						return true;
					}

					p.sendMessage(ChatColor.GREEN + "Gambler " + args[1] + " created");

					Evoker evoker = (Evoker) p.getLocation().getWorld().spawn(p.getLocation(), Evoker.class);

					evoker.setCustomName(GamblerName);
					evoker.setAI(false);
					evoker.setCustomNameVisible(true);
					evoker.setSilent(true);
					evoker.setInvulnerable(true);
					evoker.setRemoveWhenFarAway(false);
					evoker.addScoreboardTag("BossGamble.gambler.id" + args[1]);

					Location loc = evoker.getLocation();
					loc.setPitch(p.getLocation().getPitch());
					loc.setYaw(p.getLocation().getYaw());
					evoker.teleport(loc);

					break;
				}

				case "remove":{

					if(p.hasPermission("BossGamble.admin") == false) {
						p.sendMessage(ChatColor.RED + "You dont have permission to use this command");
						return true;
					}

					if(args.length < 2) {
						p.sendMessage(ChatColor.RED + "Please spicify an id for your gambler");
						return true;
					}

					Evoker evoker;

					for(World world : Bukkit.getServer().getWorlds()){
		                for(Entity entity : world.getEntities()){
		                    if(entity.getType() == EntityType.EVOKER) {
		                    	evoker = (Evoker) entity;
		                    	if(evoker.getScoreboardTags().toString().contains("BossGamble.gambler.id" + args[1])) {
		                    		evoker.remove();
		                    		p.sendMessage(ChatColor.GREEN + "Gambler " + args[1] + " removed");
		                    		return true;
		                    	}
		                    }
		                }
					}

					p.sendMessage(ChatColor.GREEN + "Gambler " + args[1] + " not found");
					break;
				}

				case "open":{

					if(p.hasPermission("BossGamble.player") == false) {
						p.sendMessage(ChatColor.RED + "You dont have permission to use this command");
						return true;
					}

					openBar(p);
					return true;
				}

			}

		}
		return false;
    }

	@EventHandler
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		Entity e = event.getRightClicked();
		Player p = event.getPlayer();

		if(e.getType() == EntityType.EVOKER) {

			Evoker evoker = (Evoker) e;
			if(evoker.getScoreboardTags().toString().contains("BossGamble.gambler")) {
				openBar(p);
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		InventoryView inventory = event.getView();
		try {
			if (inventory.getTitle().equals(ChatColor.translateAlternateColorCodes("&".charAt(0), getConfig().getString("gui-title")))) {
				event.setCancelled(true);

				Player p = (Player) event.getWhoClicked();
	        	int index = ((InventoryClickEvent) event).getRawSlot();

				gamble(p, index);

			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	void gamble(Player p, int index) {
		double random = ThreadLocalRandom.current().nextDouble(0, 1);

		List<Map<?, ?>> drinks = getConfig().getMapList("drinks");

		if(index > drinks.size() - 1) return;

		Map<?, ?> drink = drinks.get(index);

		double chance = (double) drink.get("odds");
		int price = (int) drink.get("price");
		int win = (int) getConfig().getDouble("win-multiplier") * price;

		if(eco.getBalance(p) < price) {
			p.sendMessage(ChatColor.translateAlternateColorCodes("&".charAt(0), getConfig().getString("cant-afford-message")));
		} else {

			if(chance > random) {
				eco.depositPlayer(p, win);
				alertPlayer(p, true, win);
			} else {
				eco.withdrawPlayer(p, price);
				alertPlayer(p, false, price);
			}

		}

		new BukkitRunnable(){
		  	@Override
		  	public void run(){
				p.closeInventory();
		  	}
		}.runTaskLater(this, 1);
	}

	private void alertPlayer(Player p, Boolean b, int money) {
		if(b == true) {
			p.sendMessage(ChatColor.translateAlternateColorCodes("&".charAt(0), getConfig().getString("win-message")).replaceAll("%win%", money + ""));
		} else {
			p.sendMessage(ChatColor.translateAlternateColorCodes("&".charAt(0), getConfig().getString("lost-message")));
			if(getConfig().getBoolean("nausea-on-loss")) {
				p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 150, 5));
			}
		}
	}

	@EventHandler
	public boolean onEntityDamage(EntityDamageEvent event) {
		if(event.getEntity().getType() == EntityType.EVOKER) {
			Evoker evoker = (Evoker) event.getEntity();
			if(evoker.getScoreboardTags().toString().contains("BossGamble.gambler")) {
				event.setCancelled(true);
			}
			return true;
		} else {
			return false;
		}
	}

	private void openBar(Player p) {

		List<Map<?, ?>> drinks = getConfig().getMapList("drinks");

		Bar = Bukkit.createInventory(null, (int) Math.ceil((drinks.size() - 1) / 9) * 9 + 9, ChatColor.translateAlternateColorCodes("&".charAt(0), getConfig().getString("gui-title")));

		int index = 0;

		for(Map<?, ?> drink : drinks) {

			String name = (String) drink.get("name");
			String price = new DecimalFormat("#0.00").format((int) drink.get("price"));
			String win = new DecimalFormat("#0.00").format((int) drink.get("price") * getConfig().getDouble("win-multiplier"));
			String odds = new DecimalFormat("#0.##").format((double) drink.get("odds") * 100);
			Color color = Color.fromRGB((int) drink.get("color"));

			ItemStack potion = new ItemStack(Material.POTION);
			PotionMeta meta = (PotionMeta) potion.getItemMeta();

			meta.setColor(color);
			meta.setDisplayName(ChatColor.translateAlternateColorCodes("&".charAt(0), getConfig().getString("drink-name")).replaceAll("%name%", name + ""));
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

			String lore = String.join("\n", getConfig().getStringList("description"));

			lore = lore.replaceAll("&", "§");
			lore = lore.replaceAll("%price%", price);
			lore = lore.replaceAll("%odds%", odds);
			lore = lore.replaceAll("%win%", win);

			meta.setLore(Arrays.asList(lore.split("\n")));

			potion.setItemMeta(meta);

			Bar.setItem(index, potion);
			index ++;

		}

		p.openInventory(Bar);

	}

}
