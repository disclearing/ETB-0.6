package net.minecraft.command;

import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldServer;

public class CommandTrigger extends CommandBase
{
  private static final String __OBFID = "CL_00002337";
  
  public CommandTrigger() {}
  
  public String getCommandName()
  {
    return "trigger";
  }
  



  public int getRequiredPermissionLevel()
  {
    return 0;
  }
  
  public String getCommandUsage(ICommandSender sender)
  {
    return "commands.trigger.usage";
  }
  
  public void processCommand(ICommandSender sender, String[] args) throws CommandException
  {
    if (args.length < 3)
    {
      throw new WrongUsageException("commands.trigger.usage", new Object[0]);
    }
    
    EntityPlayerMP var3;
    
    EntityPlayerMP var3;
    if ((sender instanceof EntityPlayerMP))
    {
      var3 = (EntityPlayerMP)sender;
    }
    else
    {
      net.minecraft.entity.Entity var4 = sender.getCommandSenderEntity();
      
      if (!(var4 instanceof EntityPlayerMP))
      {
        throw new CommandException("commands.trigger.invalidPlayer", new Object[0]);
      }
      
      var3 = (EntityPlayerMP)var4;
    }
    
    Scoreboard var8 = MinecraftServer.getServer().worldServerForDimension(0).getScoreboard();
    ScoreObjective var5 = var8.getObjective(args[0]);
    
    if ((var5 != null) && (var5.getCriteria() == IScoreObjectiveCriteria.field_178791_c))
    {
      int var6 = parseInt(args[2]);
      
      if (!var8.func_178819_b(var3.getName(), var5))
      {
        throw new CommandException("commands.trigger.invalidObjective", new Object[] { args[0] });
      }
      

      Score var7 = var8.getValueFromObjective(var3.getName(), var5);
      
      if (var7.func_178816_g())
      {
        throw new CommandException("commands.trigger.disabled", new Object[] { args[0] });
      }
      

      if ("set".equals(args[1]))
      {
        var7.setScorePoints(var6);
      }
      else
      {
        if (!"add".equals(args[1]))
        {
          throw new CommandException("commands.trigger.invalidMode", new Object[] { args[1] });
        }
        
        var7.increseScore(var6);
      }
      
      var7.func_178815_a(true);
      
      if (theItemInWorldManager.isCreative())
      {
        notifyOperators(sender, this, "commands.trigger.success", new Object[] { args[0], args[1], args[2] });
      }
      

    }
    else
    {
      throw new CommandException("commands.trigger.invalidObjective", new Object[] { args[0] });
    }
  }
  

  public java.util.List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
  {
    if (args.length == 1)
    {
      Scoreboard var4 = MinecraftServer.getServer().worldServerForDimension(0).getScoreboard();
      ArrayList var5 = com.google.common.collect.Lists.newArrayList();
      Iterator var6 = var4.getScoreObjectives().iterator();
      
      while (var6.hasNext())
      {
        ScoreObjective var7 = (ScoreObjective)var6.next();
        
        if (var7.getCriteria() == IScoreObjectiveCriteria.field_178791_c)
        {
          var5.add(var7.getName());
        }
      }
      
      return getListOfStringsMatchingLastWord(args, (String[])var5.toArray(new String[var5.size()]));
    }
    

    return args.length == 2 ? getListOfStringsMatchingLastWord(args, new String[] { "add", "set" }) : null;
  }
}
