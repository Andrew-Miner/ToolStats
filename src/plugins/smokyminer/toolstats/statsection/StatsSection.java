package plugins.smokyminer.toolstats.statsection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
	
	protected boolean enabled, update;
	protected List<String> defaultLore;
	protected Set<T> trackList;
	
	protected Map<String, TrackWord<T>> trackWords;
	protected String color, header, configPath;
	
	protected ArrayList<RewardSection<T>> rewards;
	
	public StatsSection()
	{
		parent = null;
		enabled = false;
		trackList = null;
		stringToType = null;
		eventSection = null;
		trackListName = null;
	}
	
	public StatsSection(ToolGroup parent, String eventSection, String trackListName, StringToType<T> stringToType)
	{
		this.parent = parent;
		this.stringToType = stringToType;
		this.eventSection = eventSection;
		this.trackListName = trackListName;
		this.configPath = parent.getConfigPath() + "." + eventSection;
		
		reload();
	}
	
	public boolean reload()
	{
		if(parent == null)
			return false;
		
		FileConfiguration config = parent.getFileConfiguration();
		enabled = ConfigUtils.loadBoolean(config, configPath, "Track Stats", true);
		update = ConfigUtils.loadBoolean(config, configPath, "Update Preexisting Tools", true);
		header = ConfigUtils.loadString(config, configPath, "Header");
		color = ConfigUtils.loadColor(config, configPath, parent.warningPrefix);
		
		List<String> stringList = null;
		List<T> typeList = null;
		
		if(header == null)
			Bukkit.getServer().getLogger().severe(parent.errorPrefix + "Ignoring \"" + eventSection + "\": missing config section \"Header\"!");
		else
		{
			String type = trackListName.substring(0, trackListName.length() - 1);
			
			// Get list of T trackers and get list of Keyword trackers
			List<String> stringItems = new ArrayList<String>();
			typeList = ConfigUtils.loadList(config, configPath, trackListName, (str) -> {
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
			
			if(typeList == null)
				trackList = null;
			else
				trackList = new HashSet<T>(typeList);
			
			if(stringItems.size() != 0)
			{
				buildTrackedWords(stringItems);
				stringList = stringItems;
			}
			else
				this.trackWords = null;
		}
		
		defaultLore = Utils.buildLore(typeList, stringList, header, color, true);
		if(defaultLore == null)
			return false;
		
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
			if(trackList != null)
				if(trackList.contains(type))
					return true;
		
		if(trackWords != null)
			for(Map.Entry<String, TrackWord<T>> word : trackWords.entrySet())
				if(word.getValue().word.equalsIgnoreCase(trackItem))
					return true;
		
		return false;
	}
	
	public boolean isTracked(T trackItem)
	{
		if(trackList != null)
			if(trackList.contains(trackItem))
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
		if(trackList == null)
			return null;
		
		return  (List<T>) Arrays.asList(trackList.toArray());
	}
	
	public String getHeader()
	{
		return header;
	}
	
	public boolean updateLore(Player player, ItemStack tool, T item, boolean deleteUntracked)
	{
		if(defaultLore == null)
			return false;
		
		ItemMeta tMeta = tool.getItemMeta();
		List<String> oldLore = tMeta.getLore();
		
		// Get lore start and end indices, and update tool lore if it's outdated and update is true
		Map.Entry<Integer, Integer> startEnd = Utils.getHeaderPattern(oldLore, trackList, 
																	  (trackWords == null) ? null : trackWords.keySet(), 
																	  header, update, color);
		
		int startIndex = startEnd.getKey();
		int endIndex = startEnd.getValue();

		if(isEnabled())
		{
			// If no lore was found add it
			if(startIndex == -1)
			{
				if(!update)
					return false;
				
				List<String> newLore = Utils.addLore(oldLore, defaultLore);
				startIndex = newLore.size() - defaultLore.size();
				endIndex = newLore.size();
				oldLore = newLore;
			}
			

			boolean success = false;
			
			// Update if item is on track list
			if(trackList.contains(item)) 
				if(Utils.updateLore(oldLore, item, startIndex + 1, endIndex)) // + 1 for header line
					success = true;
			
			 // Update TrackWord if item effects it
			if(trackWords != null)
				for(Entry<String, TrackWord<T>> word : trackWords.entrySet())
					if(word.getValue().isTracked(item))
						if(Utils.updateLore(oldLore, word.getKey(), startIndex + 1, endIndex))
							success = true;
			
			if(update || success)
			{
				tMeta.setLore(oldLore);
				tool.setItemMeta(tMeta);
				
				if(success)
					for(RewardSection<T> r : rewards)
						r.update(player, tool, item, oldLore, startIndex + 1, endIndex);
			}
			
			return success;
		}
		else if(deleteUntracked && startIndex != -1)
		{
			for(int i = startIndex; i < endIndex + 1; i++) // + 1 for the space line at the end
				oldLore.remove(startIndex);
			tMeta.setLore(oldLore);
			tool.setItemMeta(tMeta);
		}
		
		return false;
	}
}
