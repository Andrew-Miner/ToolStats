package plugins.smokyminer.toolstats.utils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
	
	
	public static <T> HashMap<T, NamespacedKey> buildTags(List<T> tagTypes, String tagPrefix)
	{
		if(tagTypes == null || tagTypes.isEmpty())
			return new HashMap<T, NamespacedKey>();
		
		HashMap<T, NamespacedKey> tags = new HashMap<T, NamespacedKey>();
		
		for(T t : tagTypes)
			tags.put(t, new NamespacedKey(plugin, (tagPrefix + t.toString()).replace(' ', '_')));
		
		return tags;
	}
	
	public static boolean updateTag(PersistentDataContainer container, NamespacedKey key)
	{
		if(container == null || key == null)
			return false;
		
		if(!container.has(key, PersistentDataType.INTEGER))
			return false;
		
		int count = container.get(key, PersistentDataType.INTEGER) + 1;
		count = (count < 0) ? 0 : count;
		
		container.set(key, PersistentDataType.INTEGER, count);
		
		return true;
	}
	
	public static <T> List<String> buildLore(List<T> loreTypes, List<String> stringList, String header, String color, String countColor, String prefix)
	{
		return buildLore(loreTypes, stringList, header, color, countColor, prefix, false);
	}
	
	public static <T> List<String> buildLore(List<T> loreTypes, List<String> stringList, String header, String color, String countColor, String prefix, boolean blankEndLine)
	{
		if((loreTypes == null || loreTypes.isEmpty()) && (stringList == null || stringList.isEmpty()))
			return null;
		
		ArrayList<String> lore = new ArrayList<String>();
		
		if(header != null)
			lore.add(color + ChatColor.translateAlternateColorCodes('&', header));
		
		if(countColor == null)
			countColor = "";
		else
			countColor =  ChatColor.translateAlternateColorCodes('&', countColor);
		
		if(loreTypes != null)
			for(T t : loreTypes)
				lore.add(color + prefix + formatAPIName(t.toString()) + ": " + countColor + "0");
		
		if(stringList != null)
			for(String s : stringList)
				lore.add(color + prefix + s + ": " + countColor + "0");
		
		if(blankEndLine)
			lore.add("");
		
		return lore;
	}
	
	public static <T> boolean updateLore(List<String> toolLore, T loreType, int startIndex, int exclusiveEndIndex, String color, String countColor, String prefix)
	{
		for(int i = startIndex; i < toolLore.size() && i < exclusiveEndIndex; i++)
		{
			String splitLine[] = toolLore.get(i).split(":");
			
			if(splitLine.length != 2)
				continue;
			
			boolean changeColor = false;
			
			if(countColor != null && !countColor.isEmpty())
			{
				if(!splitLine[1].contains(ChatColor.translateAlternateColorCodes('&', countColor)))
					changeColor = true;
			}
			else if(!ChatColor.stripColor(splitLine[1]).equals(splitLine[1]))
				changeColor = true;
				
			
			String val = ChatColor.stripColor(splitLine[1].trim());
			
			splitLine = splitLine[0].split(prefix);
			
			String typeName;
			if(splitLine.length == 2)
			{
				if(!splitLine[0].equals(color))
					changeColor = true;
				typeName = ChatColor.stripColor(splitLine[1].trim());
			}
			else
				typeName = ChatColor.stripColor(splitLine[0].trim());

			
			if(!typeName.equals(formatAPIName(loreType.toString())))
				continue;
			
			if(!StringUtils.isNumeric(val))
				continue;
			
			int count = Integer.parseInt(val) + 1;
			count = (count < 0) ? 0 : count;
			
			if(countColor != null)
				toolLore.set(i, color + prefix + typeName + ": " + ChatColor.translateAlternateColorCodes('&', countColor) + count);
			else
				toolLore.set(i, color + prefix + typeName + ": " + count);
			
			if(changeColor)
				changeLoreColor(toolLore, startIndex, exclusiveEndIndex, color, countColor, prefix);
			
			return true;
		}
		return false;
	}
	public static <T> void changeLoreColor(List<String> toolLore, int startIndex, int exclusiveEndIndex, String color, String countColor, String prefix)
	{
		for(int i = startIndex; i < toolLore.size() && i < exclusiveEndIndex; i++)
		{
			String splitLine[] = toolLore.get(i).split(":");
			
			if(splitLine.length != 2)
				continue;
			
			String val = ChatColor.stripColor(splitLine[1].trim());
			
			splitLine = splitLine[0].split(prefix);

			if(splitLine.length != 2)
				continue;
			
			if(!StringUtils.isNumeric(val))
				continue;

			String typeName = splitLine[1].trim();
			if(countColor != null && !countColor.isEmpty())
				toolLore.set(i, color + prefix + typeName + ": " + ChatColor.translateAlternateColorCodes('&', countColor) + val);
			else
				toolLore.set(i, color + prefix + typeName + ": " + val);
		}
	}

	public static List<String> addLore(List<String> destination, List<String> source)
	{
		return addLore(destination, source, false);
	}
	public static List<String> addLore(List<String> destination, List<String> source, boolean addSpace)
	{
		if(source == null)
			return destination;
		
		ArrayList<String> retArray = new ArrayList<String>();
		if(destination == null || destination.isEmpty())
			retArray.addAll(source);
		else if(addSpace && !destination.get(destination.size() - 1).trim().isEmpty())
		{
			retArray.addAll(destination);
			retArray.add("");
			retArray.addAll(source);
		}
		else
		{
			retArray.addAll(destination);
			retArray.addAll(source);
		}
			
		return retArray;
	}
	
	public static boolean isValidLine(String line) 
	{
		if(line == null)
			return false;
		
		String[] split = line.split(":");
		if(split.length != 2)
			return false;
		
		if(!StringUtils.isNumeric(ChatColor.stripColor(split[1].trim())))
			return false;
		
		return true;
	}
	  
	public static String getTypeFromLore(String line, String prefix)
	{
		if(line == null)
			return null;
		
		String[] split = line.split(":");
		if(split.length != 2)
			return null;
		
		if(!StringUtils.isNumeric(ChatColor.stripColor(split[1].trim())))
			return null;
		
		split = split[0].split(prefix);
		if(split.length == 2)
			return ChatColor.stripColor(split[1]).trim();
		return ChatColor.stripColor(split[0]).trim();
	}

	public static <T> Map.Entry<Integer, Integer> getHeaderPattern(List<String> toolLore, Set<T> loreTypes, Set<String> loreStrings, String header, String prefix)
	{
		return getHeaderPattern(toolLore, loreTypes, loreStrings, header, prefix, null, null, null);
	}
	
	public static <T> Map.Entry<Integer, Integer> getHeaderPattern(List<String> toolLore, Set<T> loreTypes, Set<String> loreStrings, String header, String prefix,
																   List<T> missingTypes, List<String> missingStrings, List<Integer> extraItems)
	{
		if((loreTypes == null || loreTypes.isEmpty()) && (loreStrings == null || loreStrings.isEmpty()))
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		
		if(toolLore == null || toolLore.isEmpty())
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		
		int index = 0, startIndex = -1;
		for(; index < toolLore.size(); index++)
		{
			if(ChatColor.stripColor(toolLore.get(index)).equals(ChatColor.stripColor(header)))
			{
				startIndex = index;
				break;
			}
		}
			
		if(startIndex == -1)
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);

		Set<String> foundTypes = new HashSet<String>();	
		ListIterator<String> it = toolLore.listIterator(++index);
		while(it.hasNext())
		{
			String line = it.next();
			
			String item = getTypeFromLore(line, prefix);
			if(item == null)
				break;
			
			if(!setToStrContains(loreTypes, item) && !loreStrings.contains(item))
			{
				if(extraItems != null)
					extraItems.add(index);
			}
			else
				foundTypes.add(item);
			
			index++;
		}
		
		if(missingTypes != null)
		{
			for(T t : loreTypes)
			{
				String tName = formatAPIName(t.toString());
				if(!foundTypes.contains(tName))
					missingTypes.add(t);
			}
		}
		
		if(missingStrings != null)
		{
			for(String s : loreStrings)
			{
				if(!foundTypes.contains(s))
					missingStrings.add(s);
			}
		}
		
		return new AbstractMap.SimpleEntry<Integer, Integer>(startIndex, index);
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

	public static <T> List<String> buildLore(PersistentDataContainer container, Map<T, NamespacedKey> typeTags, List<T> orderedKeys, String color, String countColor) 
	{
		if(container == null || typeTags == null || orderedKeys == null || color == null)
			return null;
		
		List<String> newLore = new ArrayList<String>();
		for(T t : orderedKeys)
		{
			NamespacedKey key = typeTags.get(t);
			
			if(key == null)
				continue;
			
			if(!container.has(key, PersistentDataType.INTEGER))
				continue;
			
			int count = container.get(key, PersistentDataType.INTEGER);
			
			if(countColor != null && !countColor.isEmpty())
				newLore.add(color + formatAPIName(t.toString()) + ": " + ChatColor.translateAlternateColorCodes('&', countColor) + count);
			else
				newLore.add(color + formatAPIName(t.toString()) + ": " + count);
		}
		
		if(newLore.isEmpty())
			return null;
		return newLore;
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
}





















