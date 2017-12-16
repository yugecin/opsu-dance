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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;

@SuppressWarnings("unchecked")
public class Event<T> {

	private final Class<T> type;
	private final LinkedList<T> listeners;

	public Event(Class<T> type) {
		this.type = type;
		this.listeners = new LinkedList<>();
	}

	public void addListener(T listener) {
		this.listeners.add(listener);
	}

	public T make() {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
			new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					for (T listener : listeners) {
						method.invoke(listener, args);
					}
					return null;
				}
			});
	}

}
