package de.cubbossa.commonprivacy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PrivacyDataHandler {

  private static volatile PrivacyDataHandler instance;
  private static Object mutex = new Object();

  public static PrivacyDataHandler getInstance() {
    PrivacyDataHandler result = instance;
    if (result == null) {
      synchronized (mutex) {
        result = instance;
        if (result == null) {
          instance = result = new PrivacyDataHandler();
        }
      }
    }
    return result;
  }

  private final Map<String, PrivacyDataHolder> dataHolderMap;

  private PrivacyDataHandler() {
    dataHolderMap = new HashMap<>();
  }

  /**
   * Register a service as PrivacyDataHolder. This means, that the service processes and keeps track
   * of user-specific data. Registering an implemented PrivacyDataHolder interface allows administrators
   * to easily manage all services and their user data.
   *
   * @param dataHolder The implemented interface
   * @throws IllegalArgumentException If another service with the same name has already been registered.
   */
  public synchronized void register(PrivacyDataHolder dataHolder)
      throws IllegalArgumentException {
    if (dataHolderMap.containsKey(dataHolder.getService())) {
      throw new IllegalArgumentException("Another service with the key '" + dataHolder.getService()
          + "' has already been registered.");
    }
    dataHolderMap.put(dataHolder.getService(), dataHolder);
  }

  /**
   * Unregister a DataHolder.
   *
   * @param dataHolder The PrivacyDataHolder instance that has already been registered.
   * @return true if successfully unregistered.
   */
  public synchronized boolean unregister(PrivacyDataHolder dataHolder) {
    return dataHolderMap.remove(dataHolder.getService(), dataHolder);
  }

  public Collection<PrivacyDataHolder> getRegisteredDataHolders() {
    return dataHolderMap.values();
  }
}
