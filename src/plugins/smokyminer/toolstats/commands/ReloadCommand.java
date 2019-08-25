package plugins.smokyminer.toolstats.commands;

import org.bukkit.command.CommandSender;

import plugins.smokyminer.toolstats.utils.Utils;

public class ReloadCommand  extends AbstractCommand 
{
	public ReloadCommand()
	{
		super("reload", "toolstats.reload", true, null);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) 
	{
		if(args.length > 0)
			return false;
		Utils.plugin.reload();
		return true;
	}
}