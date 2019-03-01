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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Singleton
public class HintArrowOverlay extends Overlay
{
	private static final int ACTOR_Z_OFFSET = 15;
	private static final int WORLD_Z_OFFSET = 10;
	private static final int ARROW_Y_OFFSET = 6;

	private final HintArrowManager hintArrowManager;
	private final Client client;

	private final BufferedImage hintArrow;
	private final BufferedImage playerHintArrow;


	@Inject
	private HintArrowOverlay(HintArrowManager hintArrowManager, Client client) throws Exception
	{
		this.hintArrowManager = hintArrowManager;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGHEST);
		setLayer(OverlayLayer.ABOVE_SCENE);

		synchronized (ImageIO.class)
		{
			hintArrow = ImageIO.read(hintArrowManager.getClass().getResourceAsStream("rl_hint_arrow.png"));
			playerHintArrow = ImageIO.read(hintArrowManager.getClass().getResourceAsStream("rl_player_hint_arrow.png"));
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (HintArrow arrow : hintArrowManager.getHintArrows())
		{
			Point arrowPoint;
			Actor actor = arrow.getActor();

			switch (arrow.getType())
			{
				case PLAYER:
					if (actor == null)
					{
						continue;
					}

					drawActorArrow(graphics, actor, playerHintArrow);
					break;
				case NPC:
					if (client.getGameCycle() % 20 < 10)
					{
						if (actor == null)
						{
							continue;
						}

						drawActorArrow(graphics, actor, hintArrow);
					}
					break;
				case WORLD_POSITION:
					if (client.getGameCycle() % 20 < 10)
					{
						LocalPoint arrowLocalPoint = LocalPoint.fromWorld(client, arrow.getWorldPoint());
						if (arrowLocalPoint == null)
						{
							break;
						}

						arrowPoint = Perspective.localToCanvas(client, arrowLocalPoint, client.getPlane(),
							WORLD_Z_OFFSET);
						if (arrowPoint == null)
						{
							break;
						}

						graphics.drawImage(hintArrow,
							arrowPoint.getX() - hintArrow.getWidth() / 2,
							arrowPoint.getY() - hintArrow.getHeight() - ARROW_Y_OFFSET,
							null);
					}
					break;
			}
		}

		return null;
	}

	private void drawActorArrow(Graphics2D graphics, Actor actor, BufferedImage arrow)
	{
		int height = actor.getLogicalHeight() + ACTOR_Z_OFFSET;
		LocalPoint playerLoc = actor.getLocalLocation();

		Point arrowPoint = Perspective.localToCanvas(client, playerLoc, client.getPlane(), height);
		if (arrowPoint == null)
		{
			return;
		}

		graphics.drawImage(arrow,
			arrowPoint.getX() - hintArrow.getWidth() / 2,
			arrowPoint.getY() - hintArrow.getHeight() - ARROW_Y_OFFSET,
			null);
	}
}