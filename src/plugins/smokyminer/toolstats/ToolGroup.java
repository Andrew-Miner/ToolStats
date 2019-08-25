package plugins.smokyminer.toolstats;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.google.common.collect.Lists;

import plugins.smokyminer.toolstats.statsection.BreakSection;
import plugins.smokyminer.toolstats.statsection.KillSection;
import plugins.smokyminer.toolstats.statsection.StatsSection;
import plugins.smokyminer.toolstats.utils.ConfigUtils;
import plugins.smokyminer.toolstats.utils.Utils;

public class ToolGroup implements Listener
{
	public final String logPrefix;;
	public final String errorPrefix;
	public final String warningPrefix;
	public final String configPath, groupName;
	
	@SuppressWarnings("unused")
	private File configFile;
	private FileConfiguration config;
	
	BreakSection breakStats;
	StatsSection<Material> tillStats;
	KillSection killStats;
	
	private List<String> worlds;
	private ArrayList<Material> tools;
	private boolean hoeOnly, deleteUntracked;
	
	public ToolGroup()
	{
		logPrefix = Utils.logName + "[Null] ";
		errorPrefix =  Utils.logName + "[ERROR][Null] ";
		warningPrefix =  Utils.logName + "[WARNING][Null] ";
		
		breakStats = new BreakSection();
		tillStats = new StatsSection<Material>();
		killStats = new KillSection();
		
		configPath = null;
		groupName = null;
	}
	
	public ToolGroup(File configFile, FileConfiguration config, String configPath, String name)
	{
		logPrefix = Utils.logName + "[" + name + "] ";
		errorPrefix =  Utils.logName + "[ERROR][" + name + "] ";
		warningPrefix = Utils.logName + "[WARNING][" + name + "] ";
		
		this.configFile = configFile;
		this.config = config;
		this.configPath = configPath;
		this.groupName = name;
		
		init();
	}
	
	private void init()
	{ 
		loadGeneralVars();
		
		if(config.contains(configPath + ".Blocks Destroyed"))
			breakStats = new BreakSection(this, "Blocks Destroyed", "Materials", (str) -> Material.getMaterial(str));
		else
			breakStats = new BreakSection();

		if(config.contains(configPath + ".Blocks Tilled"))
			tillStats = new StatsSection<Material>(this, "Blocks Tilled", "Materials", (str) -> Material.getMaterial(str));
		else
			tillStats = new StatsSection<Material>();

		if(config.contains(configPath + ".Mobs Killed"))
			killStats = new KillSection(this, "Mobs Killed", "Entity Types", (str) -> Utils.getEntityByName(str));
		else
			killStats = new KillSection();

		if(tillStats.isEnabled())
		{
			List<Material> tills = tillStats.getTrackList();
			
			for(Material m : tills)
			{
				boolean found = false;
				for(Material tillable : Utils.hoeMaterials)
					if(tillable.equals(m))
						found = true;
				
				if(!found)
					Bukkit.getServer().getLogger().warning(warningPrefix + m.toString() + " cannot normally be tilled!");
			}
		}
	}
	
	public void reload()
	{ 
		loadGeneralVars();
		
		breakStats.reload();
		tillStats.reload();
		killStats.reload();

		if(tillStats.isEnabled())
		{
			List<Material> tills = tillStats.getTrackList();
			for(Material m : tills)
			{
				boolean found = false;
				for(Material tillable : Utils.hoeMaterials)
					if(tillable.equals(m))
						found = true;
				
				if(!found)
					Bukkit.getServer().getLogger().warning(warningPrefix + m.toString() + " cannot normally be tilled!");
			}
		}
	}
	
	private void loadGeneralVars()
	{
		tools = ConfigUtils.loadList(config, configPath, "Tools", (str) -> Material.getMaterial(str));
		if(tools == null)
		{
			Bukkit.getServer().getLogger().severe(warningPrefix + "Missing config section \"Tools\"!");
			tools = new ArrayList<Material>();
		}
		
		worlds = ConfigUtils.loadWorlds(config, configPath);
		deleteUntracked = ConfigUtils.loadBoolean(config, configPath, "Delete Untracked Stats");
	}
	
	// =============== Getters ===============
	
	public List<String> getHeaders()
	{
		String headers[] = {breakStats.getHeader(), 
							killStats.getHeader(), 
							tillStats.getHeader()};
		return Arrays.asList(headers);
	}
	
	public List<String> getWorlds()
	{
		if(worlds == null)
			return null;
		
		return Arrays.asList((String[])worlds.toArray());
	}
	
	@SuppressWarnings("unchecked")
	public List<Material> getTools()
	{
		if(tools == null)
			return null;
		
		return (List<Material>) tools.clone();
	}
	
	public FileConfiguration getFileConfiguration()
	{
		return config;
	}
	
	public String getConfigPath()
	{
		return configPath;
	}
	
	// =============== Utility Methods ===============
	
	public boolean hasMaterials()
	{
		return breakStats.isEnabled();
	}
	
	public boolean hasTills()
	{
		return tillStats.isEnabled();
	}
	
	public boolean hasEntities()
	{
		return killStats.isEnabled();
	}
	
	public boolean containsTool(Material tool)
	{
		if(tools == null)
			return false;
		
		return tools.contains(tool);
	}
	
	public boolean containsWorld(String world)
	{
		if(worlds == null)
			return false;
		
		return worlds.contains(world);
	}
	
	public boolean addLore(ItemStack item)
	{
		if(!tools.contains(item.getType()))
			return false;
		
		breakStats.addLore(item);
		tillStats.addLore(item);
		killStats.addLore(item);
		return true;
	}
	
	public boolean removeLore(ItemStack item)
	{
		if(!tools.contains(item.getType()))
			return false;
		
		breakStats.removeLore(item);
		tillStats.removeLore(item);
		killStats.removeLore(item);
		return true;
	}
	
	public boolean addStats(ItemStack item)
	{
		boolean updated = false;
		ItemMeta tMeta = item.getItemMeta();
		List<String> oldLore = tMeta.getLore();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();
		
		if(breakStats.isEnabled() && !breakStats.hasTags(container))
		{
			breakStats.addTags(container, true);
			oldLore = breakStats.addLore(container, oldLore);
			updated = true;
		}
		
		if(tillStats.isEnabled() && !tillStats.hasTags(container))
		{
			if(!hoeOnly || item.getType().toString().contains("HOE"))
			{
				tillStats.addTags(container,  true);
				oldLore = tillStats.addLore(container, oldLore);
				updated = true;
			}
		}
		
		if(killStats.isEnabled() && !killStats.hasTags(container))
		{
			killStats.addTags(container, true);
			oldLore = killStats.addLore(container, oldLore);
			updated = true;
		}
		
		if(updated)
		{
			container.set(Utils.plugin.showKey, PersistentDataType.INTEGER, 1);
			tMeta.setLore(oldLore);
			item.setItemMeta(tMeta);
		}
		
		return updated;
	}
	
	// =============== Minecraft Events ===============
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void updateBlockStats(BlockBreakEvent e)
	{
		if(e.getBlock() == null)
			return;
		
		ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
		if(tool == null)
			return;
		
		// Verify ToolGroup is active in current world
		if(worlds != null && !containsWorld(e.getBlock().getWorld().getName()))
			return;

		// If tool is apart of ToolGroup and ToolGroup tracks block material
		Material block = e.getBlock().getType();
		if(!containsTool(tool.getType()))
			return;

		//breakStats.updateLore(e.getPlayer(), tool, block, deleteUntracked);
		breakStats.updateCount(e.getPlayer(), tool, block, deleteUntracked);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void updateKillStats(EntityDeathEvent e)
	{
		if(e.getEntity().getKiller() == null || !(e.getEntity().getKiller() instanceof Player))
			return;
		
		ItemStack tool = e.getEntity().getKiller().getInventory().getItemInMainHand();
		if(tool == null)
			return;
		
		if(worlds != null && !containsWorld(e.getEntity().getWorld().getName()))
			return;
		
		if(!containsTool(tool.getType()))
			return;

		EntityType ent = e.getEntityType();
		//killStats.updateLore(e.getEntity().getKiller(), tool, ent, deleteUntracked);
		killStats.updateCount(e.getEntity().getKiller(), tool, ent, deleteUntracked);
	}
	

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void updateTilledStats(PlayerInteractEvent e)
	{
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		if(e.getClickedBlock() == null)
			return;

		if(e.getBlockFace() == BlockFace.DOWN)
			return;
		
		ItemStack tool = e.getItem();
		if(tool == null)
			return;
		
		if(worlds != null && !containsWorld(e.getPlayer().getWorld().getName()))
			return;
		
		if(!containsTool(tool.getType()))
			return;
		
		if(hoeOnly && !tool.getType().toString().contains("HOE"))
			return;

		Material block = e.getClickedBlock().getType();
		//tillStats.updateLore(e.getPlayer(), tool, block, deleteUntracked);
		tillStats.updateCount(e.getPlayer(), tool, block, deleteUntracked);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void prepareItemCraft(PrepareItemCraftEvent e)
	{
		if(e.getRecipe() == null)
			return;
		
		ItemStack tool = e.getInventory().getResult();
		
		if(tool == null || !containsTool(tool.getType()))
			return;
		
		Inventory inv = e.getInventory();
		List<ItemStack> contents = Lists.newArrayList(inv.getContents());
		Bukkit.getLogger().info("Inv Item Count: " + contents.size());
		contents.remove(e.getInventory().getResult());
		Bukkit.getLogger().info("Inv Item Count: " + contents.size());
		Bukkit.getLogger().info("Inv Size: " + inv.getSize());
		
		Recipe recipe = e.getRecipe();
		
		if(recipe instanceof ShapelessRecipe)
		{
			Bukkit.getLogger().info("Shapeless");
			ShapelessRecipe r = (ShapelessRecipe) e.getRecipe();
			List<ItemStack> ingredients = r.getIngredientList();
			Bukkit.getLogger().info("Ingredient Size: " + ingredients.size());
			
			for(ItemStack i : contents)
			{
				if(i == null || i.getType().equals(Material.AIR))
					continue;
				Bukkit.getLogger().info("Ingredient: " + i.getType().toString());
				ItemMeta m = i.getItemMeta();
				PersistentDataContainer container = m.getPersistentDataContainer();
				Bukkit.getLogger().info("ShowKey: " + container.has(Utils.plugin.showKey, PersistentDataType.INTEGER));
			}
		}
		else if(recipe instanceof ShapedRecipe)
			if(addStats(tool))
				e.getInventory().setResult(tool);
		
		
//		ItemMeta tMeta = tool.getItemMeta();
//		PersistentDataContainer container = tMeta.getPersistentDataContainer();
//		
//		if(!breakStats.hasTags(container))
//		{
//			breakStats.addTags(container, true);
//			breakStats.addLore(tool);
//		}
//		
//		if(!killStats.hasTags(container))
//		{
//			breakStats.addTags(container, true);
//			breakStats.addLore(tool);
//		}
	}	
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void prepareAnvil(PrepareAnvilEvent e)
	{
		ItemStack tool = e.getResult();
		
		if(tool == null || !containsTool(tool.getType()))
			return;
		
		Inventory inv = e.getInventory();
		Bukkit.getLogger().info("Inv Item Count: " + inv.getStorageContents().length);
		Bukkit.getLogger().info("Inv Size: " + inv.getSize());
		
		
//		ItemMeta tMeta = tool.getItemMeta();
//		PersistentDataContainer container = tMeta.getPersistentDataContainer();
//		
//		if(!breakStats.hasTags(container))
//		{
//			breakStats.addTags(container, true);
//			breakStats.addLore(tool);
//		}
//		
//		if(!killStats.hasTags(container))
//		{
//			breakStats.addTags(container, true);
//			breakStats.addLore(tool);
//		}
	}
}
































