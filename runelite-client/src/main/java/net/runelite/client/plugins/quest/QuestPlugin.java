/*
 * Copyright (c) 2018, trimbe <github.com/trimbe>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.quest;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetTextChanged;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.quest.data.Quest;
import net.runelite.client.plugins.quest.data.QuestStep;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Quest"
)
@Slf4j
public class QuestPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	private static final Gson GSON = new Gson();

	private int usedItem;
	private Multiset<Integer> inventoryItems = HashMultiset.create();
	private Multiset<Integer> equipmentItems = HashMultiset.create();
	private int[] varbitValues = new int[10000];
	private int[] varpValues = new int[5000];

	private int[] oldVarps;
	private int[] oldVarps2;

	private boolean loggedIn = false;
	private boolean inventoryInit = false;
	private boolean equipmentInit = false;
	private int numVarbits = 10000;

	private Set<Integer> questVarbits = ImmutableSet.of(2561, 2378, 3468, 6071, 3185, 299, 3274, 487, 203, 2573, 2258, 358, 1465, 2780, 2639, 1560, 2866, 2497, 1803, 2326, 334, 822, 961, 217, 571, 346, 1527, 34, 418, 1990, 532, 2448, 1383, 260, 1103, 2790, 1404, 1850, 657, 2140, 2610, 1372, 2011, 1444, 2098, 1028, 451, 1051, 3293, 3311, 3337, 3523, 3534, 3550, 3618, 2783, 3888, 5027, 5619, 5795, 6037, 6027, 6104, 6358, 6396, 6528, 7796, 7856, 4982, 4976, 5078, 821, 816, 1391, 3330, 1344, 3290, 5347, 6067, 8063);
	private Set<Integer> questVarps = ImmutableSet.of(130, 29, 31, 176, 32, 160, 122, 71, 273, 107, 144, 63, 179, 145, 146, 178, 67, 293, 68, 655, 10, 399, 314, 131, 80, 0, 335, 299, 148, 17, 11, 347, 65, 180, 150, 382, 223, 188, 5, 387, 175, 139, 147, 14, 365, 30, 517, 192, 307, 112, 416, 165, 302, 714, 328, 402, 600, 76, 159, 339, 60, 116, 320, 26, 359, 197, 226, 111, 200, 385, 317, 161, 162, 212, 980, 492, 77, 267);

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (event.getCommand().equals("quest"))
		{
			final InputStream questData = QuestPlugin.class.getResourceAsStream("xmarksthespot.json");
			final Quest quest = GSON.fromJson(new InputStreamReader(questData), Quest.class);
			for (QuestStep step : quest.getSteps())
			{
				log.debug("Quest name: {}", step.getName());
			}
		}
	}

	@Subscribe
	public void onWidgetTextChanged(WidgetTextChanged event) throws IOException
	{
		if (event.getWidget().getId() == WidgetInfo.DIALOG_NPC_TEXT.getId())
		{
			log.debug("Text changed: {}", event.getWidget().getText());
			logData("Text changed: " + event.getWidget().getText());
		}

		if (event.getWidget().getId() == WidgetInfo.PACK(219, 1))
		{
			if (!event.getWidget().getText().equals("Select an Option")
				&& !event.getWidget().getText().equals("Please wait..."))
			{
				String msg = String.format("Dialog option: %s, index: %d",
					event.getWidget().getText(), event.getWidget().getIndex());
				log.debug(msg);
				logData(msg);
			}
			else if (event.getWidget().getText().equals("Please wait..."))
			{
				String msg = String.format("Dialog option picked: %d", event.getWidget().getIndex());
				log.debug(msg);
				logData(msg);
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String msg;
		NPC npc;
		String spell;
		GameObject[] tileObjs;
		ObjectComposition objComp;
		Tile[][][] tiles;
		ItemComposition item;

		log.debug("{}", event);

		switch (event.getMenuAction())
		{
			case ITEM_FIRST_OPTION:
			case ITEM_SECOND_OPTION:
			case ITEM_THIRD_OPTION:
			case ITEM_FOURTH_OPTION:
			case ITEM_FIFTH_OPTION:
				item = itemManager.getItemComposition(event.getId());
				logData(String.format("Item option used - Item: %s ID: %d, Option: %s", item.getName(), item.getId(), event.getMenuOption()));
				break;
			case ITEM_USE:
				usedItem = event.getId();
				break;
			case ITEM_USE_ON_GAME_OBJECT:
				item = itemManager.getItemComposition(usedItem);
				tiles = client.getScene().getTiles();
				tileObjs = tiles[client.getPlane()][event.getActionParam()][event.getWidgetId()].getGameObjects();
				objComp = client.getObjectDefinition(event.getId());
				for (GameObject obj : tileObjs)
				{
					if (obj != null && obj.getId() == event.getId())
					{
						msg = String.format("Item used on object - Item name: %s, Item ID: %d, Name: %s, ID: %s, Location: %s, Option: %s",
							item.getName(),
							item.getId(),
							objComp.getName(),
							objComp.getId(),
							obj.getWorldLocation(),
							event.getMenuOption());
						logData(msg);
					}
				}
				break;
			case ITEM_USE_ON_NPC:
				npc = client.getCachedNPCs()[event.getId()];
				item = itemManager.getItemComposition(usedItem);
				msg = String.format("Item used on NPC - Item name: %s, Item ID: %d, Name: %s, ID: %d, Location: %s",
					item.getName(),
					item.getId(),
					npc.getName(),
					npc.getId(),
					npc.getWorldLocation());
				logData(msg);
				break;
			case SPELL_CAST_ON_GROUND_ITEM:
				int x = event.getActionParam();
				int y = event.getWidgetId();
				ItemComposition itemComp = client.getItemDefinition(event.getId());
				spell = Text.removeTags(client.getCurrentTargetNoun());
				msg = String.format("Spell cast on item - Spell: %s, Name: %s, ID: %s, Location: %s",
					spell,
					itemComp.getName(),
					itemComp.getId(),
					WorldPoint.fromScene(client, x, y, client.getPlane()));
				logData(msg);
				break;
			case SPELL_CAST_ON_NPC:
				npc = client.getCachedNPCs()[event.getId()];
				spell = Text.removeTags(client.getCurrentTargetNoun());
				msg = String.format("Spell cast on NPC - Spell: %s, Name: %s, ID: %d, Location: %s",
					spell,
					npc.getName(),
					npc.getId(),
					npc.getWorldLocation().toString());
				logData(msg);
				break;
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
				npc = client.getCachedNPCs()[event.getId()];
				msg = String.format("Interacted with NPC - Name: %s, ID: %d, Location: %s, Option: %s",
					npc.getName(),
					npc.getId(),
					npc.getWorldLocation().toString(),
					event.getMenuOption());
				logData(msg);
				break;
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
				tiles = client.getScene().getTiles();
				tileObjs = tiles[client.getPlane()][event.getActionParam()][event.getWidgetId()].getGameObjects();
				objComp = client.getObjectDefinition(event.getId());
				for (GameObject obj : tileObjs)
				{
					if (obj != null && obj.getId() == event.getId())
					{
						msg = String.format("Interacted with object - Name: %s, ID: %s, Location: %s, Option: %s",
							objComp.getName(),
							objComp.getId(),
							obj.getWorldLocation(),
							event.getMenuOption());
						logData(msg);
					}
				}
				break;
			case WIDGET_TYPE_6:
				if (event.getWidgetId() == WidgetInfo.DIALOG_NPC_TEXT.getId())
				{
					log.debug("Dialog option picked - Index: {}", event.getActionParam());
					logData(String.format("Dialog option picked - Index: %s", event.getActionParam()));
				}
				break;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (oldVarps != null)
		{
			int[] varps = client.getVarps();

			// Check varbits
			for (int i = 0; i < numVarbits; i++)
			{
				try
				{
					int old = client.getVarbitValue(oldVarps, i);
					int neew = client.getVarbitValue(varps, i);
					if (old != neew)
					{
						// Set the varbit so it doesn't show in the varp changes
						// However, some varbits share common bits, so we only do it in oldVarps2
						// Example: 4101 collides with 4104-4129
						client.setVarbitValue(oldVarps2, i, neew);

						String name = String.format("%d", i);
						for (Varbits varbit : Varbits.values())
						{
							if (varbit.getId() == i)
							{
								name = String.format("%s(%d)", varbit.name(), i);
								break;
							}
						}
						String msg = String.format("Varbit changed - ID: %s, Old: %d, New: %d",
							name, old, neew);

						if (questVarbits.contains(i))
						{
							msg = "Quest " + msg;
						}

						logData(msg);
					}
				}
				catch (IndexOutOfBoundsException e)
				{
					// We don't know what the last varbit is, so we just hit the end, then set it for future iterations
					log.debug("Hit OOB at varbit {}", i);
					numVarbits = i;
					break;
				}
			}

			// Check varps
			for (int i = 0; i < varps.length; i++)
			{
				int old = oldVarps2[i];
				int neew = varps[i];
				if (old != neew)
				{
					String name = String.format("%d", i);
					for (VarPlayer varp : VarPlayer.values())
					{
						if (varp.getId() == i)
						{
							name = String.format("%s(%d)", varp.name(), i);
							break;
						}
					}

					String msg = String.format("Varp changed - ID: %s, Old: %d, New: %d",
						name, old, neew);

					if (questVarps.contains(i))
					{
						msg = "Quest " + msg;
					}

					logData(msg);
				}
			}

			System.arraycopy(client.getVarps(), 0, oldVarps, 0, oldVarps.length);
			System.arraycopy(client.getVarps(), 0, oldVarps2, 0, oldVarps2.length);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		log.debug("{}", client.getItemContainer(InventoryID.INVENTORY) == event.getItemContainer());
		if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY) && !inventoryInit)
		{
			inventoryInit = true;
			inventoryItems = createItemMultiset(event.getItemContainer().getItems());
			return;
		}

		if (event.getItemContainer() == client.getItemContainer(InventoryID.EQUIPMENT) && !equipmentInit)
		{
			equipmentInit = true;
			equipmentItems = createItemMultiset(event.getItemContainer().getItems());
			return;
		}

		ItemContainer container;
		Set changedItems;
		boolean changed = false;

		if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY))
		{
			container = client.getItemContainer(InventoryID.INVENTORY);
			Multiset<Integer> newItems = createItemMultiset(container.getItems());

			String changeStr = "Items added - [";
			for (Multiset.Entry<Integer> itemEntry : Multisets.difference(newItems, inventoryItems).entrySet())
			{
				changeStr += itemsChangedString("inventory", "added", itemEntry);
				changed = true;
			}
			changeStr += "]";
			if (changed)
			{
				log.debug(changeStr);
				logData(changeStr);
				changed = false;
			}

			changeStr = "Items removed - [";
			for (Multiset.Entry<Integer> itemEntry : Multisets.difference(inventoryItems, newItems).entrySet())
			{
				changeStr += itemsChangedString("inventory", "removed", itemEntry);
				changed = true;
			}
			changeStr += "]";
			if (changed)
			{
				log.debug(changeStr);
				logData(changeStr);
				changed = false;
			}

			inventoryItems = newItems;
		}
		else if (event.getItemContainer() == client.getItemContainer(InventoryID.EQUIPMENT))
		{
			container = client.getItemContainer(InventoryID.EQUIPMENT);
			Multiset<Integer> newItems = createItemMultiset(container.getItems());

			String changeStr = "Items equipped - [";
			for (Multiset.Entry<Integer> itemEntry : Multisets.difference(newItems, equipmentItems).entrySet())
			{
				changed = true;
				changeStr += itemsChangedString("equipment", "equipped", itemEntry);
			}
			changeStr += "]";
			if (changed)
			{
				log.debug(changeStr);
				logData(changeStr);
				changed = false;
			}

			changeStr = "Items unequipped - [";
			for (Multiset.Entry<Integer> itemEntry : Multisets.difference(equipmentItems, newItems).entrySet())
			{
				changed = true;
				changeStr += itemsChangedString("equipment", "unequipped", itemEntry);
			}
			changeStr += "]";
			if (changed)
			{
				log.debug(changeStr);
				logData(changeStr);
			}

			equipmentItems = newItems;
		}
	}

	private String itemsChangedString(String inv, String op, Multiset.Entry<Integer> entry)
	{
		int id = entry.getElement();
		String name = itemManager.getItemComposition(id).getName();
		int count = entry.getCount();
		return String.format("%s (%d) x%d,", name, id, count);
	}

	private HashMultiset<Integer> createItemMultiset(Item[] items)
	{
		HashMultiset<Integer> multiset = HashMultiset.create();
		for (Item item : items)
		{
			multiset.add(item.getId(), item.getQuantity());
		}

		return multiset;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (loggedIn && oldVarps == null)
		{
			oldVarps = new int[client.getVarps().length];
			oldVarps2 = new int[client.getVarps().length];
			System.arraycopy(client.getVarps(), 0, oldVarps, 0, oldVarps.length);
			System.arraycopy(client.getVarps(), 0, oldVarps2, 0, oldVarps2.length);

			loggedIn = false;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case CONNECTION_LOST:
			case LOGIN_SCREEN:
			case HOPPING:
				loggedIn = false;
				oldVarps = null;
				oldVarps2 = null;
				inventoryInit = false;
				equipmentInit = false;
				break;
			case LOGGED_IN:
				if (oldVarps == null)
				{
					loggedIn = true;
				}
				break;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE
			|| event.getType() == ChatMessageType.CONSOLE)
		{
			log.debug("Chat message - {}", event.getMessage());
			logData("Chat message - " + event.getMessage());
		}
	}

	private void logData(String message)
	{
		try
		{
			URI filePath = new File(System.getProperty("user.home"), ".runelite").toPath().resolve("data.txt").toUri();
			BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.getPath(), true));
			writer.append(message + " [" + client.getLocalPlayer().getWorldLocation().toString() + "] \n");
			writer.close();
		}
		catch (IOException ex)
		{
			log.debug(ex.getMessage());
		}
	}
}
