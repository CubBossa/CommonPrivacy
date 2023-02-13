package de.cubbossa.commonprivacy;

import de.tr7zw.changeme.nbtapi.NBTFile;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CommonPrivacy extends JavaPlugin implements PrivacyDataHolder {

  private BukkitAudiences audiences;

  @Override
  public void onLoad() {
    CommandAPI.onLoad(new CommandAPIConfig()
        .verboseOutput(false)
        .silentLogs(true)
    );
  }

  @Override
  public void onEnable() {
    CommandAPI.onEnable(this);

    audiences = BukkitAudiences.create(this);
    registerCommand();

    registerBukkit();
  }

  @Override
  public void onDisable() {
    CommandAPI.onDisable();
  }

  public void registerCommand() {
    new CommandTree("commonprivacy")
        .withAliases("cp", "cprivacy", "privacy")
        .executes((sender, args) -> {
          audiences.sender(sender)
              .sendMessage(Component.text("-".repeat(9), NamedTextColor.YELLOW)
                  .append(Component.text(" Help: CommonPrivacy ", NamedTextColor.WHITE))
                  .append(Component.text("-".repeat(20), NamedTextColor.YELLOW))
                  .append(Component.newline())
                  .append(Component.text("/cp info", NamedTextColor.GOLD)
                      .hoverEvent(Component.text("Click to execute"))
                      .clickEvent(ClickEvent.runCommand("/cp info"))
                      .append(Component.text(": "))
                      .append(Component.text("Show details for registered services", NamedTextColor.WHITE)))
                  .append(Component.newline())
                  .append(Component.text("/cp info <user>", NamedTextColor.GOLD)
                      .hoverEvent(Component.text("Click to execute"))
                      .clickEvent(ClickEvent.suggestCommand("/cp info "))
                      .append(Component.text(": "))
                      .append(Component.text("Generate privacy data file for user", NamedTextColor.WHITE)))
                  .append(Component.newline())
                  .append(Component.text("/cp reset-user <user>", NamedTextColor.GOLD)
                      .hoverEvent(Component.text("Click to execute"))
                      .clickEvent(ClickEvent.runCommand("/cp reset-user "))
                      .append(Component.text(": "))
                      .append(Component.text("Deletes all user-specific data for given user", NamedTextColor.WHITE))));
        })
        .then(new LiteralArgument("info")
            .executes((sender, args) -> {
              infoPlugins(sender);
            })
            .then(new OfflinePlayerArgument("user")
                .executes((sender, args) -> {
                  infoUser(sender, (OfflinePlayer) args[0]);
                })
            )
        )
        .then(new LiteralArgument("reset-user")
            .executes((CommandExecutor) (sender, args) -> {
              throw CommandAPI.failWithAdventureComponent(Component.text("Please provide a user to reset user-data."));
            })
            .then(new OfflinePlayerArgument("user")
                .executes((sender, args) -> {
                  resetUser(sender, (OfflinePlayer) args[0]);
                })
            )
        )
        .register();
  }

  public void resetUser(CommandSender sender, OfflinePlayer player) {
    Audience audience = audiences.sender(sender);
    audience.sendMessage(
        Component.text("Resetting data for user with UUID " + player.getUniqueId()));

    Collection<CompletableFuture<?>> futures = new HashSet<>();
    for (PrivacyDataHolder dataHolder : getPrivacyDataHolders()) {
      futures.add(dataHolder.resetUserData(player.getUniqueId()));
    }
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
      audience.sendMessage(Component.text("Successfully deleted all data for user."));
    });
  }

  public void infoPlugins(CommandSender sender) {
    Component component = Component
        .text("Service Overview:")
        .append(Component.newline());
    for (PrivacyDataHolder privacyDataHolder : getPrivacyDataHolders()) {
      component = component
          .append(Component.text("• ", NamedTextColor.GRAY))
          .append(Component.text(privacyDataHolder.getService() + ": "))
          .append(switch (privacyDataHolder.getMode()) {
            case NOT_IMPLEMENTED -> Component.text("Not implemented", NamedTextColor.DARK_RED)
                .hoverEvent(HoverEvent.showText(
                    Component.text("Reach out to the plugin owner and ask for privacy support!")));
            case NOT_SUPPORTED -> Component.text("Not supported", NamedTextColor.RED)
                .hoverEvent(HoverEvent.showText(Component.text(
                    "The plugin does not have intentions to add support. You have to delete user-data of this plugin manually!")));
            case PARTLY_SUPPORTED -> Component.text("Partly Supported", NamedTextColor.YELLOW)
                .hoverEvent(HoverEvent.showText(Component.text(
                    "The plugin can only assure to partly remove user-data. You have to do some manual deleting.")));
            case FULLY_SUPPORTED -> Component.text("Supported", NamedTextColor.GREEN)
                .hoverEvent(HoverEvent.showText(Component.text(
                    "The plugin does fully support privacy and allows you to easily reset user-related data.")));
          })
          .append(Component.newline());
    }
    audiences.sender(sender).sendMessage(component);
  }

  public void infoUser(CommandSender sender, OfflinePlayer user) {
    Audience audience = audiences.sender(sender);
    audience.sendMessage(Component.text("Creating file for user with UUID " + user.getUniqueId()));

    createUserFile(user).thenAccept(file -> {
      audience.sendMessage(
          Component.text("Created file at location " + file.getAbsoluteFile().getPath()));
    });
  }

  public CompletableFuture<File> createUserFile(OfflinePlayer user) {
    try {
      Collection<PrivacyDataHolder> dataHolders = getPrivacyDataHolders();

      File dir = new File(getDataFolder(), "/user-data/");
      dir.mkdirs();

      CreateInfoFileTask task = new CreateInfoFileTask(dir, user.getUniqueId());
      return task.run(dataHolders);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private Collection<PrivacyDataHolder> getPrivacyDataHolders() {
    Collection<PrivacyDataHolder> dataHolders = new ArrayList<>(PrivacyDataHandler.getInstance()
        .getRegisteredDataHolders());
    for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
      if (dataHolders.stream()
          .anyMatch(dataHolder -> dataHolder.getService().equalsIgnoreCase(plugin.getName()))) {
        continue;
      }
      if (plugin instanceof PrivacyDataHolder dataHolder) {
        dataHolders.add(dataHolder);
      } else {
        dataHolders.add(new NoImplPrivacyHolder(plugin));
      }
    }
    return dataHolders;
  }

  @Override
  public String getService() {
    return getName();
  }

  @Override
  public PrivacyMode getMode() {
    return PrivacyMode.FULLY_SUPPORTED;
  }

  @Override
  public CompletableFuture<Map<String, Object>> requestUserData(UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      File dir = new File(getDataFolder(), "user-data");
      if (!dir.exists()) {
        return new HashMap<>();
      }

      Collection<String> fileList = new ArrayList<>();
      for (File file : dir.listFiles()) {
        if (file.getName().startsWith(uuid.toString())) {
          fileList.add(file.getAbsolutePath());
        }
      }
      return Map.of("user-files", fileList);
    });
  }

  @Override
  public CompletableFuture<Boolean> resetUserData(UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      File dir = new File(getDataFolder(), "user-data");
      if (!dir.exists()) {
        return true;
      }

      for (File file : dir.listFiles()) {
        if (file.getName().startsWith(uuid.toString())) {
          file.delete();
        }
      }
      return true;
    }).exceptionally(throwable -> false);
  }

  private void registerBukkit() {
    PrivacyDataHandler.getInstance().register(new PrivacyDataHolder() {
      @Override
      public String getService() {
        return "Bukkit";
      }

      @Override
      public PrivacyMode getMode() {
        return PrivacyMode.FULLY_SUPPORTED;
      }

      @Override
      public CompletableFuture<Map<String, Object>> requestUserData(UUID uuid) {

        // world -> type -> files (dat, dat_old)
        Map<String, Map<String, Collection<String>>> files = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
          File worldFile = new File(getServer().getWorldContainer(), world.getName() + "/");

          Map<String, Collection<String>> inner =
              files.computeIfAbsent(world.getName(), s -> new HashMap<>());

          File playerdata = new File(worldFile, "/playerdata/");
          File advancements = new File(worldFile, "/advancements/");
          File stats = new File(worldFile, "/stats/");
          Map.of(
              "playerdata", playerdata,
              "advancements", advancements,
              "stats", stats
          ).forEach((s, file) -> {
            if (file.listFiles() == null) {
              return;
            }
            inner.put(s,
                Arrays.stream(file.listFiles())
                    .filter(f -> f.getName().startsWith(uuid.toString().toLowerCase()))
                    .map(f -> {
                      if (f.getName().endsWith(".dat") || f.getName().endsWith(".dat_old")) {
                        try {
                          NBTFile nbtFile = new NBTFile(f);
                          return nbtFile.toString();
                        } catch (IOException e) {
                          throw new RuntimeException(e);
                        }
                      } else {
                        StringBuilder contentBuilder = new StringBuilder();

                        try (Stream<String> stream = Files.lines(f.toPath(),
                            StandardCharsets.ISO_8859_1)) {
                          stream.forEach(str -> contentBuilder.append(str).append("\n"));
                        } catch (IOException e) {
                          e.printStackTrace();
                        }
                        return contentBuilder.toString();
                      }
                    })
                    .collect(Collectors.toList()));
          });
        }
        return CompletableFuture.completedFuture(new HashMap<>(files));
      }

      @Override
      public CompletableFuture<Boolean> resetUserData(UUID uuid) {
        return null;
      }
    });
  }
}
