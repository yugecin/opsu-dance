// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state;

import org.newdawn.slick.Graphics;

public interface Renderable
{
	public static final Renderable[] EMPTY_ARRAY = new Renderable[0];

	void preRenderUpdate();
	void render(Graphics g);
}
