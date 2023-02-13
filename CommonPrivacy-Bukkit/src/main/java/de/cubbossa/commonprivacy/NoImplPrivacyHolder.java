package de.cubbossa.commonprivacy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.Plugin;

public class NoImplPrivacyHolder implements PrivacyDataHolder {

  private Plugin plugin;

  public NoImplPrivacyHolder(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getService() {
    return plugin.getName();
  }

  @Override
  public PrivacyMode getMode() {
    return PrivacyMode.NOT_IMPLEMENTED;
  }

  @Override
  public CompletableFuture<Map<String, Object>> requestUserData(UUID uuid) {
    return CompletableFuture.completedFuture(new HashMap<>());
  }

  @Override
  public CompletableFuture<Boolean> resetUserData(UUID uuid) {
    return CompletableFuture.completedFuture(false);
  }
}
