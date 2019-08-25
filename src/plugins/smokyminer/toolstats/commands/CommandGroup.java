package plugins.smokyminer.toolstats.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;

import plugins.smokyminer.toolstats.utils.Utils;

public class CommandGroup extends AbstractCommand
{
	private Map<String, AbstractCommand> subCommands;
	
	public CommandGroup()
	{
		super("ts", null, true, Utils.plugin);
		subCommands = new HashMap<String, AbstractCommand>();
	}
	
	public void registerCommand(AbstractCommand command)
	{
		if(command != null)
			subCommands.put(command.commandName.toLowerCase(), command);
	}
	
	public boolean unregisterCommand(AbstractCommand command)
	{
		if(!subCommands.containsKey(command.commandName.toLowerCase()))
			return false;
		
		subCommands.remove(command.commandName.toLowerCase());
		return true;
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) 
	{
		if(args.length < 1)
			return false;
		
		String lowerLabel = args[0].toLowerCase();
		if(!subCommands.containsKey(lowerLabel))
			return false;

		String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
		return subCommands.get(lowerLabel).onCommand(sender, args[0], newArgs);
	}
}
