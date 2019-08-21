package plugins.smokyminer.toolstats.statsection.rewardsection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import plugins.smokyminer.toolstats.statsection.StatsSection;
import plugins.smokyminer.toolstats.utils.ConfigUtils;

public class RewardSection<T> 
{
	protected final String configPath;
	protected final StatsSection<T> parent;
	protected final String rewardStr;
	
	protected boolean enabled;
	Map<String, Integer> objectives;
	
	boolean ignoreIncompatible;
	Map<Enchantment, Integer> enchantments;
	
	List<String> commands;
	Map<Material, Integer> items;
	
	Sound sound;
	Integer expLevels, expPoints;
	
	@SuppressWarnings("deprecation")
	public RewardSection(StatsSection<T> parent, String configPath, String rewardStr)
	{
		this.parent = parent;
		if(parent == null)
			throw new IllegalArgumentException("RewardSection parent cannot be null!");
		
		this.configPath = configPath;
		if(configPath == null)
			throw new IllegalArgumentException("RewardSection configuartion path cannot be null!");
		
		this.rewardStr = rewardStr;

		FileConfiguration config = parent.getParent().getFileConfiguration();
		if(!config.contains(configPath))
			throw new IllegalArgumentException("RewardSection configuration path is invalid!");
		
		reload();
		
		if(objectives != null)
			for(Map.Entry<String, Integer> pair : objectives.entrySet())
				Bukkit.getLogger().info(pair.getKey() + " : " + pair.getValue());
		
		if(enchantments != null)
			for(Map.Entry<Enchantment, Integer> pair : enchantments.entrySet())
				Bukkit.getLogger().info(pair.getKey().getName() + " : " + pair.getValue());
		
		if(items != null)
			for(Map.Entry<Material, Integer> pair : items.entrySet())
				Bukkit.getLogger().info(pair.getKey().name() + " : " + pair.getValue());
		
		if(commands != null)
			for(String c : commands)
				Bukkit.getLogger().info(c);
		
		if(sound != null)
			Bukkit.getLogger().info(sound.name());
		if(expLevels != null)
			Bukkit.getLogger().info(expLevels.toString());
		if(expPoints != null)
			Bukkit.getLogger().info(expPoints.toString());
	}
	
	public void reload()
	{
		FileConfiguration config = parent.getParent().getFileConfiguration();
		
		enabled = ConfigUtils.loadBoolean(config, configPath, "Enabled", true);
		ignoreIncompatible = ConfigUtils.loadBoolean(config, configPath, "Enchantments.Ignore Incompatible", true);
		
		loadItems(config);
		loadSound(config);
		loadObjectives(config);
		loadEnchantments(config);
		
		expLevels = ConfigUtils.loadInteger(config, configPath, "Exp Levels");
		expPoints = ConfigUtils.loadInteger(config, configPath, "Exp Points");
		
		commands = ConfigUtils.loadStrList(config, configPath, "Commands", (str) -> {
			return ChatColor.translateAlternateColorCodes('&', str);
		});
	}
	
	private void loadSound(FileConfiguration config)
	{
		String soundStr = ConfigUtils.loadString(config, configPath, "Sound");
		
		if(soundStr == null)
			return;
		
		try
		{
			soundStr = soundStr.toUpperCase().trim().replace(' ', '_');
			sound = Sound.valueOf(soundStr);
		}
		catch(IllegalArgumentException e) { return; }
	}
	
	private void loadItems(FileConfiguration config)
	{
		if(config.contains(configPath + ".Items"))
		{
			items = ConfigUtils.loadMap(config, configPath, "Items",
					
			(str) -> {
				Material m = Material.getMaterial(str);
				if(m == null)
					Bukkit.getServer().getLogger().severe(parent.getParent().warningPrefix + parent.eventSection + " - " + 
										  				  rewardStr + ": \"" + str + "\" is not a valid item!");
				return m;
			},
					
			(str) -> {
				if(StringUtils.isNumeric(str))
					return Integer.parseInt(str);
				Bukkit.getLogger().warning(parent.getParent().errorPrefix + parent.eventSection + 
										   " - " + rewardStr + ": \"" + str + "\" is not an integer!");
				return null;
			},
					
			parent.getParent().warningPrefix + parent.eventSection + " - " + rewardStr + ": ", "item");
					
			if(items == null)
				Bukkit.getServer().getLogger().warning(parent.getParent().warningPrefix + parent.eventSection + " - " + rewardStr + ": \"Items\" list is empty!");
		}
	}
	
	private void loadEnchantments(FileConfiguration config)
	{
		if(config.contains(configPath + ".Enchantments"))
		{
			enchantments = ConfigUtils.loadMap(config, configPath, "Enchantments.List",
					
			(str) -> {
				String newStr = "";
				String split[] = str.split("_");
				for(int i = 0; i < split.length; i++)
					newStr += split[i].toLowerCase() + " ";
				newStr = newStr.trim();
				
				Enchantment e = null;
				try { e = EnchantmentWrapper.getByKey(NamespacedKey.minecraft(str.toLowerCase())); }
				catch(IllegalArgumentException ex) { }
				
				if(e == null)
					Bukkit.getServer().getLogger().severe(parent.getParent().warningPrefix + parent.eventSection + " - " + 
										  						  rewardStr + ": \"" + newStr + "\" is not a valid enchantment!");
				return e;
			},
					
			(str) -> {
				if(StringUtils.isNumeric(str))
					return Integer.parseInt(str);
				Bukkit.getLogger().warning(parent.getParent().errorPrefix + parent.eventSection + 
												   " - " + rewardStr + ": \"" + str + "\" is not an integer!");
				return null;
			},
					
			parent.getParent().warningPrefix + parent.eventSection + " - " + rewardStr + ": ", "enchantment");
					
			if(enchantments == null)
				Bukkit.getServer().getLogger().severe(parent.getParent().errorPrefix + parent.eventSection + " - " + rewardStr +
								  							 ": Ignoring \"Enchantments\" because it is missing \"List\"!");
		}
	}
	
	private void loadObjectives(FileConfiguration config)
	{
		objectives = ConfigUtils.loadMap(config, configPath, "Requirements", 
				
		(str) -> {
			if(parent.isTracked(str))
				return str;
			Bukkit.getServer().getLogger().severe(parent.getParent().warningPrefix + parent.eventSection + " - " + 
					   							  rewardStr + ": \"" + str + "\" is not a valid requirement!");
			return null;
		}, 
		
		(str) -> {
			if(StringUtils.isNumeric(str))
				return Integer.parseInt(str);
			Bukkit.getLogger().warning(parent.getParent().errorPrefix + parent.eventSection + 
									   " - " + rewardStr + ": \"" + str + "\" is not an integer!");
			return null;
		}, 
		
		parent.getParent().warningPrefix + parent.eventSection + " - " + rewardStr + ": ", "requirement");
		
		if(objectives == null)
			Bukkit.getServer().getLogger().severe(parent.getParent().errorPrefix + parent.eventSection + 
												  ": Ignoring \"" + rewardStr + "\" because it is missing \"Requirements\"!");
	}
	
	public boolean update(Player player, ItemStack tool, T item, List<String> lore, int startIndex, int endIndex)
	{
		if(!enabled || objectives == null || objectives.isEmpty())
			return false;
		
		if(!checkRequirements(item, lore, startIndex, endIndex))
			return false;
		
		if(sound != null)
			player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
		
		if(expLevels != null)
			player.giveExpLevels(expLevels);
		
		if(expPoints != null)
			player.giveExp(expPoints);
		
		if(items != null)
			for(Map.Entry<Material, Integer> pair : items.entrySet())
				player.getInventory().addItem(new ItemStack(pair.getKey(), pair.getValue()));
		
		if(enchantments != null)
		{
			if(ignoreIncompatible)
				tool.addEnchantments(enchantments);
			else
				tool.addUnsafeEnchantments(enchantments);
		}
		
		if(commands != null)
			runCommands(player);
		
		return true;
	}
	
	public boolean checkRequirements(T item, List<String> lore, int startIndex, int endIndex)
	{
		if(objectives == null || !objectives.containsKey(item.toString()))
			return false;
		
		Map<String, Integer> found = new HashMap<String, Integer>();
		
		for(int i = startIndex; i < endIndex && i < lore.size(); i++)
		{
			String line = lore.get(i);
			if(line == null)
				continue;
			
			String[] split = line.split(":");
			if(split.length != 2)
				continue;
			
			split[1] = split[1].trim();
			if(!StringUtils.isNumeric(split[1]))
				continue;
			
			String foundItem = ChatColor.stripColor(split[0].trim().toUpperCase().replace(' ', '_'));
			if(objectives.containsKey(foundItem))
				found.put(foundItem, Integer.parseInt(split[1]));
		}
		
		for(Map.Entry<String, Integer> pair : objectives.entrySet())
		{
			if(found.containsKey(pair.getKey()))
			{
				Integer stat = found.get(pair.getKey());
				if(pair.getKey().equals(item.toString()))
				{
					if(stat != pair.getValue())
						return false;
				}
				else if(stat < pair.getValue())
					return false;
			}
			else
				return false;
		}
		
		return true;
	}
	
	private void runCommands(Player player)
	{
		for(String c : commands)
		{
			String split[] = c.split("%");
			
			String newC = "";
			for(int i = 0; i < split.length; i++)
			{
				if(split[i].equalsIgnoreCase("player"))
					newC = newC.substring(0, newC.length() - 1) + player.getName();
				else if(i != split.length - 1)
					newC += split[i] + "%";
				else
					newC += split[i];
			}
			
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), newC);
		}
	}
}
