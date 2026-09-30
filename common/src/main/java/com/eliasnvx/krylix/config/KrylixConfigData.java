package com.eliasnvx.krylix.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class KrylixConfigData {
    @ConfigEntry.BoundedDiscrete(min = 1, max = 300)
    public int displaySeconds = 15;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int maxEntries = 5;
    public boolean killFeedEnabled = true;
    public boolean mobStatsEnabled = true;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    public int mobStatsMaxEntries = 8;
    public boolean restrictBroadcastToSameDimension = false;
    public boolean healthIndicatorEnabled = true;
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HealthBarStyle healthBarStyle = HealthBarStyle.HEARTS;
}
