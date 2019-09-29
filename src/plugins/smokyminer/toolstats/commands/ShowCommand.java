package plugins.smokyminer.toolstats.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import plugins.smokyminer.toolstats.ToolGroup;
import plugins.smokyminer.toolstats.utils.Utils;

// TODO: Send player warning when they show/hide on a non-tool/non-tracked tool

public class ShowCommand extends AbstractCommand 
{
	public ShowCommand()
	{
		super("show", "toolstats.show", false, null);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) 
	{
		Player p = (Player) sender;
		
		ItemStack tool = p.getInventory().getItemInMainHand();
		if(tool == null || !Utils.plugin.isToolTracked(tool.getType()))
			tool = p.getInventory().getItemInOffHand();
		
		if(tool == null)
		{
			sender.sendMessage(ChatColor.RED + "This item is not tracked!");
			return true;
		}
		
		List<ToolGroup> groups = Utils.plugin.getToolGroups(tool.getType());
		
		if(groups == null)
		{
			sender.sendMessage(ChatColor.RED + "This item is not tracked!");
			return true;
		}
		
		@SuppressWarnings("unused")
		boolean success = false;
		for(ToolGroup group : groups)
			if(group.addLore(tool))
				success = true;
		
		return true; //success;
	}
}
