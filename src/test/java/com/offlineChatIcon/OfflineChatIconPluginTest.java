package com.offlineChatIcon;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OfflineChatIconPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OfflineChatIconPlugin.class);
		RuneLite.main(args);
	}
}