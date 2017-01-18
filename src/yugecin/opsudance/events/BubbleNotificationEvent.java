/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.events;

import org.newdawn.slick.Color;

public class BubbleNotificationEvent {

	public static final Color COMMONCOLOR_GREEN = new Color(98, 131, 59);
	public static final Color COMMONCOLOR_WHITE = new Color(220, 220, 220);
	public static final Color COMMONCOLOR_PURPLE = new Color(94, 46, 149);
	public static final Color COLOR_RED = new Color(178, 62, 41);
	public static final Color COLOR_ORANGE = new Color(138, 72, 51);

	public final String message;
	public final Color borderColor;

	public BubbleNotificationEvent(String message, Color borderColor) {
		this.message = message;
		this.borderColor = borderColor;
	}

}
