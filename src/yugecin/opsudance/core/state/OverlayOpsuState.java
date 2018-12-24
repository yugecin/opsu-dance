// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state;

import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.input.*;

import java.io.StringWriter;

@Deprecated
public abstract class OverlayOpsuState implements OpsuState
{
	protected boolean active;
	protected boolean acceptInput;

	public void hide() {
		acceptInput = active = false;
	}

	public void show() {
		acceptInput = active = true;
	}
	
	public boolean isActive() {
		return this.active;
	}

	@Override
	public final void update() {
	}

	public void revalidate() {
	}

	protected abstract void onPreRenderUpdate();

	@Override
	public final void preRenderUpdate() {
		if (active) {
			onPreRenderUpdate();
		}
	}

	protected abstract void onRender(Graphics g);

	@Override
	public final void render(Graphics g) {
		if (active) {
			onRender(g);
		}
	}

	@Override
	public final void enter() {
	}

	@Override
	public final void leave() {
	}

	@Override
	public final boolean onCloseRequest() {
		return true;
	}

	protected abstract void onKeyPressed(KeyEvent e);

	@Override
	public final void keyPressed(KeyEvent e) {
		if (this.acceptInput) {
			this.onKeyPressed(e);
		}
	}

	protected abstract void onKeyReleased(KeyEvent e);

	@Override
	public final void keyReleased(KeyEvent e)
	{
		if (this.acceptInput) {
			this.onKeyReleased(e);
		}
	}

	protected abstract void onMouseWheelMoved(MouseWheelEvent e);

	@Override
	public final void mouseWheelMoved(MouseWheelEvent e)
	{
		if (this.acceptInput) {
			this.onMouseWheelMoved(e);
		}
	}

	protected abstract void onMousePressed(MouseEvent e);

	@Override
	public final void mousePressed(MouseEvent e)
	{
		if (this.acceptInput) {
			this.onMousePressed(e);
		}
	}

	protected abstract void onMouseReleased(MouseEvent e);

	@Override
	public final void mouseReleased(MouseEvent e)
	{
		if (this.acceptInput) {
			this.onMouseReleased(e);
		}
	}

	protected abstract void onMouseDragged(MouseDragEvent e);

	@Override
	public final void mouseDragged(MouseDragEvent e)
	{
		if (this.acceptInput) {
			this.onMouseDragged(e);
		}
	}

	@Override
	public void writeErrorDump(StringWriter dump) {
		dump.append("> OverlayOpsuState dump\n");
		dump.append("accepts input: ").append(String.valueOf(acceptInput)).append(" is active: ").append(String.valueOf(active));
	}
}
