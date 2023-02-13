package de.cubbossa.commonprivacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

public class CreateInfoFileTask {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
  private final Logger logger;
  private final FileHandler fileHandler;
  private final String fileName;
  private final UUID uuid;
  private final File directory;

  public CreateInfoFileTask(File directory, UUID uuid, Handler... handlers) throws IOException {
    this.uuid = uuid;
    this.directory = directory;
    String dateString = DATE_FORMAT.format(new Date());
    fileName = uuid.toString().toLowerCase() + "_" + dateString;

    logger = Logger.getLogger(CreateInfoFileTask.class.getName());
    fileHandler = new FileHandler(new File(directory, fileName + ".report.log").getAbsolutePath());
    fileHandler.setFormatter(new LogFormatter("[%1$tT %4$s]: %5$s%6$s%n"));
    logger.addHandler(fileHandler);
    for (Handler handler : handlers) {
      logger.addHandler(handler);
    }

    LogManager.getLogManager().addLogger(logger);

    logSeparator();
    logger.info(" STARTING PRIVACY DATA REQUEST");
    logSeparator();
  }

  private void logSeparator() {
    logger.info("=".repeat(100));
  }

  public CompletableFuture<File> run(Collection<PrivacyDataHolder> dataHolders)
      throws IllegalArgumentException, IllegalStateException, IOException {
    try {

      if (directory == null || !directory.isDirectory()) {
        throw new IllegalArgumentException(
            "Directory must not be null and must be a directory file.");
      }
      if (directory.mkdirs()) {
        logger.info("Created user-data directory.");
      }
      File file = new File(directory, fileName + ".yml");
      if (!file.createNewFile()) {
        throw new IllegalStateException("Cannot create file, it already exists");
      }
      YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
      logger.info("Successfully created data file.");

      Collection<CompletableFuture<?>> futures = new HashSet<>();

      // all loading
      for (PrivacyDataHolder dataHolder : dataHolders) {
        logger.info("Starting data access for service '" + dataHolder.getService() + "'.");

        if (dataHolder.getMode() != PrivacyDataHolder.PrivacyMode.FULLY_SUPPORTED) {
          logger.warning("Service '" + dataHolder.getService() + "' is not fully supported!"
              + "Some additional steps might be necessary.");
        }

        futures.add(dataHolder.requestUserData(uuid).thenAccept(map -> {

          cfg.set(dataHolder.getService(), map);
          logger.info(
              "Service '" + dataHolder.getService() + "' has been loaded. Storing results...");
          try {
            cfg.save(file);
          } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred while reading user data for service '" +
                dataHolder.getService() + "'.", e);
          }
          logger.info("Service '" + dataHolder.getService() + "' successfully stored.");

        }).exceptionally(throwable -> {
          logger.log(Level.SEVERE, "An error occurred while reading user data for service '" +
              dataHolder.getService() + "'.", throwable);
          return null;
        }));
      }

      // summary
      return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(unused -> {
        logSeparator();
        logger.info("Summary");
        logSeparator();

        fileHandler.close();
        return file;
      });

    } catch (Throwable t) {
      logger.log(Level.SEVERE, "An error occured while creating user-data file.", t);
      throw t;
    }
  }
}
