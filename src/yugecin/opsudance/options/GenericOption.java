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

public abstract class GenericOption extends Option {

	public int intval;
	public String strval;
	public boolean boolval;

	public GenericOption(String name, String configurationName, String description, int intval, String strval, boolean boolval) {
		super(name, configurationName, description);
		this.intval = intval;
		this.strval = strval;
		this.boolval = boolval;
	}

	@Override
	public abstract String getValueString();
	@Override
	public abstract void read(String s);
	@Override
	public abstract String write();

}
