package net.samagames.uhcrun.commands;

import net.samagames.uhcrun.game.BasicGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Created by vialarl on 16/01/2015.
 */
public class CommandNextEvent implements CommandExecutor {

    private BasicGame game;

    public CommandNextEvent(BasicGame game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        game.getGameLoop().forceNextEvent();
        return true;
    }
}