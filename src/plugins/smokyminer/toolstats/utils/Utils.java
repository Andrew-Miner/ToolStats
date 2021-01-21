package plugins.smokyminer.toolstats.utils;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import plugins.smokyminer.toolstats.ToolStats;


public class Utils 
{
	public static final Material hoeMaterials[] = {Material.GRASS_BLOCK, 
											       Material.GRASS_PATH, 
											       Material.DIRT, 
											       Material.COARSE_DIRT};
	
	public static final String logName = "[ToolStats]";
	public static final String logPrefix = logName +" ";
	public static final String warningPrefix = logName + "[WARNING] ";
	public static final String errorPrefix = logName + "[ERROR] ";

	public static final String cWarningPrefix = ChatColor.YELLOW + logName + "[WARNING] ";
	public static final String cErrorPrefix = ChatColor.YELLOW + logName + ChatColor.RED + "[ERROR] ";
	
	public static ToolStats plugin;
	
	public static String convertColorCode(String colorArray[])
	{
		String code = "";
		for(String s : colorArray)
			if(!s.isEmpty() && !s.equals(" "))
				code += "\u00A7" + s.toLowerCase();
		return code;
	}

	public static <T> boolean hasCollision(List<T> list1, List<T> list2)
	{
		if(list1 == null || list2 == null)
			return false;
		
		for(T t1 : list1)
			if(list2.contains(t1))
				return true;
		return false;
	}

	public static <T> T getCollision(List<T> list1, List<T> list2)
	{
		if(list1 == null || list2 == null)
			return null;
		
		for(T t1 : list1)
			if(list2.contains(t1))
				return t1;
		return null;
	}
	
	public static EntityType getEntityByName(String name)
	{
		for(EntityType t : EntityType.values())
			if(t.name().equalsIgnoreCase(name))
				return t;
		return null;
	}
	
	public static String formatAPIName(String bukkitName)
	{
		if(bukkitName == null)
			return null;
		
		StringBuilder builder = new StringBuilder();
		for(String word : bukkitName.split("_"))
			builder.append(word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase() + " ");
		return builder.toString().trim();
	}
	
	public static <T> boolean listToStrContains(List<T> list, String minecraftName)
	{
		for(T t : list)
			if(minecraftName.equals(formatAPIName(t.toString())))
				return true;
		return false;
	}
	
	public static <T> boolean setToStrContains(Set<T> list, String minecraftName)
	{
		for(T t : list)
			if(minecraftName.equals(formatAPIName(t.toString())))
				return true;
		return false;
	}
	
	public static boolean viewerHasPermission(List<HumanEntity> viewers, String groupName)
	{
		if(viewers.isEmpty())
			return false;
		
		for(HumanEntity ent : viewers)
		{
			if(!(ent instanceof Player))
				continue;
			
			Player p = (Player) ent;
			if(p.hasPermission("toolstats.track.*"))
				return true;
			if(p.hasPermission("toolstats.track." + groupName.toLowerCase()))
				return true;
		}
		
		return false;
	}
	
	public static <T> SimpleEntry<Integer, Integer> fixStats(PersistentDataContainer container, List<String> lore, int start, int end, String color, 
			 												 String countColor, String prefix, List<T> missing, List<T> ordered, Map<T, NamespacedKey> keys)
	{
		if(container == null || lore == null)
			return new AbstractMap.SimpleEntry<Integer, Integer>(start, end);
		if(missing == null || missing.isEmpty())
			return new AbstractMap.SimpleEntry<Integer, Integer>(start, end);
		if(ordered == null || ordered.isEmpty())
			return new AbstractMap.SimpleEntry<Integer, Integer>(start, end);
		if(keys == null || keys.isEmpty())
			return new AbstractMap.SimpleEntry<Integer, Integer>(start, end);
		
		for(T item : missing)
		{
			container.set(keys.get(item), PersistentDataType.INTEGER, 0);
			if(start != -1)
			{
				if(color == null || countColor == null || prefix == null)
					continue;
				
				int i = ordered.indexOf(item);
		
				i += start + 1;
				if(i > lore.size())
					i = lore.size();
		
				String itemStr = formatAPIName(item.toString());
				lore.add(i, color + prefix + itemStr + ": " + countColor + "0");
				end++;
			}
		}

		return new AbstractMap.SimpleEntry<Integer, Integer>(start, end);
	}
	
	public static void logCollision(String disabledGroup, List<String> disWorlds, List<String> disHeaders, 
								    String collidedGroup, List<String> colWorlds, List<String> colHeaders, 
								    Material sharedTool, String sharedWorld,  String sharedHeader)
	{
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		
		Bukkit.getLogger().info("");
		console.sendMessage(Utils.cErrorPrefix + "Disabling Tool Group \"" + ChatColor.YELLOW + 
							disabledGroup + ChatColor.RED + "\": configuration collision found!");

		Bukkit.getLogger().info("");
					
		String tab = "  ";
		ChatColor warnColor = ChatColor.YELLOW;
					
		console.sendMessage(tab + Utils.cWarningPrefix + "Collision between tool groups \"" + 
							ChatColor.WHITE + disabledGroup + warnColor + "\" and \"" + 
							ChatColor.WHITE + collidedGroup + warnColor + "\" found!");
					
					
		if(sharedWorld != null)
			console.sendMessage(tab + Utils.cWarningPrefix + "Both groups include the tool " + ChatColor.WHITE + 
								sharedTool.toString() + warnColor + " and are active in the world \"" + 
								ChatColor.WHITE + sharedWorld  + warnColor + "\"!");
		else if(colWorlds == null && disWorlds != null)
			console.sendMessage(tab + Utils.cWarningPrefix + "Both groups include the tool " + ChatColor.WHITE + 
								sharedTool.toString() + warnColor + " and are active in the world \"" + 
								ChatColor.WHITE + disWorlds.get(0) + warnColor + "\"!");
		else if(colWorlds != null && disWorlds == null)
			console.sendMessage(tab + Utils.cWarningPrefix + "Both groups include the tool " + ChatColor.WHITE + 
								sharedTool.toString() + warnColor + " and are active in the world \"" + 
								ChatColor.WHITE + colWorlds.get(0) + warnColor + "\"!");
		else
			console.sendMessage(tab + Utils.cWarningPrefix + "Both groups include the tool " + ChatColor.WHITE + 
								sharedTool.toString() + warnColor + " and are active in " + 
								ChatColor.WHITE + "all" + warnColor + " worlds!");

					
		switch(colHeaders.indexOf(sharedHeader))
		{
		case 0:
			console.sendMessage(tab + Utils.cWarningPrefix + "They also each have the same header \"" + 
								ChatColor.WHITE + sharedHeader + warnColor + "\" under \"" + 
								ChatColor.WHITE + "Blocks Destroyed" + warnColor + "\"!");
			break;
		case 1:
			console.sendMessage(tab + Utils.cWarningPrefix + "They also each have the same header \"" + 
								ChatColor.WHITE + sharedHeader + warnColor + "\" under \"" + 
								ChatColor.WHITE + "Mobs Killed" + warnColor + "\"!");
			break;
		case 2:
			console.sendMessage(tab + Utils.cWarningPrefix + "They also each have the same header \"" + 
								ChatColor.WHITE + sharedHeader + warnColor + "\" under \"" + 
								ChatColor.WHITE + "Blocks Tilled" + warnColor + "\"!");
			break;
		default:
			console.sendMessage(tab + Utils.cWarningPrefix + 
								"They also each have the same header \"" + 
								ChatColor.WHITE + sharedHeader + warnColor + "\"!");
			break;
		}
					
					
		console.sendMessage(tab + Utils.cWarningPrefix + "To fix this collision change at least one of these configuration options!");
		Bukkit.getLogger().info("");
	}
	
	public static boolean isInteger(String s) 
	{
	    return s.matches("^\\+?(0|[1-9]\\d*)$");
	}
}





















