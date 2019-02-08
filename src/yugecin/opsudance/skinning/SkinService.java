// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.skinning;

import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.skins.Skin;
import itdelatrisu.opsu.skins.SkinLoader;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.ResourceLoader;
import org.newdawn.slick.util.ResourceLocation;

import yugecin.opsudance.events.SkinChangedListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * @author itdelatrisu (https://github.com/itdelatrisu) most functions are copied from itdelatrisu.opsu.Options.java
 */
public class SkinService
{
	private final List<SkinChangedListener> skinChangedListeners;

	private ResourceLocation lastSkinLocation;

	public String[] availableSkinDirectories;
	public String usedSkinName = "Default";
	public static Skin skin;
	
	public SkinService()
	{
		this.skinChangedListeners = new ArrayList<>();
	}
	
	public void addSkinChangedListener(SkinChangedListener l)
	{
		this.skinChangedListeners.add(l);
	}

	public void reloadSkin()
	{
		loadSkin();
		SoundController.init();
		for (SkinChangedListener l : this.skinChangedListeners) {
			l.onSkinChanged(this.usedSkinName);
		}
	}

	/**
	 * Loads the skin given by the current skin directory.
	 * If the directory is invalid, the default skin will be loaded.
	 */
	public void loadSkin() {
		File skinDir = getCurrentSkinDirectory();
		if (skinDir == null) {
			// invalid skin name
			usedSkinName = Skin.DEFAULT_SKIN_NAME;
		}

		// create available skins list
		File[] dirs = SkinLoader.getSkinDirectories(config.skinRootDir);
		availableSkinDirectories = new String[dirs.length + 1];
		availableSkinDirectories[0] = Skin.DEFAULT_SKIN_NAME;
		for (int i = 0; i < dirs.length; i++) {
			availableSkinDirectories[i + 1] = dirs[i].getName();
		}

		if (this.lastSkinLocation != null) {
			ResourceLoader.removeResourceLocation(this.lastSkinLocation);
			this.lastSkinLocation = null;
		}
		if (skinDir == null) {
			skin = new Skin(null);
			return;
		}

		// load the skin
		skin = SkinLoader.loadSkin(skinDir);
		this.lastSkinLocation = new FileSystemLocation(skinDir);
		ResourceLoader.addPrimaryResourceLocation(this.lastSkinLocation);
	}

	/**
	 * Returns the current skin directory.
	 * <p>
	 * NOTE: This directory will differ from that of the currently loaded skin
	 * if {@link #loadSkin()} has not been called after a directory change.
	 * Use {@link Skin#getDirectory()} to get the directory of the currently
	 * loaded skin.
	 * @return the skin directory, or null for the default skin
	 */
	public File getCurrentSkinDirectory() {
		File dir = new File(config.skinRootDir, usedSkinName);
		return (dir.isDirectory()) ? dir : null;
	}
}
