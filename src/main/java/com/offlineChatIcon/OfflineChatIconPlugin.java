package com.offlineChatIcon;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

@Slf4j
@PluginDescriptor(
		name = "Offline Chat Icon"
)
public class OfflineChatIconPlugin extends Plugin
{
	@Inject
	private Client client;

	private int offlineIconLocation = -1;
	private String iconImg;
	@Inject
	private ClientThread clientThread;

	@Inject
	private OfflineChatIconConfig config;

	@Inject
	private ConfigManager configManager;

	@Provides
	OfflineChatIconConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OfflineChatIconConfig.class);
	}
	//	Format on startup
	@Override
	protected void startUp() throws Exception
	{
		clientThread.invoke(() ->
		{
			if (client.getModIcons() == null)
			{
				return false;
			}
			loadIcon();
			formatAllMessages();
			return true;
		});
	}
	// Remove formatting on shutdown
	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invoke(() ->
		{
			IterableHashTable<MessageNode> messages = client.getMessages();
			if (messages == null)
			{
				return false;
			}

			boolean updated = false;
			for (MessageNode message : messages)
			{
				updated |= removeFormatting(message);
			}

			if (updated)
			{
				client.refreshChat();
			}
			return true;
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals("offlineChatIcon"))
		{
			return;
		}
		formatAllMessages();
	}
	//	Check if rsn is in online clan member list
	private boolean isClanMemberOnline(String rsn)
	{
		ClanChannel clanChannel = client.getClanChannel(ClanID.CLAN);
		if (clanChannel == null)
		{
			return false;
		}

		String standardizedPlayerName = Text.standardize(rsn);

		for (ClanChannelMember member : clanChannel.getMembers())
		{
			if (Text.standardize(member.getName()).equals(standardizedPlayerName))
			{
				return true;
			}
		}
		return false;
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined clanMemberJoined)
	{
		clientThread.invokeLater(() -> rebuildChat(clanMemberJoined.getClanMember().getName()));
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft clanMemberLeft)
	{
		clientThread.invokeLater(() -> rebuildChat(clanMemberLeft.getClanMember().getName()));
	}

	private void loadIcon()
	{

		final IndexedSprite[] modIcons = client.getModIcons();

		if (offlineIconLocation != -1 || modIcons == null)
		{
			return;
		}

		BufferedImage image = ImageUtil.loadImageResource(getClass(), "/logout_icon.png");
		IndexedSprite indexedSprite = ImageUtil.getImageIndexedSprite(image, client);

		offlineIconLocation = modIcons.length;

		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 1);
		newModIcons[newModIcons.length - 1] = indexedSprite;

		client.setModIcons(newModIcons);

		iconImg = "<img=" + offlineIconLocation + ">";
	}

	private String stripName(String name)
	{
		return name
				.replace(iconImg, "")
				.replaceAll("<col=[0-9a-fA-F]{6}>", "")
				.replace("</col>", "");
	}

	private boolean removeFormatting(MessageNode message)
	{
		ChatMessageType type = message.getType();
		if (type != ChatMessageType.CLAN_CHAT && type != ChatMessageType.CLAN_GUEST_CHAT)
		{
			return false;
		}

		// Strip name
		String original = message.getName();
		String cleaned = stripName(original);

		if (!original.equals(cleaned))
		{
			message.setName(cleaned);
			return true;
		}
		return false;
	}

	private boolean applyFormatting(MessageNode message)
	{
		ChatMessageType type = message.getType();
		if (type != ChatMessageType.CLAN_CHAT && type != ChatMessageType.CLAN_GUEST_CHAT)
		{
			return false;
		}
		// Strip name
		String original = message.getName();
		String cleaned = stripName(original);
		String standardized = Text.standardize(Text.removeTags(cleaned));

		if (!isClanMemberOnline(standardized))
		{
			if (config.enableOfflineColor())
			{
				cleaned = getColorTag() + cleaned + "</col>";
			}
			if (config.enableOfflineIcon())
			{
				cleaned = iconImg + cleaned;
			}
		}
		message.setName(cleaned);
		return true;
	}

	private void formatAllMessages()
	{
		IterableHashTable<MessageNode> messages = client.getMessages();
		if (messages == null)
		{
			return;
		}

		boolean updated = false;
		for (MessageNode message : messages)
		{
			updated |= applyFormatting(message);
		}

		if (updated)
		{
			client.refreshChat();
		}
	}

	private void rebuildChat(String rsn)
	{
		boolean needRefreshing = false;
		IterableHashTable<MessageNode> messages = client.getMessages();
		String standardizedRsnFromEvent = Text.standardize(rsn);

		if (messages == null)
		{
			return;
		}
		for (MessageNode message : messages)
		{
			String cleanRsnFromMessage = Text.standardize(Text.removeTags(message.getName()));
			if (cleanRsnFromMessage.equals(standardizedRsnFromEvent))
			{
				needRefreshing |= applyFormatting(message);
			}
		}
		if (needRefreshing)
		{
			client.refreshChat();
		}
	}
	//	Get color from config
	private String getColorTag()
	{
		Color color = config.offlineColor();
		return String.format("<col=%02x%02x%02x>", color.getRed(), color.getGreen(), color.getBlue());
	}
}
