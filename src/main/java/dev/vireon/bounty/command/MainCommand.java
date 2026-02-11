package dev.vireon.bounty.command;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.Default;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import dev.triumphteam.cmd.core.annotation.Suggestion;
import dev.vireon.bounty.BountyPlugin;
import dev.vireon.bounty.bounty.BountyIndex;
import dev.vireon.bounty.bounty.BountyResult;
import dev.vireon.bounty.gui.ConfirmGui;
import dev.vireon.bounty.gui.MainGui;
import dev.vireon.bounty.util.ChatUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command("bounty")
public class MainCommand extends BaseCommand {

    private final BountyPlugin plugin;

    public MainCommand(BountyPlugin plugin) {
        super(plugin.getConfig().getStringList("settings.commands"));
        this.plugin = plugin;
    }

    @Default
    public void onDefault(Player player) {
        MainGui.open(player, BountyIndex.SortField.AMOUNT, null, plugin);
    }

    @SubCommand("add")
    public void onAdd(Player player, String targetName, long amount) {
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        // Chỉ cho phép target là người chơi đã từng vào server (hoặc đang online)
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            ChatUtils.sendConfigMessage(player, plugin.getConfig(), "messages.player-not-found");
            return;
        }

        // Không cho tự đặt bounty lên bản thân
        if (target.getUniqueId().equals(player.getUniqueId())) {
            ChatUtils.sendConfigMessage(player, plugin.getConfig(), "messages.cannot-add-yourself");
            return;
        }

        boolean skipConfirm = plugin.getConfig().getBoolean("settings.skip-confirm-gui", false);
        if (skipConfirm) {
            BountyResult result = plugin.getBountyManager().addBounty(player, target.getUniqueId(), amount);
            ConfirmGui.handleResult(player, target, amount, result, plugin);
        } else {
            ConfirmGui.open(player, plugin, target, amount);
        }
    }

    @SubCommand("remove")
    @Permission("bounty.remove")
    public void onRemove(CommandSender sender, @Suggestion("online-players") String targetName) {
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore()) {
            ChatUtils.sendConfigMessage(sender, plugin.getConfig(), "messages.player-not-found");
            return;
        }

        BountyIndex bountyIndex = plugin.getBountyManager().getBountyMap();
        if (bountyIndex.remove(target.getUniqueId())) {
            ChatUtils.sendMessage(sender, ChatUtils.format(
                    plugin.getConfig().getString("messages.bounty-removed"),
                    Placeholder.unparsed("player", target.getName() == null ? "---" : target.getName())
            ));
        } else {
            ChatUtils.sendMessage(sender, ChatUtils.format(plugin.getConfig().getString("messages.no-bounty")));
        }
    }

    @SubCommand("reload")
    @Permission("bounty.reload")
    public void onReload(CommandSender sender) {
        plugin.getConfig().reload();
        ChatUtils.sendConfigMessage(sender, plugin.getConfig(), "messages.reload");
    }

}
