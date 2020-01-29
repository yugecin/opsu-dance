// Copyright 2017-2020 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state;

import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.input.*;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.events.SkinChangedListener;

import java.io.StringWriter;

import static yugecin.opsudance.core.InstanceContainer.*;

public abstract class BaseOpsuState
	implements OpsuState, ResolutionChangedListener, SkinChangedListener
{
	/**
	 * state is dirty when resolution or skin changed but hasn't rendered yet
	 */
	private boolean isDirty;
	private boolean isCurrentState;

	public BaseOpsuState() {
		displayContainer.addResolutionChangedListener(this);
		skinservice.addSkinChangedListener(this);
	}

	protected void revalidate() {
	}

	@Override
	public void update() {
	}

	@Override
	public void preRenderUpdate() {
	}

	@Override
	public void render(Graphics g) {
	}
	
	@Override
	public void onSkinChanged(String name) {
		makeDirty();
	}

	@Override
	public void onResolutionChanged(int w, int h) {
		makeDirty();
	}
	
	private void makeDirty() {
		if (isCurrentState) {
			revalidate();
			return;
		}
		isDirty = true;
	}

	@Override
	public void enter() {
		isCurrentState = true;
		if (isDirty) {
			revalidate();
			isDirty = false;
		}
	}

	@Override
	public void leave() {
		isCurrentState = false;
	}

	@Override
	public boolean onCloseRequest() {
		return true;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
	}

	@Override
	public final void writeErrorDump(StringWriter dump)
	{
		dump.append("> BaseOpsuState dump\n");
		dump.append("isDirty: ").append(String.valueOf(isDirty)).append('\n');
		this.writeStateErrorDump(dump);
	}

	protected abstract void writeStateErrorDump(StringWriter dump);
}
