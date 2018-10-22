/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017-2018 yugecin
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
package yugecin.opsudance.skinning;

import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.skins.Skin;
import itdelatrisu.opsu.skins.SkinLoader;
import org.newdawn.slick.util.ClasspathLocation;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.ResourceLoader;
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

		// set skin and modify resource locations
		ResourceLoader.removeAllResourceLocations();
		if (skinDir == null) {
			skin = new Skin(null);
		} else {
			// load the skin
			skin = SkinLoader.loadSkin(skinDir);
			ResourceLoader.addResourceLocation(new FileSystemLocation(skinDir));
		}
		ResourceLoader.addResourceLocation(new ClasspathLocation());
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File(".")));
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));
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
