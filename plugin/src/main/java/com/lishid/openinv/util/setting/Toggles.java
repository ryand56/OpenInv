package com.lishid.openinv.util.setting;

import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class Toggles {

  private static final String TOGGLE_ANYCONTAINER_BASE = "toggles.any-chest.";
  private static final String TOGGLE_SILENTCONTAINER_BASE = "toggles.silent-chest.";

  private final PlayerToggle any = new PlayerToggle() {
    @Override
    public boolean is(UUID uuid) {
      return configuration.getBoolean(TOGGLE_ANYCONTAINER_BASE + uuid, false);
    }

    @Override
    public void set(UUID uuid, boolean value) {
      configuration.set(TOGGLE_ANYCONTAINER_BASE + uuid, value);
      save();
    }
  };

  private final PlayerToggle silent = new PlayerToggle() {
    @Override
    public boolean is(UUID uuid) {
      return configuration.getBoolean(TOGGLE_SILENTCONTAINER_BASE + uuid, false);
    }

    @Override
    public void set(UUID uuid, boolean value) {
      configuration.set(TOGGLE_SILENTCONTAINER_BASE + uuid, value);
      save();
    }
  };

  private @NotNull MemoryConfiguration configuration;

  public Toggles() {
    this.configuration = new MemoryConfiguration();
  }

  public PlayerToggle any() {
    return any;
  }

  public PlayerToggle silent() {
    return silent;
  }

  public void reload(@NotNull MemoryConfiguration configuration) {
    this.configuration = configuration;
  }

  public abstract void save();

}
