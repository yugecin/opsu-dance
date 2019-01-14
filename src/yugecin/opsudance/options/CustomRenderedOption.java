// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

public class CustomRenderedOption extends Option
{
	public CustomRenderedOption(String name, String configurationName, String description)
	{
		super(name, configurationName, description);
	}

	public void render(int baseHeight, int x, int y, int textOffsetY, int width)
	{
	}
}
