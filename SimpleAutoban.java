package dev.simpleautoban;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Date;

@Mod(SimpleAutoban.MODID)
public class SimpleAutoban {
    public static final String MODID = "simpleautoban";
    private static final Config config = new Config();

    public SimpleAutoban() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (config.enabled && event.getEntity() instanceof ServerPlayer player) {
            String username = player.getGameProfile().getName().toLowerCase();
            if (!config.whitelist.contains(username)) {
                String ip = player.getIpAddress();
                
                // Standard-Kicknachricht
                player.connection.disconnect(Component.literal("You are not whitelisted on this server"));
                
                // Ban mit konfigurierbarem Grund
                UserBanList nameBanList = player.server.getPlayerList().getBans();
                nameBanList.add(new UserBanListEntry(
                    player.getGameProfile(),
                    new Date(),
                    "SimpleAutoBan",
                    null,
                    config.nameBanReason // Verwendet den konfigurierbaren Grund
                ));
                
                IpBanList ipBanList = player.server.getPlayerList().getIpBans();
                ipBanList.add(new IpBanListEntry(
                    ip,
                    new Date(),
                    "SimpleAutoBan",
                    null,
                    "IP not whitelisted" // Standard-IP-Ban-Grund
                ));
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("autoban")
                .requires(source -> source.hasPermission(3))
                // Whitelist Management
                .then(Commands.literal("add")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .executes(context -> {
                            String username = StringArgumentType.getString(context, "username");
                            config.whitelist.add(username.toLowerCase());
                            config.save();
                            context.getSource().sendSuccess(
                                () -> Component.literal("§aAdded §e" + username + " §ato whitelist"), 
                                true
                            );
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .executes(context -> {
                            String username = StringArgumentType.getString(context, "username");
                            if (config.whitelist.remove(username.toLowerCase())) {
                                config.save();
                                context.getSource().sendSuccess(
                                    () -> Component.literal("§aRemoved §e" + username + " §afrom whitelist"), 
                                    true
                                );
                            } else {
                                context.getSource().sendSuccess(
                                    () -> Component.literal("§c" + username + " was not in whitelist"), 
                                    true
                                );
                            }
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("list")
                    .executes(context -> {
                        if (config.whitelist.isEmpty()) {
                            context.getSource().sendSuccess(
                                () -> Component.literal("§eWhitelist is empty"),
                                true
                            );
                        } else {
                            context.getSource().sendSuccess(
                                () -> Component.literal("§aWhitelisted players:"),
                                true
                            );
                            config.whitelist.forEach(name -> 
                                context.getSource().sendSuccess(
                                    () -> Component.literal("§7- §e" + name),
                                    false
                                )
                            );
                        }
                        return 1;
                    })
                )
                // System Control
                .then(Commands.literal("on")
                    .executes(context -> {
                        config.enabled = true;
                        config.save();
                        context.getSource().sendSuccess(
                            () -> Component.literal("§aAutoBan activated"), 
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        config.enabled = false;
                        config.save();
                        context.getSource().sendSuccess(
                            () -> Component.literal("§cAutoBan deactivated"), 
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("status")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.literal("AutoBan is currently " + 
                                (config.enabled ? "§aenabled" : "§cdisabled")),
                            true
                        );
                        return 1;
                    })
                )
                // Ban Reason Configuration
                .then(Commands.literal("setreason")
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> {
                            config.nameBanReason = StringArgumentType.getString(context, "reason");
                            config.save();
                            context.getSource().sendSuccess(
                                () -> Component.literal("§aBan reason set to: §r" + config.nameBanReason),
                                true
                            );
                            return 1;
                        })
                    )
                )
        );
    }

    public static class Config {
        public boolean enabled = false;
        public String nameBanReason = "Not whitelisted";
        public final Set<String> whitelist = new HashSet<>();
        private final Path configPath = Paths.get("config/simpleautoban.json");

        public Config() {
            load();
        }

        public void load() {
            try {
                if (!Files.exists(configPath)) {
                    save();
                    return;
                }
                JsonObject json = JsonParser.parseReader(new FileReader(configPath.toFile())).getAsJsonObject();
                enabled = json.get("enabled").getAsBoolean();
                nameBanReason = json.get("nameBanReason").getAsString();
                whitelist.clear();
                json.getAsJsonArray("whitelist").forEach(e -> 
                    whitelist.add(e.getAsString().toLowerCase()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void save() {
            try {
                Files.createDirectories(configPath.getParent());
                JsonObject json = new JsonObject();
                json.addProperty("enabled", enabled);
                json.addProperty("nameBanReason", nameBanReason);
                JsonArray list = new JsonArray();
                whitelist.forEach(list::add);
                json.add("whitelist", list);
                try (FileWriter writer = new FileWriter(configPath.toFile())) {
                    new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}