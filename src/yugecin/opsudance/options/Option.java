// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

import static yugecin.opsudance.core.InstanceContainer.*;

import java.util.LinkedList;

public abstract class Option
{
	protected static final LinkedList<Runnable> EMPTY_LISTENER_COLLECTION = new LinkedList<>();

	public final String name;
	public final String configurationName;
	public final String description;

	public LinkedList<Runnable> listeners = EMPTY_LISTENER_COLLECTION;

	/**
	 * If this option should not be shown in the optionsmenu because it does
	 * not match the search string.
	 */
	private boolean filtered;

	/**
	 * Constructor for internal options (not displayed in-game).
	 */
	public Option(String configurationName) {
		this(null, configurationName, null);
	}

	public Option(String name, String configurationName, String description) {
		this.name = name;
		this.configurationName = configurationName;
		this.description = description;
		optionservice.registerOption(this);
	}
	
	public void addListener(Runnable listener)
	{
		if (this.listeners == EMPTY_LISTENER_COLLECTION) {
			this.listeners = new LinkedList<>();
		}
		this.listeners.add(listener);
	}
	
	public void removeListener(Runnable listener)
	{
		this.listeners.remove(listener);
	}
	
	public void notifyListeners()
	{
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

	/**
	 * should the option be shown
	 * @return true if the option should be shown
	 */
	public boolean showCondition() {
		return true;
	}

	public String getValueString() {
		return "";
	}

	public String write() {
		return getValueString();
	}

	public void read(String s) {
	}

	/**
	 * Update the filtered flag for this option based on the given searchString.
	 * @param searchString the searched string or null to reset the filtered flag
	 * @return true if this option does need to be filtered
	 */
	public boolean filter(String searchString) {
		if (!showCondition()) {
			return filtered = true;
		}
		if (searchString == null || searchString.length() == 0) {
			filtered = false;
			return false;
		}
		filtered = !name.toLowerCase().contains(searchString) && !description.toLowerCase().contains(searchString);
		if (this instanceof ListOption) {
			for (Object itm : ((ListOption) this).getListItems()) {
				if (itm != null && itm.toString().toLowerCase().contains(searchString)) {
					filtered = false;
					return false;
				}
			}
		}
		return filtered;
	}

	public void setFiltered(boolean flag) {
		this.filtered = flag;
	}

	/**
	 * Check if this option should be filtered (= not shown) because it does not
	 * match the search string.
	 * @return true if the option shouldn't be shown.
	 */
	public boolean isFiltered() {
		return filtered;
	}
}
