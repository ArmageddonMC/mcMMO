package com.gmail.nossr50.commands.skills;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import com.gmail.nossr50.commands.CommandManager;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.text.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandPermission("mcmmo.commands.mmopower")
@CommandAlias("mmopower|mmopowerlevel|powerlevel")
public class PowerLevelCommand extends BaseCommand {
    private final @NotNull mcMMO pluginRef;

    public PowerLevelCommand(@NotNull mcMMO pluginRef) {
        this.pluginRef = pluginRef;
    }

    @Default
    @Conditions(CommandManager.POWER_LEVEL_CONDITION)
    public void processCommand(String[] args) {
        BukkitCommandIssuer bukkitCommandIssuer = (BukkitCommandIssuer) getCurrentCommandIssuer();
        Player player = bukkitCommandIssuer.getPlayer();
        McMMOPlayer mmoPlayer = UserManager.getPlayer(player); //Should never be null at this point because its caught in an ACF validation
        if (mmoPlayer == null) return;

        int powerLevel = mmoPlayer.getPowerLevel();

        mmoPlayer.getPlayer().sendMessage(ChatColor.GOLD + "[mcMMO]" + ChatColor.DARK_AQUA + " Seu nível de poder é: " + ChatColor.GREEN + StringUtils.formatNumber(powerLevel));
    }
}
