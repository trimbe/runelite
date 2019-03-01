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
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ImageUtil;

@Singleton
public class HintArrowMinimapOverlay extends Overlay
{
	private static final int MINIMAP_ARROW_DISTANCE_FROM_EDGE = 60;
	private static final int DISTANCE_VISIBLE_ON_MINIMAP = 15;
	private static final int DUNGEON_START_Y = 4160;

	private final HintArrowManager hintArrowManager;
	private final Client client;

	private final BufferedImage miniMapHintArrow;

	@Inject
	private HintArrowMinimapOverlay(HintArrowManager hintArrowManager, Client client) throws Exception
	{
		this.hintArrowManager = hintArrowManager;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGHEST);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

		synchronized (ImageIO.class)
		{
			BufferedImage hintArrow = ImageIO.read(hintArrowManager.getClass().getResourceAsStream("rl_hint_arrow.png"));
			miniMapHintArrow = ImageUtil.resizeImage(hintArrow, 15, 15);
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameCycle() % 20 >= 10)
		{
			return null;
		}

		for (HintArrow arrow : hintArrowManager.getHintArrows())
		{
			Actor actor = arrow.getActor();

			switch (arrow.getType())
			{
				case PLAYER:
					if (actor == null)
					{
						continue;
					}

					drawMinimapArrow(graphics, actor.getWorldLocation());
					break;
				case NPC:
					if (actor == null)
					{
						continue;
					}

					drawMinimapArrow(graphics, actor.getWorldLocation());
					break;
				case WORLD_POSITION:
					WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
					// we want to show the minimap arrow regardless of our current plane
					WorldPoint arrowLocation = new WorldPoint(arrow.getWorldPoint().getX(),
						arrow.getWorldPoint().getY(), client.getPlane());
					// restrict the arrow edge overlay for dungeons unless we're probably in the same dungeon
					// vanilla uses only the distance check, use region and scene check to cover slightly more cases
					if (arrowLocation.getY() > DUNGEON_START_Y || playerLocation.getY() > DUNGEON_START_Y || client.isInInstancedRegion())
					{
						if (WorldPoint.isInScene(client, arrowLocation.getX(), arrowLocation.getY())
							|| arrowLocation.getRegionID() == playerLocation.getRegionID()
							|| euclideanDistance(arrowLocation, playerLocation) < 75)
						{
							drawMinimapArrow(graphics, arrow.getWorldPoint());
						}
					}
					else
					{
						drawMinimapArrow(graphics, arrowLocation);
					}
					break;
			}
		}

		return null;
	}

	private void drawMinimapArrow(Graphics2D graphics, WorldPoint location)
	{
		LocalPoint localLocation = LocalPoint.fromWorld(client, location);
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		// if arrow's location is not visible in the minimap, draw an arrow pointing towards it
		if (localLocation == null || location.distanceTo(playerLocation) > DISTANCE_VISIBLE_ON_MINIMAP)
		{
			Widget minimapDrawWidget = getMinimapDrawArea();
			if (minimapDrawWidget == null)
			{
				return;
			}

			int mmX = minimapDrawWidget.getCanvasLocation().getX() + minimapDrawWidget.getWidth() / 2;
			int mmY = minimapDrawWidget.getCanvasLocation().getY() + minimapDrawWidget.getHeight() / 2;

			int dy = playerLocation.getY() - location.getY();
			int dx = playerLocation.getX() - location.getX();
			double rads = Math.atan2(dx, dy);

			// convert camera rotation from JAU to radians
			double cameraRads = (Math.PI / 1024d) * client.getCameraYaw();

			AffineTransform transform = new AffineTransform();
			// rotate the arrow, offsetting with the camera rotation
			transform.rotate(rads + cameraRads);
			// translate the arrow to the edge of the minimap
			transform.translate(-miniMapHintArrow.getWidth() / 2,
				MINIMAP_ARROW_DISTANCE_FROM_EDGE - miniMapHintArrow.getHeight() / 2);

			AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
			graphics.drawImage(miniMapHintArrow, transformOp, mmX, mmY);
		}
		else
		{
			Point minimapPoint = Perspective.localToMinimap(client, localLocation);
			if (minimapPoint == null)
			{
				return;
			}

			graphics.drawImage(miniMapHintArrow,
				minimapPoint.getX() - miniMapHintArrow.getWidth() / 2,
				minimapPoint.getY() - miniMapHintArrow.getHeight(),
				null);
		}
	}

	private Widget getMinimapDrawArea()
	{
		Widget minimapDrawWidget;
		if (client.isResized())
		{
			if (client.getVar(Varbits.SIDE_PANELS) == 1)
			{
				minimapDrawWidget = client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_DRAW_AREA);
			}
			else
			{
				minimapDrawWidget = client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_STONES_DRAW_AREA);
			}
		}
		else
		{
			minimapDrawWidget = client.getWidget(WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
		}

		if (minimapDrawWidget == null || minimapDrawWidget.isHidden())
		{
			return null;
		}

		return minimapDrawWidget;
	}

	private double euclideanDistance(WorldPoint a, WorldPoint b)
	{
		return Math.sqrt((b.getY() - a.getY()) * (b.getY() - a.getY()) + (b.getX() - a.getX()) * (b.getX() - a.getX()));
	}
}