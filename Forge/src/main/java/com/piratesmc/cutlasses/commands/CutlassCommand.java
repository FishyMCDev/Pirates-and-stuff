package com.piratesmc.cutlasses.commands;

import com.piratesmc.cutlasses.CutlassVariant;
import com.piratesmc.cutlasses.CutlassesPlugin;
import com.piratesmc.cutlasses.PirateFruit;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CutlassCommand implements CommandExecutor, TabCompleter {

    private final CutlassesPlugin plugin;

    public CutlassCommand(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6FishyPirateItems §7v" + plugin.getPluginMeta().getVersion());
            sender.sendMessage("§7/cutlass give <player> <cutlass|grappling_hook|fruit>");
            sender.sendMessage("§7/cutlass reload");
            sender.sendMessage("§7/cutlass info");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> give(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("piratesmc.cutlass.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                plugin.cfg().reload();
                sender.sendMessage("§aConfig reloaded. §7Recipes update on server restart to avoid Canvas recipe reload crashes.");
            }
            case "info" -> info(sender);
            default -> sender.sendMessage("§cUnknown sub-command: " + args[0]);
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("piratesmc.cutlass.give")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /cutlass give <player> <cutlass|grappling_hook|fruit>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }

        ItemStack item;
        String displayName;
        CutlassVariant variant = CutlassVariant.fromKey(args[2]);
        if (variant != null) {
            item = plugin.cutlasses().createCutlass(variant);
            displayName = variant.getDisplayName();
        } else if (args[2].equalsIgnoreCase("grappling_hook") || args[2].equalsIgnoreCase("grapple")) {
            item = plugin.cutlasses().createGrapplingHook();
            displayName = "§6Grappling Hook";
        } else {
            PirateFruit fruit = PirateFruit.fromKey(args[2]);
            if (fruit == null) {
                sender.sendMessage("§cUnknown pirate item: " + args[2]);
                sender.sendMessage("§7Cutlasses: " + Arrays.toString(CutlassVariant.values()));
                sender.sendMessage("§7Items: grappling_hook, " + fruitNames());
                return;
            }
            item = plugin.cutlasses().createFruit(fruit);
            displayName = fruit.getDisplayName();
        }

        target.getInventory().addItem(item);
        sender.sendMessage("§aGave " + displayName + " §ato " + target.getName());
    }

    private void info(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cPlayers only.");
            return;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        CutlassVariant variant = plugin.cutlasses().getVariant(hand);
        if (variant != null) {
            sender.sendMessage("§6Cutlass: §f" + variant.getDisplayName());
            sender.sendMessage("§7  Base: §f" + variant.getBaseItem().name());
            sender.sendMessage("§7  Damage: §f" + plugin.cfg().getDamage(variant));
            sender.sendMessage("§7  Max block hits: §f" + plugin.cfg().getMaxHits(variant));
            sender.sendMessage("§7  Stamina cooldown: §f" + plugin.cfg().getStaminaCooldownSeconds() + "s");
            sender.sendMessage("§7  Model ID: §f" + plugin.cfg().getModelId(variant)
                    + (plugin.cfg().isModelsEnabled() ? " §a(enabled)" : " §c(disabled)"));
            return;
        }

        PirateFruit fruit = plugin.cutlasses().getFruit(hand);
        if (plugin.cutlasses().isGrapplingHook(hand)) {
            sender.sendMessage("§6Pirate item: §fGrappling Hook");
            sender.sendMessage("§7  Base: §fCARROT_ON_A_STICK");
            sender.sendMessage("§7  Cooldown: §f" + plugin.cfg().getGrapplingHookCooldownSeconds() + "s");
            sender.sendMessage("§7  Model ID: §f" + plugin.cfg().getGrapplingHookModelId()
                    + (plugin.cfg().isModelsEnabled() ? " §a(enabled)" : " §c(disabled)"));
        } else if (fruit != null) {
            sender.sendMessage("§6Pirate fruit: §f" + fruit.getDisplayName());
            sender.sendMessage("§7  Base: §f" + fruit.getBaseItem().name());
            sender.sendMessage("§7  Heal: §f" + plugin.cfg().getFruitHeal(fruit));
            sender.sendMessage("§7  Model ID: §f" + plugin.cfg().getFruitModelId(fruit)
                    + (plugin.cfg().isModelsEnabled() ? " §a(enabled)" : " §c(disabled)"));
        } else {
            sender.sendMessage("§7You're not holding a pirate item.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"give", "reload", "info"}) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[2].toLowerCase();
            for (CutlassVariant v : CutlassVariant.values()) {
                if (v.name().toLowerCase().startsWith(prefix)) out.add(v.name().toLowerCase());
            }
            if ("grappling_hook".startsWith(prefix)) out.add("grappling_hook");
            for (PirateFruit fruit : PirateFruit.values()) {
                if (fruit.getConfigKey().startsWith(prefix)) out.add(fruit.getConfigKey());
            }
        }
        return out;
    }

    private String fruitNames() {
        List<String> names = new ArrayList<>();
        for (PirateFruit fruit : PirateFruit.values()) {
            names.add(fruit.getConfigKey());
        }
        return String.join(", ", names);
    }
}
