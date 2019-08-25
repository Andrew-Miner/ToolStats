package plugins.smokyminer.toolstats.statsection;

import java.util.List;

import org.bukkit.Material;

import plugins.smokyminer.toolstats.ToolGroup;
import plugins.smokyminer.toolstats.utils.StringToType;

public class BreakSection extends StatsSection<Material>
{
	public static final String otherWord = "Other";
	
	public BreakSection()
	{
		super();
	}
	
	public BreakSection(ToolGroup parent, String eventSection, String trackListName, StringToType<Material> stringToType)
	{
		super(parent, eventSection, trackListName, stringToType);
	}
	
	public boolean isStringItem(String item)
	{
		if(super.isStringItem(item))
			return true;
		if(item.equals(otherWord))
			return true;
		
		return false;
	}
	
	protected void buildTrackedWords(List<String> words)
	{
		super.buildTrackedWords(words);
		
		if(words.contains(otherWord))
			trackWords.put(otherWord, new TrackWord<Material>(otherWord, (Material m) -> {
				if(typeTags != null && !typeTags.containsKey(m))
					return true;
				return false;
			}));
	}
}
