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
package yugecin.opsudance.core.events;

import java.util.*;

@SuppressWarnings("unchecked")
public class EventBus {

	@Deprecated
	public static EventBus instance; // TODO get rid of this

	private final List<Subscriber> subscribers;

	public EventBus() {
		subscribers = new LinkedList<>();
		instance = this;
	}

	public <T> void subscribe(Class<T> eventType, EventListener<T> eventListener) {
		subscribers.add(new Subscriber<>(eventType, eventListener));
	}

	public void post(Object event) {
		for (Subscriber s : subscribers) {
			if (s.eventType.isInstance(event)) {
				s.listener.onEvent(event);
			}
		}
	}

	private class Subscriber<T> {

		private final Class<T> eventType;
		private final EventListener<T> listener;

		private Subscriber(Class<T> eventType, EventListener<T> listener) {
			this.eventType = eventType;
			this.listener = listener;
		}

	}

}
