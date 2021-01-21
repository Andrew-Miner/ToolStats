package plugins.smokyminer.toolstats.statsection.rewardsection;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import plugins.smokyminer.toolstats.statsection.StatsSection;
import plugins.smokyminer.toolstats.utils.ConfigUtils;
import plugins.smokyminer.toolstats.utils.Utils;

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
				if(Utils.isInteger(str))
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
				if(Utils.isInteger(str))
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
			if(Utils.isInteger(str))
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
	
	public boolean update(Player player, ItemStack tool, T item, Map<T, NamespacedKey> ledger, Map<String, NamespacedKey> strLedger, int startIndex, int endIndex)
	{
		if(!enabled || objectives == null || objectives.isEmpty())
			return false;
		
		ItemMeta meta = tool.getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();
		if(!checkRequirements(container, item, ledger, strLedger, startIndex, endIndex))
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
			for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet())
			{
				if(tool.getEnchantmentLevel(entry.getKey()) >= entry.getValue())
					continue;
				
				if(ignoreIncompatible)
				{
					try { tool.addEnchantment(entry.getKey(), entry.getValue()); }
					catch(IllegalArgumentException e) { }
				}
				else
					tool.addUnsafeEnchantment(entry.getKey(), entry.getValue());
			}
		}
		
		if(commands != null)
			runCommands(player);
		
		return true;
	}
	
	public boolean checkRequirements(PersistentDataContainer container, T item, Map<T, NamespacedKey> ledger, Map<String, NamespacedKey> strLedger, int startIndex, int endIndex)
	{
		if(objectives == null || item == null ||  !objectives.containsKey(item.toString()))
			return false;
		
		for(Map.Entry<String, Integer> pair : objectives.entrySet())
		{
			NamespacedKey tag = null;
			if(strLedger.containsKey(pair.getKey()))
				tag = strLedger.get(pair.getKey());
			else
			{
				for(T t : ledger.keySet())
				{
					if(pair.getKey().equals(t.toString()))
					{
						tag = ledger.get(t);
						break;
					}
				}
			}
			
			if(tag == null)
				return false;
			
			if(!container.has(tag, PersistentDataType.INTEGER))
				return false;
			
			int count = container.get(tag, PersistentDataType.INTEGER);
			
			if(pair.getKey().equals(item.toString()))
			{
				if(count != pair.getValue().intValue())
					return false;
			}
			else if(count < pair.getValue().intValue())
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
