package de.cubbossa.commonprivacy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PrivacyDataHolder {

  enum PrivacyMode {
    FULLY_SUPPORTED,
    PARTLY_SUPPORTED,
    NOT_SUPPORTED,
    NOT_IMPLEMENTED
  }

  /**
   * @return The service name, most likely the plugin name.
   */
  String getService();

  /**
   * @return The support mode. Set to PARTLY_SUPPORTED, if it cannot be guaranteed that all user-related data is being
   * deleted. Set to NOT_SUPPORTED, if your plugin won't be capable of resetting user-related data.
   * Set to FULLY_SUPPORTED, if your plugin does not store user-related data or all data is being deleted.
   */
  PrivacyMode getMode();

  /**
   * Returns all user-related data as completable future.
   * The value of the returned map has to be either a primitive, a primitive array or a map with the same requirements
   * as its parent.
   *
   * @param uuid The uuid of the player to request data for.
   * @return A completable future that returns a map with all user data.
   */
  CompletableFuture<Map<String, Object>> requestUserData(UUID uuid);

  /**
   * Reset all user-related data and complete the returned CompletableFuture once the user data was cleared.
   *
   * @param uuid The uuid of the player to reset all data of.
   * @return A completable future that completes once all data was cleared.
   */
  CompletableFuture<Boolean> resetUserData(UUID uuid);
}
