package com.offlineChatIcon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("offlineChatIcon")
public interface OfflineChatIconConfig extends Config
{

    @ConfigItem(
            keyName = "enableOfflineIcon",
            name = "Show offline icon",
            description = "Display an icon next to offline clan members' names",
            position = 0
    )
    default boolean enableOfflineIcon() { return true; }

    @ConfigItem(
            keyName = "enableOfflineColor",
            name = "Enable offline color",
            description = "Change the color of offline clan members' names",
            position = 1
    )
    default boolean enableOfflineColor()
    {
        return true;
    }

    @ConfigItem(
            keyName = "offlineColor",
            name = "Offline color",
            description = "Choose the color for offline clan members' names",
            position = 2
    )
    default Color offlineColor()
    {
        return Color.DARK_GRAY;
    }
}