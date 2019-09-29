package plugins.smokyminer.toolstats.statsection;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import plugins.smokyminer.toolstats.utils.LoreUtils;
import plugins.smokyminer.toolstats.utils.StringToType;
import plugins.smokyminer.toolstats.utils.TagUtils;
import plugins.smokyminer.toolstats.utils.Utils;

public class StatsSection<T> 
{
	public static final String prefix = "\u25C6 ";
	public static final String totalWord = "Total";
	
	protected final ToolGroup parent;
	public final StringToType<T> stringToType;
	public final String eventSection, trackListName;
	public final String tagPrefix;
	
	protected boolean enabled, update;
	protected List<String> defaultLore;
	
	protected Map<String, TrackWord<T>> trackWords;
	protected String color, header, configPath, countColor;
	
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
		countColor = ConfigUtils.loadString(config, configPath, "Count Color");
		
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
			else if(!typeList.isEmpty())
				orderedTypes = typeList;
			else
				orderedTypes = new ArrayList<T>();
			
			if(typeList.isEmpty() && stringItems.isEmpty())
				Bukkit.getServer().getLogger().severe(parent.errorPrefix + "\"" + eventSection + "\" list is empty!");
			
			if(stringItems.size() != 0)
			{
				buildTrackedWords(stringItems);
				orderedStrings = stringItems;
			}
			else
			{
				this.trackWords = new HashMap<String, TrackWord<T>>();
				orderedStrings = new ArrayList<String>();
			}
		}
		
		defaultLore = LoreUtils.buildDefaultLore(orderedTypes, orderedStrings, header, color, countColor, prefix, true);
		if(defaultLore == null)
			return false;
		
		typeTags = TagUtils.buildTags(orderedTypes, tagPrefix);
		stringTags = TagUtils.buildTags(orderedStrings, tagPrefix);
		
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
		Map.Entry<Integer, Integer> startEnd = LoreUtils.getHeaderPattern(oldLore, typeTags.keySet(),
																	 (trackWords == null) ? null : trackWords.keySet(), 
																	 ChatColor.translateAlternateColorCodes('&', header), prefix);
	
		int startIndex = startEnd.getKey();
		int endIndex = startEnd.getValue();
		
		if(startIndex != -1 && endIndex < oldLore.size())
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

		validateTool(tool, false);
		
		String header = container.getOrDefault(headerKey, PersistentDataType.STRING, this.header);
		if(!header.equals(this.header))
			container.set(headerKey, PersistentDataType.STRING, this.header);
		
		List<String> section = LoreUtils.buildLore(container, typeTags, orderedTypes, color, countColor, prefix);
		section = LoreUtils.addLore(section, LoreUtils.buildLore(container, stringTags, orderedStrings, color, countColor, prefix));
		section.add("");
		section.add(0, color + ChatColor.translateAlternateColorCodes('&', this.header));
	
		List<String> newLore = LoreUtils.addLore(oldLore, section, true);
		
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
		
		Map.Entry<Integer, Integer> startEnd = validateTool(tool, deleteUntracked);

		int startIndex = startEnd.getKey();
		int endIndex = startEnd.getValue();
		
		ItemMeta tMeta = tool.getItemMeta();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();
		
		boolean toolTracked = container.has(inLoreKey, PersistentDataType.INTEGER);
		
		if(isEnabled() && toolTracked)
		{
			// Update Material's Count
			
			// Update material's tag
			boolean tagUpdated = false;
			NamespacedKey tag = typeTags.get(item);
			if(tag != null)
			{
				tagUpdated = TagUtils.updateTag(container, tag);
				if(!tagUpdated)
				{
					container.set(tag, PersistentDataType.INTEGER, 1);
					tagUpdated = true;
				}
					
			}
			
			// Update material's lore
			boolean loreUpdated = false;
			List<String> oldLore = tMeta.getLore();
			if(startIndex != -1 && tagUpdated)
				loreUpdated = LoreUtils.updateLore(oldLore, item, startIndex + 1, 
												   endIndex, color, countColor, prefix, 
												   ChatColor.translateAlternateColorCodes('&', header));
			
			// Update TrackWords' Count
			if(trackWords != null)
			{
				for(Map.Entry<String, TrackWord<T>> entry : trackWords.entrySet())
				{
					if(entry.getValue().isTracked(item))
					{
						// Update TrackWords' tag
						NamespacedKey word = stringTags.get(entry.getKey());
						if(TagUtils.updateTag(container, word))
							tagUpdated = true;
						else if(word != null)
						{
							container.set(word, PersistentDataType.INTEGER, 1);
							tagUpdated = true;
						}
						
						// Update TrackWords' lore
						if(startIndex != -1)
							if(LoreUtils.updateLore(oldLore, entry.getKey(), startIndex + 1, 
											        endIndex, color, countColor, prefix, 
											        ChatColor.translateAlternateColorCodes('&', header)))
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
	
	// Ensures the tool is in a valid state and is ready to be processed by updateTool
	private Map.Entry<Integer, Integer> validateTool(ItemStack tool, boolean deleteUntracked)
	{
		if(defaultLore == null)
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);

		// Remove stats if necessary
		if(!isEnabled() && deleteUntracked)
		{
			removeStats(tool);
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		}
		
		
		ItemMeta tMeta = tool.getItemMeta();
		boolean toolTracked = isTracked(tMeta.getPersistentDataContainer());
		
		// Add stats if necessary
		if(!toolTracked)
		{
			if(!isEnabled() || !update)
				return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
			
			return addDefaultStats(tool);
		}
		
		return fixStats(tool);
	}
	
	public boolean isTracked(PersistentDataContainer container)
	{
		if(inLoreKey == null)
			return false;
		return container.has(inLoreKey, PersistentDataType.INTEGER);
	}
	
	public boolean combineTags(ItemMeta destination, ItemMeta source)
	{
		if(defaultLore == null)
			return false;
		
		PersistentDataContainer cSource = source.getPersistentDataContainer();
		PersistentDataContainer cDestination = destination.getPersistentDataContainer();
		
		if(!isTracked(cSource))
			return false;
		
		boolean destTags = isTracked(cDestination);
		
		if(!destTags)
			cDestination.set(inLoreKey, PersistentDataType.INTEGER, 0);
		
		TagUtils.combineTags(cDestination, cSource, headerKey, typeTags.values(), stringTags.values());
		
		return true;
	}
	
	public boolean removeStats(ItemStack tool)
	{
		ItemMeta tMeta = tool.getItemMeta();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();
		
		if(!isTracked(container))
			return false;
		
		List<String> oldLore = tMeta.getLore();
		
		for(Map.Entry<T, NamespacedKey> entry : typeTags.entrySet())
			container.remove(entry.getValue());
		for(Map.Entry<String, NamespacedKey> entry : stringTags.entrySet())
			container.remove(entry.getValue());
		
		container.remove(inLoreKey);
		container.remove(headerKey);
		
		Map.Entry<Integer, Integer> startEnd = LoreUtils.getHeaderPattern(oldLore, typeTags.keySet(), 
				 							   							 (trackWords == null) ? null : trackWords.keySet(), 
				 							   							  ChatColor.translateAlternateColorCodes('&', header), prefix);

		int startIndex = startEnd.getKey();
		int endIndex = startEnd.getValue();
		
		if(startIndex != -1)
		{
			for(int i = startIndex; i < endIndex + 1; i++) // + 1 for the space line at the end
				oldLore.remove(startIndex);
			tMeta.setLore(oldLore);
		}
		
		tool.setItemMeta(tMeta);
		return true;
	}
	
	public SimpleEntry<Integer, Integer> addDefaultStats(ItemStack tool)
	{
		ItemMeta tMeta = tool.getItemMeta();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();
		
		List<String> oldLore = tMeta.getLore();
		
		int startIndex = -1;
		int endIndex = -1;
		
		if(container.has(Utils.plugin.showKey, PersistentDataType.INTEGER))
		{
			boolean show = container.get(Utils.plugin.showKey, PersistentDataType.INTEGER) == 1;
			
			if(show)
			{
				List<String> newLore = addDefaultLore(container, oldLore);
				startIndex = newLore.size() - defaultLore.size();
				endIndex = newLore.size();
				oldLore = newLore;
			}
			
			addDefaultTags(container, show);
		}
		else
		{
			List<String> newLore = addDefaultLore(container, oldLore);
			startIndex = newLore.size() - defaultLore.size();
			endIndex = newLore.size();
			oldLore = newLore;
			
			addDefaultTags(container, true);	
		}
		
		tMeta.setLore(oldLore);
		tool.setItemMeta(tMeta);
		
		return new AbstractMap.SimpleEntry<Integer, Integer>(startIndex, endIndex);
	}
	
	private List<String> addDefaultLore(PersistentDataContainer container, List<String> lore)
	{
		List<String> newLore = LoreUtils.addLore(lore, defaultLore, true);
		container.set(Utils.plugin.showKey, PersistentDataType.INTEGER, 1);
		container.set(inLoreKey, PersistentDataType.INTEGER, 1);
		return newLore;
	}
	
	private void addDefaultTags(PersistentDataContainer container, boolean inLore)
	{
		if(defaultLore == null)
			return;
		
		if(inLore)
			container.set(Utils.plugin.showKey, PersistentDataType.INTEGER, 1);
		
		container.set(inLoreKey, PersistentDataType.INTEGER, (inLore) ? 1 : 0);
		container.set(headerKey, PersistentDataType.STRING, header);
		
		TagUtils.addDefaultTags(container, typeTags.values(), stringTags.values());
	}
	
	private SimpleEntry<Integer, Integer> fixStats(ItemStack tool)
	{
		ItemMeta tMeta = tool.getItemMeta();
		PersistentDataContainer container = tMeta.getPersistentDataContainer();
		
		if(!isTracked(container))
			return new AbstractMap.SimpleEntry<Integer, Integer>(-1, -1);
		
		List<String> oldLore = tMeta.getLore();
		
		ArrayList<T> missingTypes = new ArrayList<T>();
		ArrayList<String> missingStrings = new ArrayList<String>();
		ArrayList<Integer> extraLore = new ArrayList<Integer>();
		
		int startIndex = -1;
		int endIndex = -1;

		String header = container.getOrDefault(headerKey, PersistentDataType.STRING, this.header);
		int inLore = container.get(inLoreKey,  PersistentDataType.INTEGER);
		
		if(inLore != 0) // Get all missing items from lore
		{
			// Gets Lore start and end indices
			// Also returns lists of missing and extra lore items
			Map.Entry<Integer, Integer> startEnd = LoreUtils.getHeaderPattern(oldLore, typeTags.keySet(), 
																		 	  (trackWords == null) ? null : trackWords.keySet(), 
																		 	  ChatColor.translateAlternateColorCodes('&', header), prefix, 
																		 	  missingTypes, missingStrings, extraLore);
	
			startIndex = startEnd.getKey();
			endIndex = startEnd.getValue();
			
			// Remove extra lore items
			if(startIndex != -1)
			{
				for(int i = extraLore.size() - 1; i >= 0; i--)
				{
					oldLore.remove(extraLore.get(i).intValue());
					endIndex--;
				}
			}
			
			// Fix Header
			if(!header.equals(this.header))
			{
				container.set(headerKey, PersistentDataType.STRING, this.header);
				if(startIndex != -1)
					oldLore.set(startIndex, color + ChatColor.translateAlternateColorCodes('&', this.header));
			}
		}
		else // Get all missing items from tags
		{
			for(Map.Entry<T, NamespacedKey> entry : typeTags.entrySet())
				if(!container.has(entry.getValue(), PersistentDataType.INTEGER))
					missingTypes.add(entry.getKey());
			for(Map.Entry<String, NamespacedKey> entry : stringTags.entrySet())
				if(!container.has(entry.getValue(), PersistentDataType.INTEGER))
					missingStrings.add(entry.getKey());
		}
		
		String countColor = "";
		if(this.countColor != null)
			countColor = ChatColor.translateAlternateColorCodes('&', this.countColor);
		
		AbstractMap.SimpleEntry<Integer, Integer> ret;
		
		ret = Utils.fixStats(container, 
					   		 oldLore, 
					   		 startIndex, 
					   		 endIndex, 
					   		 color, 
					   		 countColor, 
					   		 prefix,
					   		 missingTypes, 
					   		 orderedTypes, 
					   		 typeTags);
		
		ret = Utils.fixStats(container, 
					   		 oldLore, 
					   		 ret.getKey() + orderedTypes.size(), 
					   		 ret.getValue(), 
					   		 color, 
					   		 countColor, 
					   		 prefix, 
					   		 missingStrings, 
					   		 orderedStrings, 
					   		 stringTags);
		
		if(startIndex != -1)
			tMeta.setLore(oldLore);
		tool.setItemMeta(tMeta);
		
		return new AbstractMap.SimpleEntry<Integer, Integer>(ret.getKey() - orderedTypes.size(), ret.getValue());
	}
}
