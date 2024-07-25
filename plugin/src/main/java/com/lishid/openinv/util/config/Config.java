package com.lishid.openinv.util.config;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;

public class Config {

  private @NotNull Configuration root;

  public Config() {
    root = new MemoryConfiguration();
  }

  public void reload(@NotNull Configuration configuration) {
    root = configuration;
  }

  public boolean isSaveDisabled() {
    return root.getBoolean("settings.disable-saving", false);
  }

  public boolean isOfflineDisabled() {
    return root.getBoolean("settings.disable-offline-access", false);
  }

  public boolean doesNoArgsOpenSelf() {
    return root.getBoolean("settings.command.open.no-args-opens-self", false);
  }

}
