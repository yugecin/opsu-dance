// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public abstract class Event
{
	boolean consumed;

	public final void consume()
	{
		this.consumed = true;
	}

	public void unconsume()
	{
		this.consumed = false;
	}

	/**
	 * @deprecated remove soon
	 */
	@Deprecated
	public final boolean isConsumed()
	{
		return this.consumed;
	}
}
