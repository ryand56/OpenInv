package com.lishid.openinv.util.setting;

import java.util.UUID;

public interface PlayerToggle {

  boolean is(UUID uuid);

  void set(UUID uuid, boolean value);

}
