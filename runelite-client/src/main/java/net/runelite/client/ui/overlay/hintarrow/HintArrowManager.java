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
package net.runelite.client.ui.overlay.hintarrow;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;

@Singleton
public class HintArrowManager
{
	@Getter
	private Set<HintArrow> hintArrows = new HashSet<>();

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case HOPPING:
			case LOGIN_SCREEN:
			case CONNECTION_LOST:
				hintArrows.clear();
		}
	}

	public HintArrow add(NPC npc)
	{
		HintArrow hintArrow = new HintArrow(npc);

		hintArrows.add(hintArrow);
		return hintArrow;
	}

	public HintArrow add(Player player)
	{
		HintArrow hintArrow = new HintArrow(player);

		hintArrows.add(hintArrow);
		return hintArrow;
	}

	public HintArrow add(WorldPoint worldPoint)
	{
		HintArrow hintArrow = new HintArrow(worldPoint);

		hintArrows.add(hintArrow);
		return hintArrow;
	}

	public void remove(HintArrow hintArrow)
	{
		hintArrows.remove(hintArrow);
	}

	public void remove(NPC npc)
	{
		hintArrows.remove(new HintArrow(npc));
	}

	public void remove(Player player)
	{
		hintArrows.remove(new HintArrow(player));
	}

	public void remove(WorldPoint worldPoint)
	{
		hintArrows.remove(new HintArrow(worldPoint));
	}
}
