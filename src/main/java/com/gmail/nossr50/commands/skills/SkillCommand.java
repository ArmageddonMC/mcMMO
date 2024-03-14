package com.gmail.nossr50.commands.skills;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.child.FamilyTree;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.NotificationManager;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.random.RandomChanceUtil;
import com.gmail.nossr50.util.scoreboards.ScoreboardManager;
import com.gmail.nossr50.util.skills.PerksUtils;
import com.gmail.nossr50.util.skills.RankUtils;
import com.gmail.nossr50.util.skills.SkillActivationType;
import com.gmail.nossr50.util.skills.SkillTools;
import com.gmail.nossr50.util.text.StringUtils;
import com.gmail.nossr50.util.text.TextComponentFactory;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public abstract class SkillCommand implements TabExecutor {
    protected PrimarySkillType skill;
    private final String skillName;

    protected DecimalFormat percent = new DecimalFormat("##0.00%");
    protected DecimalFormat decimal = new DecimalFormat("##0.00");

    private final CommandExecutor skillGuideCommand;

    protected SkillCommand(PrimarySkillType skill) {
        this.skill = skill;
        skillName = mcMMO.p.getSkillTools().getLocalizedSkillName(skill);
        skillGuideCommand = new SkillGuideCommand(skill);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LocaleLoader.getString("Commands.NoConsole"));
            return true;
        }

        if (!CommandUtils.hasPlayerDataKey(sender)) return true;

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);

        if (mcMMOPlayer == null) {
            sender.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return true;
        }

        if (args.length == 0) {
            boolean isLucky = Permissions.lucky(player, skill);
            boolean hasEndurance = PerksUtils.handleActivationPerks(player, 0, 0) != 0;
            float skillValue = mcMMOPlayer.getSkillLevel(skill);

            //Send the players a few blank lines to make finding the top of the skill command easier
            if (mcMMO.p.getAdvancedConfig().doesSkillCommandSendBlankLines()) {
                for (int i = 0; i < 2; i++) {
                    player.sendMessage("");
                }
            }

            permissionsCheck(player);
            dataCalculations(player, skillValue);

            sendSkillCommandHeader(player, mcMMOPlayer, (int) skillValue);

            //Make JSON text components
            List<Component> subskillTextComponents = getTextComponents(player);

            //Subskills Header
            player.sendMessage(LocaleLoader.getString("Skills.Overhaul.Header", LocaleLoader.getString("Effects.SubSkills.Overhaul")));

            //Send JSON text components
            TextComponentFactory.sendPlayerSubSkillList(player, subskillTextComponents);

            //Stats
            getStatMessages(player, isLucky, hasEndurance, skillValue);

            //Header
            //Link Header
            if (mcMMO.p.getGeneralConfig().getUrlLinksEnabled()) {
                player.sendMessage(LocaleLoader.getString("Overhaul.mcMMO.Header"));
                TextComponentFactory.sendPlayerUrlHeader(player);
            }


            if (mcMMO.p.getGeneralConfig().getScoreboardsEnabled() && mcMMO.p.getGeneralConfig().getSkillUseBoard()) {
                ScoreboardManager.enablePlayerSkillScoreboard(player, skill);
            }

            return true;
        } else if ("keep".equalsIgnoreCase(args[0])) {
            if (!mcMMO.p.getGeneralConfig().getAllowKeepBoard()
                    || !mcMMO.p.getGeneralConfig().getScoreboardsEnabled()
                    || !mcMMO.p.getGeneralConfig().getSkillUseBoard()) {
                sender.sendMessage(LocaleLoader.getString("Commands.Disabled"));
                return true;
            }

            ScoreboardManager.enablePlayerSkillScoreboard(player, skill);
            ScoreboardManager.keepBoard(sender.getName());
            sender.sendMessage(LocaleLoader.getString("Commands.Scoreboard.Keep"));
            return true;
        }

        return skillGuideCommand.onCommand(sender, command, label, args);
    }

    private void getStatMessages(Player player, boolean isLucky, boolean hasEndurance, float skillValue) {
        List<String> statsMessages = statsDisplay(player, skillValue, hasEndurance, isLucky);

        if (!statsMessages.isEmpty()) {
            player.sendMessage(LocaleLoader.getString("Skills.Overhaul.Header", LocaleLoader.getString("Commands.Stats.Self.Overhaul")));

            for (String message : statsMessages) {
                player.sendMessage(message);
            }
        }

        player.sendMessage(LocaleLoader.getString("Guides.Available", skillName, skillName.toLowerCase(Locale.ENGLISH)));
    }

    private void sendSkillCommandHeader(Player player, McMMOPlayer mcMMOPlayer, int skillValue) {
        player.sendMessage(LocaleLoader.getString("Skills.Overhaul.Header", skillName));

        if (!SkillTools.isChildSkill(skill)) {
            player.sendMessage(LocaleLoader.getString("Commands.XPGain.Overhaul", LocaleLoader.getString("Commands.XPGain." + StringUtils.getCapitalized(skill.toString()))));
            player.sendMessage(LocaleLoader.getString("Effects.Level.Overhaul", skillValue, mcMMOPlayer.getSkillXpLevel(skill), mcMMOPlayer.getXpToLevel(skill)));

        } else {
            Set<PrimarySkillType> parents = FamilyTree.getParents(skill);

            ArrayList<PrimarySkillType> parentList = new ArrayList<>(parents);

            StringBuilder parentMessage = new StringBuilder();

            for (int i = 0; i < parentList.size(); i++) {
                if (i + 1 < parentList.size()) {
                    parentMessage.append(LocaleLoader.getString("Effects.Child.ParentList", mcMMO.p.getSkillTools().getLocalizedSkillName(parentList.get(i)), mcMMOPlayer.getSkillLevel(parentList.get(i))));
                    parentMessage.append(ChatColor.GRAY).append(", ");
                } else {
                    parentMessage.append(LocaleLoader.getString("Effects.Child.ParentList", mcMMO.p.getSkillTools().getLocalizedSkillName(parentList.get(i)), mcMMOPlayer.getSkillLevel(parentList.get(i))));
                }
            }

            //XP GAIN METHOD
            player.sendMessage(LocaleLoader.getString("Commands.XPGain.Overhaul", LocaleLoader.getString("Commands.XPGain.Child")));
            player.sendMessage(LocaleLoader.getString("Effects.Child.Overhaul", skillValue, parentMessage.toString()));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("?").filter(s -> s.startsWith(args[0])).toList();
        }
        return List.of();
    }

    protected String[] getAbilityDisplayValues(SkillActivationType skillActivationType, Player player, SubSkillType subSkill) {
        return RandomChanceUtil.calculateAbilityDisplayValues(skillActivationType, player, subSkill);
    }

    protected String[] calculateLengthDisplayValues(Player player, float skillValue) {
        int maxLength = mcMMO.p.getSkillTools().getSuperAbilityMaxLength(mcMMO.p.getSkillTools().getSuperAbility(skill));
        int abilityLengthVar = mcMMO.p.getAdvancedConfig().getAbilityLength();
        int abilityLengthCap = mcMMO.p.getAdvancedConfig().getAbilityLengthCap();

        int length;

        if (abilityLengthCap <= 0) {
            length = 2 + (int) (skillValue / abilityLengthVar);
        } else {
            length = 2 + (int) (Math.min(abilityLengthCap, skillValue) / abilityLengthVar);
        }

        int enduranceLength = PerksUtils.handleActivationPerks(player, length, maxLength);

        if (maxLength != 0) {
            length = Math.min(length, maxLength);
        }

        return new String[]{String.valueOf(length), String.valueOf(enduranceLength)};
    }

    protected String getStatMessage(SubSkillType subSkillType, String... vars) {
        return getStatMessage(false, false, subSkillType, vars);
    }

    protected String getStatMessage(boolean isExtra, boolean isCustom, SubSkillType subSkillType, String... args) {
        String templateKey = isCustom ? "Ability.Generic.Template.Custom" : "Ability.Generic.Template";
        String statDescriptionKey = !isExtra ? subSkillType.getLocaleKeyStatDescription() : subSkillType.getLocaleKeyStatExtraDescription();

        if (isCustom)
            return LocaleLoader.getString(templateKey, LocaleLoader.getString(statDescriptionKey, (Object) args));
        else {
            String[] mergedList = NotificationManager.addItemToFirstPositionOfArray(LocaleLoader.getString(statDescriptionKey), args);
            return LocaleLoader.getString(templateKey, (Object) mergedList);
        }
    }

    protected abstract void dataCalculations(Player player, float skillValue);

    protected abstract void permissionsCheck(Player player);

    protected abstract List<String> statsDisplay(Player player, float skillValue, boolean hasEndurance, boolean isLucky);

    protected abstract List<Component> getTextComponents(Player player);

    /**
     * Checks if a player can use a skill
     *
     * @param player       target player
     * @param subSkillType target subskill
     * @return true if the player has permission and has the skill unlocked
     */
    protected boolean canUseSubskill(Player player, SubSkillType subSkillType) {
        return Permissions.isSubSkillEnabled(player, subSkillType) && RankUtils.hasUnlockedSubskill(player, subSkillType);
    }
}
