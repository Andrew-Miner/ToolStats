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
			return null;
		
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
	
	public static <T> List<String> buildLore(List<T> loreTypes, List<String> stringList, String header, String color)
	{
		return buildLore(loreTypes, stringList, header, color, false);
	}
	
	public static <T> List<String> buildLore(List<T> loreTypes, List<String> stringList, String header, String color, boolean blankEndLine)
	{
		if(loreTypes == null || loreTypes.isEmpty())
			return null;
		
		ArrayList<String> lore = new ArrayList<String>();
		//String lore[] = new String[loreTypes.size() + ((header == null) ? 0 : 1)];
		
		if(header != null)
			lore.add(color + header);
		
		for(T t : loreTypes)
			lore.add(color + formatAPIName(t.toString()) + ": 0");
		
		if(stringList != null)
			for(String s : stringList)
				lore.add(color + s + ": 0");
		
		if(blankEndLine)
			lore.add("");
		
		return lore;
	}
	
	public static <T> boolean updateLore(List<String> toolLore, T loreType, int startIndex, int exclusiveEndIndex)
	{
		for(int i = startIndex; i < toolLore.size() && i < exclusiveEndIndex; i++)
		{
			String splitLine[] = toolLore.get(i).split(":");
			
			if(splitLine.length != 2)
				continue;
			
			String typeName = splitLine[0];
			String colorLess = ChatColor.stripColor(typeName.trim());
			
			if(!colorLess.equals(formatAPIName(loreType.toString())))
				continue;
			
			
			String val = splitLine[1].trim();
			
			if(!StringUtils.isNumeric(val))
				continue;
			
			
			int count = Integer.parseInt(val) + 1;
			count = (count < 0) ? 0 : count;
			
			toolLore.set(i, typeName + ": " + count);
			return true;
		}
		return false;
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
		
		if(!StringUtils.isNumeric(split[1].trim()))
			return false;
		
		return true;
	}
	  
	public static String getTypeFromLore(String line)
	{
		if(line == null)
			return null;
		
		String[] split = line.split(":");
		if(split.length != 2)
			return null;
		
		if(!StringUtils.isNumeric(split[1].trim()))
			return null;
		
		return ChatColor.stripColor(split[0]).trim();
	}
	
	public static <T> int getEndOfLore(List<String> toolLore, int startIndex, Set<T> loreTypes, Set<String> loreStrings, String header, boolean updateLore, String color)
	{
		if(loreTypes == null || loreTypes.isEmpty())
			if(loreStrings == null || loreStrings.isEmpty())
				return -1;
		
		if(toolLore == null || toolLore.isEmpty())
			return -1;
		
		int index = startIndex;
		if(header != null)
		{
			if(!ChatColor.stripColor(toolLore.get(index)).equals(header))
				return -1;
			else
				index++;
		}

		Set<String> foundTypes = new HashSet<String>();	
		ListIterator<String> it = toolLore.listIterator(index);
		while(it.hasNext())
		{
			String line = it.next();
			
			String item = getTypeFromLore(line);
			if(item == null)
				break;
			
			if(updateLore && !setToStrContains(loreTypes, item) && !loreStrings.contains(item))
				it.remove();
			else
			{
				index++;
				foundTypes.add(item);
			}
		}
		
		if(updateLore)
			addMissingTypes(toolLore, foundTypes, loreTypes, loreStrings, index, color);
		
		return index;
	}

	public static <T> Map.Entry<Integer, Integer> getHeaderPattern(List<String> toolLore, Set<T> loreTypes, Set<String> loreStrings, String header)
	{
		return getHeaderPattern(toolLore, loreTypes, loreStrings, header, "", null, null, null);
	}
	
	public static <T> Map.Entry<Integer, Integer> getHeaderPattern(List<String> toolLore, Set<T> loreTypes, Set<String> loreStrings, String header, String color)
	{
		return getHeaderPattern(toolLore, loreTypes, loreStrings, header, color, null, null, null);
	}
	
	public static <T> Map.Entry<Integer, Integer> getHeaderPattern(List<String> toolLore, Set<T> loreTypes, Set<String> loreStrings, String header, String color, 
																   List<T> missingTypes, List<String> missingStrings, List<Integer> extraItems)
	{
		if(loreTypes == null || loreTypes.isEmpty())
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		
		if(toolLore == null || toolLore.isEmpty())
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		
		int index = 0, startIndex = -1;
		for(; index < toolLore.size(); index++)
		{
			if(ChatColor.stripColor(toolLore.get(index)).equals(header))
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
			
			String item = getTypeFromLore(line);
			if(item == null)
				break;
			
			if(!setToStrContains(loreTypes, item) && (loreStrings == null || !loreStrings.contains(item)))
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
		
		if(loreStrings != null)
		{
			for(String s : loreStrings)
			{
				if(!foundTypes.contains(s))
					missingStrings.add(s);
			}
		}
		
		return new AbstractMap.SimpleEntry<Integer, Integer>(startIndex, index);
	}
	
	public static <T> void addMissingTypes(List<String> lore, Set<String> exsistingTypes, Set<T> includeTypes, Set<String> includeStrings, int index, String color)
	{
		for(T t : includeTypes)
		{
			String entry = formatAPIName(t.toString());
			if(!exsistingTypes.contains(entry))
				lore.add(index++, color + entry + ": 0");
		}
		
		if(includeStrings != null)
			for(String s : includeStrings)
				if(!exsistingTypes.contains(s))
					lore.add(index++, color + s + ": 0");
	}
	
	// Returns -1 is pattern not found
	// Otherwise returns pattern starting index
	public static <T> int hasLorePattern(List<String> toolLore, List<T> loreTypes, String header)
	{
		if(loreTypes == null || loreTypes.isEmpty())
			return -1;
		
		if(toolLore == null || toolLore.isEmpty())
			return -1;
		
		boolean success = false;
		int startIndex = 0;
		for(int i = 0; i < toolLore.size(); i++)
		{
			ArrayList<String> foundTypes = new ArrayList<String>();
			
			int index = i;
			boolean startFound = false;
			for(; index < toolLore.size(); index++)
			{
				if(header == null)
				{
					String startingLine = ChatColor.stripColor(toolLore.get(index).split(":")[0].trim());
					if(listToStrContains(loreTypes, startingLine))
					{
						foundTypes.add(startingLine);
						startIndex = index;
						startFound = true;
						break;
					}
				}
				else if(ChatColor.stripColor(toolLore.get(index)).equals(header))
				{
					startIndex = index;
					startFound = true;
					break;
				}
				
			}
			
			if(!startFound)
				return -1;
			
			int maxType = ++index + loreTypes.size() + ((header == null) ? -1 : 0);
			for(; index < maxType && index < toolLore.size(); index++)
			{
				String loreType = ChatColor.stripColor(toolLore.get(index).split(":")[0].trim());
				if(listToStrContains(loreTypes, loreType) && !foundTypes.contains(loreType))
					foundTypes.add(loreType);
				else
					break;
			}
			if(foundTypes.size() != loreTypes.size())
				continue;
			
			success = true;
			break;
		}
		
		if(success)
			return startIndex;
		return -1;
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

	public static <T> List<String> buildLore(PersistentDataContainer container, Map<T, NamespacedKey> typeTags, List<T> orderedKeys, String color) 
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
			
			newLore.add(color + formatAPIName(t.toString()) + ": " + count);
		}
		
		if(newLore.isEmpty())
			return null;
		return newLore;
	}
}





















