package plugins.smokyminer.toolstats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractCommand implements CommandExecutor 
{
	public final String commandName;
	public final String permission;
	public final boolean canConsoleUse;
	public final JavaPlugin plugin;
	
	public AbstractCommand(String commandName, String permission, boolean canConsoleUse, JavaPlugin plugin)
	{
		this.commandName = commandName;
		this.permission = permission;
		this.canConsoleUse = canConsoleUse;
		this.plugin = plugin;
		
		if(plugin != null)
			plugin.getCommand(commandName).setExecutor(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String str, String[] args)
	{
		if(!cmd.getLabel().equalsIgnoreCase(commandName))
			return false;
		
		if(permission != null && !sender.hasPermission(permission) && !sender.isOp())
		{
			sender.sendMessage("You don't have permission to use this command.");
			return true;
		}
		
		if(!canConsoleUse && !(sender instanceof Player))
		{
			sender.sendMessage("Only players can use this command!");
			return true;
		}
		
		return execute(sender, args);
	}
	
	public boolean onCommand(CommandSender sender, String cmd, String[] args)
	{
		if(!cmd.equalsIgnoreCase(commandName))
			return false;
		
		if(permission != null && !sender.hasPermission(permission) && !sender.isOp())
		{
			sender.sendMessage("You don't have permission to use this command.");
			return true;
		}
		
		if(!canConsoleUse && !(sender instanceof Player))
		{
			sender.sendMessage("Only players can use this command!");
			return true;
		}
		
		return execute(sender, args);
	}
	
	public abstract boolean execute(CommandSender sender, String[] args);
}
