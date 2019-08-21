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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

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
	
	@SuppressWarnings("unused")
	private File configFile;
	private FileConfiguration config;
	@SuppressWarnings("unused")
	private String configPath, groupName;
	
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

		breakStats.updateLore(e.getPlayer(), tool, block, deleteUntracked);
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
		killStats.updateLore(e.getEntity().getKiller(), tool, ent, deleteUntracked);
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
		tillStats.updateLore(e.getPlayer(), tool, block, deleteUntracked);
	}
	
}