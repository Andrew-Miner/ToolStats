package plugins.smokyminer.toolstats.utils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class LoreUtils 
{
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
	
	public static <T> List<String> buildDefaultLore(List<T> loreTypes, List<String> stringList, String header, String color, String countColor, String prefix)
	{
		return buildDefaultLore(loreTypes, stringList, header, color, countColor, prefix, false);
	}
	
	public static <T> List<String> buildDefaultLore(List<T> loreTypes, List<String> stringList, String header, String color, String countColor, String prefix, boolean blankEndLine)
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
				lore.add(color + prefix + Utils.formatAPIName(t.toString()) + ": " + countColor + "0");
		
		if(stringList != null)
			for(String s : stringList)
				lore.add(color + prefix + s + ": " + countColor + "0");
		
		if(blankEndLine)
			lore.add("");
		
		return lore;
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
				newLore.add(color + Utils.formatAPIName(t.toString()) + ": " + ChatColor.translateAlternateColorCodes('&', countColor) + count);
			else
				newLore.add(color + Utils.formatAPIName(t.toString()) + ": " + count);
		}
		
		if(newLore.isEmpty())
			return null;
		return newLore;
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

	// Updates lore count and fixes color mismatch
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
				
			
			String val = ChatColor.stripColor(splitLine[1]).trim();
			
			splitLine = splitLine[0].split(prefix);
			
			String typeName;
			if(splitLine.length == 2)
			{
				if(!splitLine[0].trim().equals(color))
					changeColor = true;
				typeName = ChatColor.stripColor(splitLine[1]).trim();
			}
			else
				typeName = ChatColor.stripColor(splitLine[0]).trim();
			
			if(!typeName.equals(Utils.formatAPIName(loreType.toString())))
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
			
			if(!Utils.setToStrContains(loreTypes, item) && !loreStrings.contains(item))
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
				String tName = Utils.formatAPIName(t.toString());
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
}
