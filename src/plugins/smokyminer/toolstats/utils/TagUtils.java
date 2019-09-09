package plugins.smokyminer.toolstats.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class TagUtils 
{
	public static <T> HashMap<T, NamespacedKey> buildTags(List<T> tagTypes, String tagPrefix)
	{
		if(tagTypes == null || tagTypes.isEmpty())
			return new HashMap<T, NamespacedKey>();
		
		HashMap<T, NamespacedKey> tags = new HashMap<T, NamespacedKey>();
		
		for(T t : tagTypes)
			tags.put(t, new NamespacedKey(Utils.plugin, (tagPrefix + t.toString()).replace(' ', '_')));
		
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

	@SafeVarargs
	public static void addDefaultTags(PersistentDataContainer container, Collection<NamespacedKey> ... keys)
	{
		if(keys == null)
			return;
		
		for(Collection<NamespacedKey> collection : keys)
		{
			if(collection == null || !collection.isEmpty())
				continue;
			
			for(NamespacedKey key : collection)
				container.set(key, PersistentDataType.INTEGER, 0);
		}
	}
	
	@SafeVarargs
	public static void combineTags(PersistentDataContainer destination, PersistentDataContainer source, NamespacedKey headerKey, Collection<NamespacedKey> ... keys)
	{
		if(source.has(headerKey, PersistentDataType.STRING))
		{
			String header = source.get(headerKey, PersistentDataType.STRING);
			destination.set(headerKey, PersistentDataType.STRING, header);
		}
		
		if(keys == null)
			return;
		
		for(Collection<NamespacedKey> collection : keys)
		{
			if(collection == null)
				continue;
			
			for(NamespacedKey key : collection)
			{
				int val = source.getOrDefault(key, PersistentDataType.INTEGER, 0);
				val += destination.getOrDefault(key, PersistentDataType.INTEGER, 0);
				destination.set(key, PersistentDataType.INTEGER, val);
			}
		}
	}
}
