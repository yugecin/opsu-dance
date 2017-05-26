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
package yugecin.opsudance.options;

import org.newdawn.slick.util.Log;
import yugecin.opsudance.events.BubNotifListener;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * @author itdelatrisu (https://github.com/itdelatrisu)
 * most functions are copied from itdelatrisu.opsu.Options.java
 */
public class OptionsService {

	public final HashMap<String, Option> optionMap;

	public OptionsService() {
		optionMap = new HashMap<>();
	}

	public void registerOption(Option option) {
		optionMap.put(option.configurationName, option);
	}

	public void loadOptions() {
		// if no config file, use default settings
		if (!config.OPTIONS_FILE.isFile()) {
			config.loadDirectories();
			saveOptions();
			return;
		}

		// read file
		try (BufferedReader in = new BufferedReader(new FileReader(config.OPTIONS_FILE))) {
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() < 2 || line.charAt(0) == '#') {
					continue;
				}
				int index = line.indexOf('=');
				if (index == -1) {
					continue;
				}

				// read option
				String name = line.substring(0, index).trim();
				Option option = optionMap.get(name);
				if (option != null) {
					try {
						String value = line.substring(index + 1).trim();
						option.read(value);
					} catch (Exception e) {
						Log.warn(String.format("Format error in options file for line: '%s'.", line), e);
					}
				}
			}
		} catch (IOException e) {
			String err = String.format("Failed to read option file '%s'.", config.OPTIONS_FILE.getAbsolutePath());
			Log.error(err, e);
			BubNotifListener.EVENT.make().onBubNotif(err, BubNotifListener.COMMONCOLOR_RED);
		}
		config.loadDirectories();
	}

	public void saveOptions() {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
			new FileOutputStream(config.OPTIONS_FILE), "utf-8"))) {
			// header
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
			String date = dateFormat.format(new Date());
			writer.write("# opsu! configuration");
			writer.newLine();
			writer.write("# last updated on ");
			writer.write(date);
			writer.newLine();
			writer.newLine();

			// options
			for (Option option : optionMap.values()) {
				writer.write(option.configurationName);
				writer.write(" = ");
				writer.write(option.write());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			String err = String.format("Failed to write to file '%s'.", config.OPTIONS_FILE.getAbsolutePath());
			Log.error(err, e);
			BubNotifListener.EVENT.make().onBubNotif(err, BubNotifListener.COMMONCOLOR_RED);
		}
	}

}
