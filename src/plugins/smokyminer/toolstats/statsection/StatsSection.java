package plugins.smokyminer.toolstats.statsection;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import plugins.smokyminer.toolstats.ToolGroup;
import plugins.smokyminer.toolstats.statsection.rewardsection.RewardSection;
import plugins.smokyminer.toolstats.utils.ConfigUtils;
import plugins.smokyminer.toolstats.utils.StringToType;
import plugins.smokyminer.toolstats.utils.Utils;

public class StatsSection<T> 
{
	public static final String totalWord = "Total";
	
	protected final ToolGroup parent;
	public final StringToType<T> stringToType;
	public final String eventSection, trackListName;
	public final String tagPrefix;
	
	protected boolean enabled, update;
	protected List<String> defaultLore;
	
	protected Map<String, TrackWord<T>> trackWords;
	protected String color, header, configPath;
	
	protected ArrayList<RewardSection<T>> rewards;
	
	protected List<T> orderedTypes;
	protected List<String> orderedStrings;
	
	protected final NamespacedKey headerKey, inLoreKey;
	protected Map<T, NamespacedKey> typeTags;
	protected Map<String, NamespacedKey> stringTags;
	
	public StatsSection()
	{
		parent = null;
		enabled = false;
		stringToType = null;
		eventSection = null;
		trackListName = null;
		tagPrefix = null;
		
		headerKey = null;
		inLoreKey = null;
	}
	
	public StatsSection(ToolGroup parent, String eventSection, String trackListName, StringToType<T> stringToType)
	{
		this.parent = parent;
		this.stringToType = stringToType;
		this.eventSection = eventSection;
		this.trackListName = trackListName;
		this.configPath = parent.getConfigPath() + "." + eventSection;
		this.tagPrefix = (parent.groupName + "." + eventSection + ".").replace(' ', '_');
		
		headerKey = new NamespacedKey(Utils.plugin, tagPrefix + "header");
		inLoreKey = new NamespacedKey(Utils.plugin, tagPrefix + "inLore");
		
		reload();
	}
	
	public boolean reload()
	{
		if(parent == null)
			return false;
		
		defaultLore = null;
		orderedTypes = null;
		orderedStrings = null;
		typeTags = null;
		stringTags = null;
		
		FileConfiguration config = parent.getFileConfiguration();
		enabled = ConfigUtils.loadBoolean(config, configPath, "Track Stats", true);
		update = ConfigUtils.loadBoolean(config, configPath, "Update Preexisting Tools", true);
		header = ConfigUtils.loadString(config, configPath, "Header");
		color = ConfigUtils.loadColor(config, configPath, parent.warningPrefix);
		
		if(header == null)
			Bukkit.getServer().getLogger().severe(parent.errorPrefix + "Ignoring \"" + eventSection + "\": missing config section \"Header\"!");
		else
		{
			String type = trackListName.substring(0, trackListName.length() - 1);
			
			// Get list of T trackers and get list of Keyword trackers
			List<String> stringItems = new ArrayList<String>();
			List<T> typeList = ConfigUtils.loadList(config, configPath, trackListName, (str) -> {
				T item = stringToType.toType(str);
				if(item == null)
				{
					String temp = (str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase()).trim();
					if(isStringItem(temp))
						stringItems.add(temp);
					else
						Bukkit.getServer().getLogger().severe(parent.warningPrefix + "\"" + str + "\" is not a valid " + type + "!");
				}
				return item;
			});
			
			if(typeList == null) 
				Bukkit.getServer().getLogger().severe(parent.errorPrefix + "\"" + eventSection + "\" missing config section \"" + trackListName + "\"!");
			else if(typeList.isEmpty())
				Bukkit.getServer().getLogger().severe(parent.errorPrefix + "\"" + eventSection + "\" list is empty!");
			else
				orderedTypes = typeList;
			
			if(stringItems.size() != 0)
			{
				buildTrackedWords(stringItems);
				orderedStrings = stringItems;
			}
			else
				this.trackWords = null;
		}
		
		defaultLore = Utils.buildLore(orderedTypes, orderedStrings, header, color, true);
		if(defaultLore == null)
			return false;
		
		typeTags = Utils.buildTags(orderedTypes, tagPrefix);
		stringTags = Utils.buildTags(orderedStrings, tagPrefix);
		
		loadRewards(config);
		
		return true;
	}
	
	private void loadRewards(FileConfiguration config)
	{
		rewards = new ArrayList<RewardSection<T>>();
		
		int i = 1;
		while(config.contains(configPath + ".Reward" + i))
		{
			rewards.add(new RewardSection<T>(this, configPath + ".Reward" + i, "Reward" + i));
			i++;
		}
	}
	
	public ToolGroup getParent()
	{
		return parent;
	}
	
	public boolean isEnabled()
	{
		return enabled && defaultLore != null;
	}
	
	public boolean isTracked(String trackItem)
	{
		T type = stringToType.toType(trackItem);
		if(type != null)
			if(typeTags != null)
				if(typeTags.containsKey(type))
					return true;
		
		if(trackWords != null)
			for(Map.Entry<String, TrackWord<T>> word : trackWords.entrySet())
				if(word.getValue().word.equalsIgnoreCase(trackItem))
					return true;
		
		return false;
	}
	
	public boolean isTracked(T trackItem)
	{
		if(typeTags != null)
			if(typeTags.containsKey(trackItem))
				return true;
		
		if(trackWords != null)
			for(Map.Entry<String, TrackWord<T>> word : trackWords.entrySet())
				if(word.getValue().isTracked(trackItem))
					return true;
		
		return false;
	}
	
	protected boolean isStringItem(String item)
	{
		if(item.equals(totalWord))
			return true;
		return false;
	}
	
	protected void buildTrackedWords(List<String> words)
	{
		trackWords = new HashMap<String, TrackWord<T>>();
		
		if(words.contains(totalWord))
			trackWords.put(totalWord, new TrackWord<T>(totalWord, (T m) -> {
				return true;
			}));
	}
	
	// ============= Getters =============
	
	@SuppressWarnings("unchecked")
	public List<T> getTrackList()
	{
		if(typeTags == null)
			return null;
		
		return  (List<T>) Arrays.asList(orderedTypes.toArray());
	}
	
	public String getHeader()
	{
		return header;
	}
	
	public boolean removeLore(ItemStack tool)
	{
		if(defaultLore == null)
			return false;
		
		ItemMeta tMeta = tool.getItemMeta();
		List<String> oldLore = tMeta.getLore();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();

		boolean toolTracked = container.has(inLoreKey, PersistentDataType.INTEGER);
		
		if(!toolTracked)
			return false;
		
		int inLore = container.get(inLoreKey,  PersistentDataType.INTEGER);
		if(inLore == 0)
			return false;

		String header = container.getOrDefault(headerKey, PersistentDataType.STRING, this.header);
		Map.Entry<Integer, Integer> startEnd = Utils.getHeaderPattern(oldLore, typeTags.keySet(), 
																	 (trackWords == null) ? null : trackWords.keySet(), 
																	  header, color);
	
		int startIndex = startEnd.getKey();
		int endIndex = startEnd.getValue();
		
		for(int i = startIndex; i < endIndex + 1; i++)
			oldLore.remove(startIndex);

		container.set(Utils.plugin.showKey, PersistentDataType.INTEGER, 0);
		container.set(inLoreKey, PersistentDataType.INTEGER, 0);
		tMeta.setLore(oldLore);
		tool.setItemMeta(tMeta);
		
		return true;
	}
	
	public boolean addLore(ItemStack tool)
	{
		if(defaultLore == null)
			return false;
		
		ItemMeta tMeta = tool.getItemMeta();
		List<String> oldLore = tMeta.getLore();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();

		boolean toolTracked = container.has(inLoreKey, PersistentDataType.INTEGER);
		
		if(!toolTracked)
			return false;
		
		int inLore = container.get(inLoreKey,  PersistentDataType.INTEGER);
		if(inLore == 1)
			return false;

		updateTool(tool, false);
		
		String header = container.getOrDefault(headerKey, PersistentDataType.STRING, this.header);
		if(!header.equals(this.header))
			container.set(headerKey, PersistentDataType.STRING, this.header);
		
		List<String> section = Utils.buildLore(container, typeTags, orderedTypes, color);
		section = Utils.addLore(section, Utils.buildLore(container, stringTags, orderedStrings, color));
		section.add("");
		section.add(0, color + this.header);
	
		List<String> newLore = Utils.addLore(oldLore, section, true);
		
		container.set(Utils.plugin.showKey, PersistentDataType.INTEGER, 1);
		container.set(inLoreKey, PersistentDataType.INTEGER, 1);
		tMeta.setLore(newLore);
		tool.setItemMeta(tMeta);
		
		return true;
	}
	
	public boolean updateCount(Player player, ItemStack tool, T item, boolean deleteUntracked)
	{
		if(defaultLore == null)
			return false;
		
		Map.Entry<Integer, Integer> startEnd = updateTool(tool, deleteUntracked);

		int startIndex = startEnd.getKey();
		int endIndex = startEnd.getValue();
		
		ItemMeta tMeta = tool.getItemMeta();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();
		
		boolean toolTracked = container.has(inLoreKey, PersistentDataType.INTEGER);
		
		if(isEnabled() && toolTracked)
		{
			List<String> oldLore = tMeta.getLore();
			
			boolean loreUpdated = false;
			
			boolean tagUpdated = false;
			NamespacedKey tag = typeTags.get(item);
			if(tag != null)
			{
				tagUpdated = Utils.updateTag(container, tag);
				if(!tagUpdated)
				{
					container.set(tag, PersistentDataType.INTEGER, 1);
					tagUpdated = true;
				}
					
			}
			if(startIndex != -1 && tagUpdated)
				loreUpdated = Utils.updateLore(oldLore, item, startIndex + 1, endIndex);
			
			if(trackWords != null)
			{
				for(Map.Entry<String, TrackWord<T>> entry : trackWords.entrySet())
				{
					if(entry.getValue().isTracked(item))
					{
						if(Utils.updateTag(container, stringTags.get(entry.getKey())))
							tagUpdated = true;
						if(startIndex != -1)
							if(Utils.updateLore(oldLore, entry.getKey(), startIndex + 1, endIndex))
								loreUpdated = true;
					}
				}
			}
			
			if(tagUpdated || loreUpdated)
			{
				if(loreUpdated)
					tMeta.setLore(oldLore);
				tool.setItemMeta(tMeta);
				
				if(tagUpdated)
					for(RewardSection<T> r : rewards)
						r.update(player, tool, item, typeTags, stringTags, startIndex + 1, endIndex);
			}
			
			return tagUpdated;
		}
		
		return false;
	}
	
	private Map.Entry<Integer, Integer> updateTool(ItemStack tool, boolean deleteUntracked)
	{
		if(defaultLore == null)
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		
		ItemMeta tMeta = tool.getItemMeta();
		List<String> oldLore = tMeta.getLore();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();
		
		boolean toolTracked = container.has(inLoreKey, PersistentDataType.INTEGER);
		
		int startIndex = -1;
		int endIndex = -1;
		
		if(!toolTracked)
		{
			if(!update)
				return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
			
			if(container.has(Utils.plugin.showKey, PersistentDataType.INTEGER))
			{
				boolean show = container.get(Utils.plugin.showKey, PersistentDataType.INTEGER) == 1;
				addTags(container, show);
				
				if(show)
				{
					List<String> newLore = addLore(container, oldLore);
					startIndex = newLore.size() - defaultLore.size();
					endIndex = newLore.size();
					oldLore = newLore;
				}
			}
			else
			{
				List<String> newLore = addLore(container, oldLore);
				startIndex = newLore.size() - defaultLore.size();
				endIndex = newLore.size();
				oldLore = newLore;
				
				container.set(Utils.plugin.showKey, PersistentDataType.INTEGER, 1);
				addTags(container, true);	
			}
			
			tMeta.setLore(oldLore);
			tool.setItemMeta(tMeta);
			
			return new AbstractMap.SimpleEntry<Integer, Integer>(startIndex, endIndex);
		}

		String header = container.getOrDefault(headerKey, PersistentDataType.STRING, this.header);
		int inLore = container.get(inLoreKey,  PersistentDataType.INTEGER);

		ArrayList<T> missingTypes = new ArrayList<T>();
		ArrayList<String> missingStrings = new ArrayList<String>();
		
		if(inLore != 0)
		{
			ArrayList<Integer> extraLore = new ArrayList<Integer>();
			
			Map.Entry<Integer, Integer> startEnd = Utils.getHeaderPattern(oldLore, typeTags.keySet(), 
																		 (trackWords == null) ? null : trackWords.keySet(), 
																		  header, color, missingTypes, missingStrings, extraLore);
	
			startIndex = startEnd.getKey();
			endIndex = startEnd.getValue();
			
			if(startIndex != -1)
			{
				for(int i = extraLore.size() - 1; i >= 0; i--)
				{
					oldLore.remove(extraLore.get(i).intValue());
					endIndex--;
				}
			}
		}
		else
		{
			for(Map.Entry<T, NamespacedKey> entry : typeTags.entrySet())
				if(!container.has(entry.getValue(), PersistentDataType.INTEGER))
					missingTypes.add(entry.getKey());
			for(Map.Entry<String, NamespacedKey> entry : stringTags.entrySet())
				if(!container.has(entry.getValue(), PersistentDataType.INTEGER))
					missingStrings.add(entry.getKey());
		}
		
		if(!isEnabled() && deleteUntracked)
		{
			for(Map.Entry<T, NamespacedKey> entry : typeTags.entrySet())
				container.remove(entry.getValue());
			for(Map.Entry<String, NamespacedKey> entry : stringTags.entrySet())
				container.remove(entry.getValue());
			
			container.remove(inLoreKey);
			container.remove(headerKey);
			
			if(startIndex != -1)
			{
				for(int i = startIndex; i < endIndex + 1; i++) // + 1 for the space line at the end
					oldLore.remove(startIndex);
				tMeta.setLore(oldLore);
			}
			
			tool.setItemMeta(tMeta);
			
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		}
		
		if(!header.equals(this.header))
		{
			container.set(headerKey, PersistentDataType.STRING, this.header);
			if(startIndex != -1)
				oldLore.set(startIndex, color + this.header);
		}
		
		if(!missingTypes.isEmpty())
		{
			for(T missing : missingTypes)
			{
				container.set(typeTags.get(missing), PersistentDataType.INTEGER, 0);
				if(startIndex != -1)
				{
					String name = Utils.formatAPIName(missing.toString());
					int index = orderedTypes.indexOf(missing);
					
					index += startIndex + 1;
					if(index > orderedTypes.size())
						index = orderedTypes.size();
					
					oldLore.add(index, color + name + ": 0");
				}
			}
		}
		
		if(!missingStrings.isEmpty())
		{
			for(String missing : missingStrings)
			{
				container.set(stringTags.get(missing), PersistentDataType.INTEGER, 0);
				if(startIndex != -1)
				{
					int index = orderedStrings.indexOf(missing);
					
					index += startIndex + 1;
					if(index > orderedTypes.size())
						index = orderedTypes.size();
					
					oldLore.add(index, color + missing + ": 0");
				}
			}
		}
		
		if(startIndex != -1)
			tMeta.setLore(oldLore);
		tool.setItemMeta(tMeta);
		return new AbstractMap.SimpleEntry<Integer, Integer>(startIndex, endIndex);
	}
	
	public void addTags(PersistentDataContainer container, boolean inLore)
	{
		if(defaultLore == null)
			return;
		
		container.set(inLoreKey, PersistentDataType.INTEGER, (inLore) ? 1 : 0);
		container.set(headerKey, PersistentDataType.STRING, header);
		
		for(Map.Entry<T, NamespacedKey> pair : typeTags.entrySet())
			container.set(pair.getValue(), PersistentDataType.INTEGER, 0);
		
		for(Map.Entry<String, NamespacedKey> pair : stringTags.entrySet())
			container.set(pair.getValue(), PersistentDataType.INTEGER, 0);
	}
	
	public boolean addTags(ItemMeta destination, ItemMeta source)
	{
		if(defaultLore == null)
			return false;
		
		PersistentDataContainer cSource = source.getPersistentDataContainer();
		PersistentDataContainer cDestination = destination.getPersistentDataContainer();
		
		if(!hasTags(cSource))
			return false;
		
		boolean destTags = hasTags(cDestination);
		
		if(!destTags)
			cDestination.set(inLoreKey, PersistentDataType.INTEGER, 0);
		
		String header = cSource.get(headerKey, PersistentDataType.STRING);
		cDestination.set(headerKey, PersistentDataType.STRING, header);
		
		for(NamespacedKey key : typeTags.values())
		{ 
			int val = cSource.getOrDefault(key, PersistentDataType.INTEGER, 0);
			val += cDestination.getOrDefault(key, PersistentDataType.INTEGER, 0);
			cDestination.set(key, PersistentDataType.INTEGER, val);
		}
		
		for(NamespacedKey key : stringTags.values())
		{
			int val = cSource.getOrDefault(key, PersistentDataType.INTEGER, 0);
			val += cDestination.getOrDefault(key, PersistentDataType.INTEGER, 0);
			cDestination.set(key, PersistentDataType.INTEGER, val);
		}
		
		return true;
	}
	
	public boolean hasTags(PersistentDataContainer container)
	{
		if(inLoreKey == null)
			return false;
		return container.has(inLoreKey, PersistentDataType.INTEGER);
	}
	
	public List<String> addLore(PersistentDataContainer container, List<String> lore)
	{
		List<String> newLore = Utils.addLore(lore, defaultLore, true);
		container.set(Utils.plugin.showKey, PersistentDataType.INTEGER, 1);
		container.set(inLoreKey, PersistentDataType.INTEGER, 1);
		return newLore;
	}
}
