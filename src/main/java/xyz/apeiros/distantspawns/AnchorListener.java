package xyz.apeiros.distantspawns;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.jeff_media.customblockdata.CustomBlockData;

public class AnchorListener implements Listener {
	// Used for storing the last used anchor location in player PDC
	private final NamespacedKey LOCATION_KEY;
	// Used for storing capacity in anchor PDC and CustomBlockData
	private final NamespacedKey CAPACITY_KEY;
	// Used for storing unique ID in anchor PDC to make them unstackable
	private final NamespacedKey ID_KEY;

	public AnchorListener(DistantSpawns plugin) {
		LOCATION_KEY = new NamespacedKey(plugin, "anchor_location");
		CAPACITY_KEY = new NamespacedKey(plugin, "anchor_capacity");
		ID_KEY = new NamespacedKey(plugin, "anchor_id");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	// Preview data in the crafting screen
	@EventHandler(ignoreCancelled = true)
	public void beforeCraft(PrepareItemCraftEvent evt) {
		ItemStack item = evt.getInventory().getResult();
		if (item == null) return;
		if (item.getType() != Material.RESPAWN_ANCHOR) return;
		if (isCustomItem(item)) return;

		addAnchorData(item, 4);
		evt.getInventory().setResult(item);
	}

	// Add data to actual crafted anchors
	@EventHandler(ignoreCancelled = true)
	public void onCraft(CraftItemEvent evt) {
		ItemStack result = evt.getInventory().getResult();
		if (result == null || result.getType() != Material.RESPAWN_ANCHOR) {
			return;
		}

		// Handle the first resulting item
		ItemStack newResult = result.clone();
		newResult.setAmount(1);
		addAnchorData(newResult, 4);
		evt.getInventory().setResult(newResult);

		// Handle the rest (if any)
		int amt = result.getAmount();
		if (amt > 1) {
			HumanEntity player = evt.getWhoClicked();
			for (int i = 1; i < amt; i++) {
				ItemStack extra = new ItemStack(Material.RESPAWN_ANCHOR);
				addAnchorData(extra, 4);
				player.getInventory().addItem(extra)
					.forEach((slot, item) ->
						// Drop extra items that don't fit
						player.getWorld().dropItem(player.getLocation(), item)
					);
			}
		}
	}

	// Add data to picked up anchors
	@EventHandler(ignoreCancelled = true)
	public void onPickup(EntityPickupItemEvent evt) {
		ItemStack item = evt.getItem().getItemStack();
		if (item.getType() != Material.RESPAWN_ANCHOR) return;
		if (isCustomItem(item)) return;

		addAnchorData(item, 4);
		evt.getItem().setItemStack(item);
	}

	// Add data to clicked anchors (in the inventory)
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent evt) {
		ItemStack item = evt.getCurrentItem();
		if (item == null) return;
		if (item.getType() != Material.RESPAWN_ANCHOR) return;
		if (isCustomItem(item)) return;

		addAnchorData(item, 4);
		evt.setCurrentItem(item);
	}

	// Convert item data to block data when placing an anchor
	@EventHandler(ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent evt) {
		ItemStack item = evt.getItemInHand();
		if (item.getType() != Material.RESPAWN_ANCHOR) return;

		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		Integer capacity = pdc.get(CAPACITY_KEY, PersistentDataType.INTEGER);
		if (capacity == null) return;

		CustomBlockData data = DistantSpawns.getDataFor(evt.getBlock());
		data.set(CAPACITY_KEY, PersistentDataType.INTEGER, capacity);
	}

	// Convert block data to item data when breaking an anchor
	@EventHandler(ignoreCancelled = true)
	public void onBreak(BlockBreakEvent evt) {
		Block block = evt.getBlock();
		if (block.getType() != Material.RESPAWN_ANCHOR) return;
		if (evt.getPlayer().getGameMode() == GameMode.CREATIVE) return;

		CustomBlockData data = DistantSpawns.getDataFor(evt.getBlock());
		Integer capacity = data.get(CAPACITY_KEY, PersistentDataType.INTEGER);
		if (capacity == null) {
			capacity = 4;
		}
		data.remove(CAPACITY_KEY);

		// Drop our own item with data rather than the default
		evt.setDropItems(false);
		ItemStack result = new ItemStack(Material.RESPAWN_ANCHOR);
		addAnchorData(result, capacity);
		block.getWorld().dropItemNaturally(block.getLocation(), result);
	}

	// Disallow charging past the capacity by hand
	@EventHandler(ignoreCancelled = true)
	public void onManualCharge(PlayerInteractEvent evt) {
		if (evt.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		ItemStack held = evt.getItem();
		if (held == null) return;
		if (held.getType() != Material.GLOWSTONE) return;

		Block block = evt.getClickedBlock();
		if (block == null) return;
		if (block.getType() != Material.RESPAWN_ANCHOR) return;

		CustomBlockData data = DistantSpawns.getDataFor(block);
		Integer capacity = data.get(CAPACITY_KEY, PersistentDataType.INTEGER);
		if (capacity == null) {
			data.set(CAPACITY_KEY, PersistentDataType.INTEGER, 4);
			capacity = 4;
		}

		RespawnAnchor anchor = (RespawnAnchor) block.getBlockData();
		if (anchor.getCharges() >= capacity) {
			// No more space left, notify the player and cancel the charging
			evt.getPlayer().sendMessage(ConfigManager.getAnchorFullMsg(capacity));
			evt.setCancelled(true);
		}
	}

	// Disallow charging past the capacity by dispensers
	@EventHandler(ignoreCancelled = true)
	public void onDispenserCharge(BlockDispenseEvent evt) {
		if (evt.getBlock().getType() != Material.DISPENSER) return;
		if (evt.getItem().getType() != Material.GLOWSTONE) return;

		Vector vel = evt.getVelocity();
		if (!isPosition(vel)) return;

		World w = evt.getBlock().getWorld();
		Location loc = new Location(w, vel.getX(), vel.getY(), vel.getZ());
		Block block = w.getBlockAt(loc);

		if (block.getType() != Material.RESPAWN_ANCHOR) return;

		CustomBlockData data = DistantSpawns.getDataFor(block);
		Integer capacity = data.get(CAPACITY_KEY, PersistentDataType.INTEGER);
		if (capacity == null) {
			data.set(CAPACITY_KEY, PersistentDataType.INTEGER, 4);
			capacity = 4;
		}

		RespawnAnchor anchor = (RespawnAnchor) block.getBlockData();
		if (anchor.getCharges() >= capacity) {
			evt.setCancelled(true);
		}
	}

	// The BlockDispenseEvent's "velocity" is actually set to integer coordinates
	// if the dispenser is interacting with a block rather than dropping an item
	// This method checks if the "velocity" is a position, or really velocity
	private static boolean isPosition(Vector vec) {
		double x = vec.getX();
		double y = vec.getY();
		double z = vec.getZ();
		// Int casting is fine, because the maximum possible coordinates are around
		// +/- 30 million, which is nowhere near +/- 2.147 billion
		return (int) x == x && (int) y == y && (int) z == z;
	}

	// Link the location of a respawn anchor to the player when they use it.
	// Needed because PlayerRespawnEvent#getRespawnLocation() returns the player's
	// location rather than the anchor's location
	@EventHandler(ignoreCancelled = true)
	public void onSetSpawn(PlayerSpawnChangeEvent evt) {
		PersistentDataContainer pdc = evt.getPlayer().getPersistentDataContainer();
		if (evt.getCause() != PlayerSpawnChangeEvent.Cause.RESPAWN_ANCHOR) {
			pdc.remove(LOCATION_KEY);
			return;
		}
		Location loc = evt.getNewSpawn();
		if (loc == null) return;
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		// Store the location of the spawn point (the anchor, not the block at which
		// the player will appear) for later use
		pdc.set(LOCATION_KEY, PersistentDataType.INTEGER_ARRAY, new int[] {x, y, z});
	}

	// Reduce capacity of the player's linked anchor when they respawn
	@EventHandler(ignoreCancelled = true)
	public void onAnchorRespawn(PlayerRespawnEvent evt) {
		if (evt.getRespawnReason() != RespawnReason.DEATH) return;

		Player p = evt.getPlayer();
		PersistentDataContainer pdc = p.getPersistentDataContainer();
		int[] pos = pdc.get(LOCATION_KEY, PersistentDataType.INTEGER_ARRAY);
		// Player doesn't have a stored anchor location, don't handle
		if (pos == null) return;

		World w = p.getWorld();
		Location loc = new Location(w, pos[0], pos[1], pos[2]);
		Block block = w.getBlockAt(loc);
		if (block.getType() != Material.RESPAWN_ANCHOR) {
			// Spawn block isn't an anchor, don't handle
			// Somehow, they got a stored location without an anchor
			// Should remove the location value
			pdc.remove(LOCATION_KEY);
			return;
		}

		CustomBlockData data = DistantSpawns.getDataFor(block);
		Integer capacity = data.get(CAPACITY_KEY, PersistentDataType.INTEGER);
		if (capacity == null) {
			capacity = 4;
		}
		if (capacity == 1) {
			// Anchor has been depleted
			data.remove(CAPACITY_KEY);

			// Remove location value because the player now has no linked anchor
			pdc.remove(LOCATION_KEY);
			p.setRespawnLocation(null);
			p.sendMessage(ConfigManager.getAnchorDepletedMsg());

			// Break block + effects
			block.setType(Material.AIR);
			p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.75f, 1f);
			p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 3f, 3f);
		} else {
			// Anchor has not been depleted
			data.set(CAPACITY_KEY, PersistentDataType.INTEGER, capacity - 1);
		}
	}

	// Heuristic for whether an item is something added by another plugin or datapack
	private boolean isCustomItem(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta.getLore() != null && !meta.getLore().isEmpty()) return true;

		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		if (!pdc.getKeys().isEmpty()) return true;

		return false;
	}

	// Add given capacity and a random ID to the given ItemStack's PDC
	private void addAnchorData(ItemStack item, int capacity) {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer pdc = meta.getPersistentDataContainer();

		meta.setLore(List.of(ConfigManager.getAnchorLore(capacity)));
		pdc.set(CAPACITY_KEY, PersistentDataType.INTEGER, capacity);
		pdc.set(ID_KEY, PersistentDataType.LONG, ThreadLocalRandom.current().nextLong());

		item.setItemMeta(meta);
	}
}
