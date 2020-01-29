// Copyright 2018-2020 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state;

import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.errorhandling.ErrorDumpable;

public interface Renderable extends ErrorDumpable
{
	public static final Renderable[] EMPTY_ARRAY = new Renderable[0];

	void preRenderUpdate();
	void render(Graphics g);
}
