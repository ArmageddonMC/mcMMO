package com.gmail.nossr50.commands.party;

import com.gmail.nossr50.datatypes.party.ItemShareType;
import com.gmail.nossr50.datatypes.party.Party;
import com.gmail.nossr50.datatypes.party.PartyFeature;
import com.gmail.nossr50.datatypes.party.ShareMode;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.text.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class PartyItemShareCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LocaleLoader.getString("Commands.NoConsole"));
            return true;
        }

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);

        if (mcMMOPlayer == null) {
            sender.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return true;
        }

        Party party = mcMMOPlayer.getParty();
        if (party == null) {
            return true;
        }

        if (party.getLevel() < mcMMO.p.getGeneralConfig().getPartyFeatureUnlockLevel(PartyFeature.ITEM_SHARE)) {
            sender.sendMessage(LocaleLoader.getString("Party.Feature.Disabled.4"));
            return true;
        }

        switch (args.length) {
            case 2:
                ShareMode mode = ShareMode.getShareModeName(args[1].toUpperCase(Locale.ENGLISH));

                if (mode == null) {
                    sender.sendMessage(LocaleLoader.getString("Commands.Usage.2", "party", "itemshare", "<nenhum | igual | aleatorio>"));
                    return true;
                }

                handleChangingShareMode(party, mode);
                return true;

            case 3:
                boolean toggle;

                if (CommandUtils.shouldEnableToggle(args[2])) {
                    toggle = true;
                } else if (CommandUtils.shouldDisableToggle(args[2])) {
                    toggle = false;
                } else {
                    sender.sendMessage(LocaleLoader.getString("Commands.Usage.2", "party", "itemshare", "<saque | mineracao | herbalismo | lenhador | outros> <sim | nao>"));
                    return true;
                }

                try {
                    handleToggleItemShareCategory(party, ItemShareType.valueOf(args[1].toUpperCase(Locale.ENGLISH)), toggle);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(LocaleLoader.getString("Commands.Usage.2", "party", "itemshare", "<saque | mineracao | herbalismo | lenhador | outros> <sim | nao>"));
                }

                return true;

            default:
                sender.sendMessage(LocaleLoader.getString("Commands.Usage.2", "party", "itemshare", "<nenhum | igual | aleatorio>"));
                sender.sendMessage(LocaleLoader.getString("Commands.Usage.2", "party", "itemshare", "<saque | mineracao | herbalismo | lenhador | outros> <sim | nao>"));
                return true;
        }
    }

    private void handleChangingShareMode(Party party, ShareMode mode) {
        party.setItemShareMode(mode);

        String changeModeMessage = LocaleLoader.getString("Commands.Party.SetSharing", LocaleLoader.getString("Party.ShareType.Item"), LocaleLoader.getString("Party.ShareMode." + StringUtils.getCapitalized(mode.toString())));

        for (Player member : party.getOnlineMembers()) {
            member.sendMessage(changeModeMessage);
        }
    }

    private void handleToggleItemShareCategory(Party party, ItemShareType type, boolean toggle) {
        party.setSharingDrops(type, toggle);

        String toggleMessage = LocaleLoader.getString("Commands.Party.ToggleShareCategory", StringUtils.getCapitalized(type.toString()), toggle ? "ativar" : "desativar");

        for (Player member : party.getOnlineMembers()) {
            member.sendMessage(toggleMessage);
        }
    }
}
