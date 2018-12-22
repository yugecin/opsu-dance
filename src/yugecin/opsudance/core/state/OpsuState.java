// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state;

import yugecin.opsudance.core.errorhandling.ErrorDumpable;
import yugecin.opsudance.core.input.InputListener;

public interface OpsuState extends ErrorDumpable, Renderable, InputListener
{
	void update();
	void enter();
	void leave();

	/**
	 * @return true if closing is allowed
	 */
	boolean onCloseRequest();
}
