package plugins.smokyminer.toolstats.statsection;

import java.util.List;

import org.bukkit.entity.EntityType;

import plugins.smokyminer.toolstats.ToolGroup;
import plugins.smokyminer.toolstats.utils.StringToType;

public class KillSection extends StatsSection<EntityType>
{
	public static final String hostileWord = "Monster";
	public static final String passiveWord = "Animal";
	public static final String playerWord = "Player";
	
	public KillSection()
	{
		super();
	}
	
	public KillSection(ToolGroup parent, String eventSection, String trackListName, StringToType<EntityType> stringToType)
	{
		super(parent, eventSection, trackListName, stringToType);
	}
	
	public boolean isStringItem(String item)
	{
		if(super.isStringItem(item))
			return true;
		if(item.equals(hostileWord))
			return true;
		if(item.equals(passiveWord))
			return true;
		if(item.equals(playerWord))
			return true;
		
		return false;
	}
	
	protected void buildTrackedWords(List<String> words)
	{
		super.buildTrackedWords(words);
		
		if(words.contains(hostileWord))
			trackWords.put(hostileWord, new TrackWord<EntityType>(hostileWord, (EntityType m) -> {
				if(org.bukkit.entity.Monster.class.isAssignableFrom(m.getEntityClass()))
					return true;
				return false;
			}));
		
		if(words.contains(passiveWord))
			trackWords.put(passiveWord, new TrackWord<EntityType>(passiveWord, (EntityType m) -> {
				if(org.bukkit.entity.Animals.class.isAssignableFrom(m.getEntityClass()))
					return true;
				return false;
			}));
		
		if(words.contains(playerWord))
			trackWords.put(playerWord, new TrackWord<EntityType>(playerWord, (EntityType m) -> {
				if(org.bukkit.entity.Player.class.isAssignableFrom(m.getEntityClass()))
					return true;
				return false;
			}));
	}
}