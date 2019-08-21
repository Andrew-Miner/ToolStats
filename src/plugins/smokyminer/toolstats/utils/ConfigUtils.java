package plugins.smokyminer.toolstats.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;


public class ConfigUtils 
{
	public static File createFile(JavaPlugin plugin, String fileName)
	{
		try
		{
			File temp = new File(plugin.getDataFolder(), fileName);
			
			if(!temp.exists())
				plugin.getLogger().info(fileName + " File Not Found! Creating...");
			else
				plugin.getLogger().info("Loading " + fileName + " File!");
			
			return temp;
		} 
		catch (Exception e) 
		{ 
			plugin.getLogger().severe(Utils.warningPrefix + "Failed To Create " + fileName + " File!");
			e.printStackTrace(); 
		}
		
		return null;
	}
	
	public static boolean fileExists(JavaPlugin plugin, String fileName)
	{
		try
		{
			File temp = new File(plugin.getDataFolder(), fileName);
			
			if(!temp.exists())
				return false;
			return true;
		} 
		catch (Exception e) 
		{ 
			plugin.getLogger().severe(Utils.warningPrefix + "Failed To Load " + fileName + " File!");
			e.printStackTrace(); 
		}
		
		return false;
	}
	
	public static FileConfiguration loadFileConfiguration(JavaPlugin plugin, File configFile)
	{
		if(!configFile.exists())
			plugin.saveResource(configFile.getName(), false);
		
		return YamlConfiguration.loadConfiguration(configFile);
	}
	
	public static FileConfiguration loadFileConfiguration(JavaPlugin plugin, File configFile, String defResource)
	{
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		Reader defConfigStream = null;
		
		try {
			defConfigStream = new InputStreamReader(plugin.getResource(defResource),"UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		if(defConfigStream != null)
		{
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			config.setDefaults(defConfig);
		}
		
		if(!configFile.exists())
		{
			try {
				config.options().copyDefaults(true);
				config.save(configFile);
				plugin.getLogger().info(Utils.logPrefix + "Default config for " + configFile.getName() + " created from: " + defResource);
			} catch (IOException e) {
				plugin.getLogger().severe(Utils.warningPrefix + "Could not save config to " + configFile);
				e.printStackTrace();
			}
		}
		
		
		return config;
	}
	
	public static boolean loadBoolean(FileConfiguration config, String configPath, String configName)
	{
		if(config.contains(configPath + "." + configName))
			return config.getBoolean(configPath + "." + configName);
		else
			return false;
	}
	
	public static boolean loadBoolean(FileConfiguration config, String configPath, String configName, boolean defaultRet)
	{
		if(config.contains(configPath + "." + configName))
			return config.getBoolean(configPath + "." + configName);
		else
			return defaultRet;
	}
	
	public static String loadString(FileConfiguration config, String configPath, String configName)
	{
		if(config.contains(configPath + "." + configName))
			return config.getString(configPath + "." + configName);
		else
			return null;
	}
	
	public static Integer loadInteger(FileConfiguration config, String configPath, String configName)
	{
		if(config.contains(configPath + "." + configName))
			return config.getInt(configPath + "." + configName);
		else
			return null;
	}
	
	public static List<String> loadWorlds(FileConfiguration config, String configPath)
	{
		if(config.contains(configPath + ".World"))
			return Arrays.asList(config.getString(configPath + ".World").split(";"));
		else
			return null;
	}
	

	public static ArrayList<String> loadStrList(FileConfiguration config, String configPath, String listPath, StringToType<String> modifier)
	{
		if(config.contains(configPath + "." + listPath))
		{
			ArrayList<String> items = new ArrayList<String>();
			List<String> itemStrs = config.getStringList(configPath + "." + listPath);
			
			for(String i : itemStrs)
			{
				String newItem = modifier.toType(i);
				if(newItem != null)
					items.add(newItem);
			}
			
			return items;
		}
		
		return null;
	}
	
	public static <T> ArrayList<T> loadList(FileConfiguration config, String configPath, String listPath, StringToType<T> strToType)
	{
		if(config.contains(configPath + "." + listPath))
		{
			ArrayList<T> items = new ArrayList<T>();
			List<String> itemStrs = config.getStringList(configPath + "." + listPath);
			
			for(String i : itemStrs)
			{
				T newItem = strToType.toType(i.toUpperCase().replace(" ", "_"));
				if(newItem != null)
					items.add(newItem);
			}
			
			return items;
		}
		
		return null;
	}
	
	public static <T> ArrayList<T> loadList(FileConfiguration config, String configPath, String listPath, StringToType<T> strToType, String warnPrefix, String typeName)
	{
		if(config.contains(configPath + "." + listPath))
		{
			ArrayList<T> items = new ArrayList<T>();
			List<String> itemStrs = config.getStringList(configPath + "." + listPath);
			
			for(String i : itemStrs)
			{
				T newItem = strToType.toType(i.toUpperCase().replace(" ", "_"));
				if(newItem != null)
					items.add(newItem);
				else
				{
					if(typeName != null)
						Bukkit.getServer().getLogger().severe(warnPrefix + "\"" + i + "\" is not a valid " + typeName + "!");
					else
						Bukkit.getServer().getLogger().severe(warnPrefix + "\"" + i + "\" is not valid!");
				}
			}
			
			return items;
		}
		
		return null;
	}
	
	public static <K, V> HashMap<K, V> loadMap(FileConfiguration config, String configPath, String listPath, 
											   StringToType<K> strToKey, StringToType<V> strToValue, String warnPrefix, String entryName)
	{
		if(config.contains(configPath + "." + listPath))
		{
			HashMap<K, V> items = new HashMap<K, V>();
			List<String> itemStrs = config.getStringList(configPath + "." + listPath);
			
			for(String i : itemStrs)
			{
				String[] split = i.split(":");
				if(split.length != 2)
				{
					Bukkit.getServer().getLogger().severe(warnPrefix + "\"" + i + "\" is not a valid " + entryName + "!");
					continue;
				}
				
				split[0] = split[0].toUpperCase().trim().replace(' ', '_');
				split[1] = split[1].trim();
				
				K key = strToKey.toType(split[0]);
				if(key == null)
					continue;
				
				V val = strToValue.toType(split[1]);
				if(val == null)
					continue;
				
				items.put(key, val);
			}
			
			return items;
		}
		
		return null;
	}
	

	public static String loadColor(FileConfiguration config, String configPath)
	{
		return loadColor(config, configPath, Utils.warningPrefix);
	}
	
	public static String loadColor(FileConfiguration config, String configPath, String warnPrefix)
	{
		if(config.contains(configPath + ".Color"))
			return Utils.convertColorCode(config.getString(configPath + ".Color").split(";"));
		else
		{
			String split[] = configPath.split("\\.");
			if(split.length == 0)
				Bukkit.getServer().getLogger().warning(warnPrefix + "\"" + configPath + "\" missing config section \"Color\"!");
			else
				Bukkit.getServer().getLogger().warning(warnPrefix + "\"" + split[split.length - 1] + "\" missing config section \"Color\"!");
			return ChatColor.LIGHT_PURPLE.toString();
		}
	}
}
