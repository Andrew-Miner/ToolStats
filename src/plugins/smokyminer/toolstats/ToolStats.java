package plugins.smokyminer.toolstats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import plugins.smokyminer.toolstats.commands.CommandGroup;
import plugins.smokyminer.toolstats.commands.HideCommand;
import plugins.smokyminer.toolstats.commands.ReloadCommand;
import plugins.smokyminer.toolstats.commands.ShowCommand;
import plugins.smokyminer.toolstats.utils.ConfigUtils;
import plugins.smokyminer.toolstats.utils.Utils;


public class ToolStats extends JavaPlugin
{
	public final NamespacedKey showKey = new NamespacedKey(this, "ts.show");
	
	private File mainConfigFile;
	private FileConfiguration mainConfig; 
	private final String configStr = "config.yml";

	private Set<String> validGroups;
	private LinkedHashMap<String, ToolGroup> toolGroups;
	
	private static CommandGroup commands;
	
	@Override
	public void onEnable()
	{
		Utils.plugin = this;
		
		init();
		
		validateGroups();
		
		for(Map.Entry<String, ToolGroup> pair : toolGroups.entrySet())
			if(validGroups.contains(pair.getKey()))
				getServer().getPluginManager().registerEvents(pair.getValue(), this);
		
		commands = new CommandGroup();
		commands.registerCommand(new ShowCommand());
		commands.registerCommand(new HideCommand());
		commands.registerCommand(new ReloadCommand());
	}
	
	private void init()
	{
		try
		{
			if(!this.getDataFolder().exists())
				this.getDataFolder().mkdirs();
		}
		catch(Exception e)
		{
			getLogger().severe("Failed to make plugin directory!");
			e.printStackTrace();
		}
		
		if(loadConfig())
			loadGroupConfigs();
	}

	private boolean loadConfig() 
	{
		mainConfigFile = ConfigUtils.createFile(this,  configStr);
		if(mainConfigFile == null)
			return false;
		mainConfig = ConfigUtils.loadFileConfiguration(this, mainConfigFile);
		return true;
	}

	private void loadGroupConfigs() 
	{
		toolGroups = new LinkedHashMap<String, ToolGroup>();
		ArrayList<String> groupRegistry = (ArrayList<String>) mainConfig.getStringList("Tool Group Names");
		
		for(String group : groupRegistry)
		{
			if(mainConfig.contains("Tool Groups." + group))
				toolGroups.put(group, new ToolGroup(mainConfigFile, mainConfig, "Tool Groups." + group, group));
			else if(ConfigUtils.fileExists(this, (group + ".yml").toLowerCase()))
			{
				File tempFile = ConfigUtils.createFile(this, (group + ".yml").toLowerCase());
				FileConfiguration tempConfig = ConfigUtils.loadFileConfiguration(this, tempFile);

				// TODO: build default group yml
				if(!tempConfig.contains(group))
				{
					ConfigUtils.addDefaultGroupLore(tempConfig, group, null);
					tempConfig.options().copyDefaults(true);
					try { tempConfig.save(tempFile); } 
					catch (IOException e) { e.printStackTrace(); }
				}
				else
					toolGroups.put(group, new ToolGroup(tempFile, tempConfig, group, group));
			}
			else
			{
				ConfigUtils.addDefaultGroupLore(mainConfig, group, "Tool Groups");
				mainConfig.options().copyDefaults(true);
				try { mainConfig.save(mainConfigFile); } 
				catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
	
	public void reload()
	{
		for(Map.Entry<String, ToolGroup> pair : toolGroups.entrySet())
			if(validGroups.contains(pair.getKey()))
				HandlerList.unregisterAll(pair.getValue());
		
		toolGroups = null;
		mainConfigFile = null;
		mainConfig = null;
		validGroups = null;
		
		if(loadConfig())
			loadGroupConfigs();
		
		validateGroups();
		
		for(Map.Entry<String, ToolGroup> pair : toolGroups.entrySet())
			if(validGroups.contains(pair.getKey()))
				getServer().getPluginManager().registerEvents(pair.getValue(), this);
	}
	
	public boolean isToolTracked(Material item)
	{
		for(String key : validGroups)
			if(toolGroups.get(key).containsTool(item))
				return true;
		return false;
	}
	
	public ArrayList<ToolGroup> getToolGroups(Material item)
	{
		ArrayList<ToolGroup> list = new ArrayList<ToolGroup>();
		
		for(String key : validGroups)
			if(toolGroups.get(key).containsTool(item))
				list.add(toolGroups.get(key));
		
		if(list.isEmpty())
			return null;
		return list;
	}
	
	private void validateGroups()
	{
		Set<String> validTools = toolGroups.keySet();
		Set<String> removeSet = new HashSet<String>();
		
		for(Iterator<String> it = validTools.iterator(); it.hasNext();)
		{
			String iGroupName = it.next();
			ToolGroup iGroup = toolGroups.get(iGroupName);
			
			if(removeSet.contains(iGroupName))
				continue;
			
			it.forEachRemaining(jGroupName -> 
			{
				if(removeSet.contains(jGroupName))
					return;
				
				ToolGroup jGroup = toolGroups.get(jGroupName);
				
				
				// Check for tool collision
				Material tool = Utils.getCollision(iGroup.getTools(), jGroup.getTools());
				if(tool == null)
					return; // Continue in forEachRemaining
				
				
				// Check for world collision
				List<String> iWorlds = iGroup.getWorlds(),
							 jWorlds = jGroup.getWorlds();
					
				String world = Utils.getCollision(iWorlds, jWorlds);
				if(world == null && iWorlds != null && jWorlds != null)
					return;
				
				
				// Check for header collision
				List<String> iHeaders = iGroup.getHeaders(),
							 jHeaders = jGroup.getHeaders();
						
				String header = Utils.getCollision(iHeaders, jHeaders);
				if(header == null)
					return;
				
				removeSet.add(jGroupName);
				Utils.logCollision(jGroupName, jWorlds, jHeaders, iGroupName, iWorlds, iHeaders, tool, world, header);
			});
		}
		
		for(String removeMe : removeSet)
			validTools.remove(removeMe);
		
		validGroups = validTools;
	}
	
	public static String getConfigCollision(ToolGroup g1, ToolGroup g2)
	{
		if(g1 == null || g2 == null)
			return null;
		
		if(g1.hasMaterials() && g2.hasMaterials())
			return "Blocks Destroyed";
		
		if(g1.hasEntities() && g2.hasEntities())
			return "Mobs Killed";
		
		if(g1.hasTills() && g2.hasTills())
			return "Blocks Tilled";
		
		return null;
	}
}
