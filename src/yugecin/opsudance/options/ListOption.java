// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

public abstract class ListOption extends Option
{
	public ListOption(String name, String configurationName, String description)
	{
		super(name, configurationName, description);
		this.setDefaultValue();
	}

	public void setDefaultValue()
	{
	}

	@Override
	public abstract String write();
	@Override
	public abstract void read(String s);
	public abstract Object[] getListItems();
	public abstract void clickListItem(int index);
}
