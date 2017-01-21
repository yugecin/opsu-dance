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
package yugecin.opsudance.core.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

@SuppressWarnings("unchecked")
public abstract class Injector implements InstanceContainer, Binder {

	private final HashMap<Class<?>, Object> instances;
	private final LinkedList<Class<?>> lazyInstances;

	private Class<?> lastType;

	public Injector() {
		instances = new HashMap<>();
		lazyInstances = new LinkedList<>();
		instances.put(InstanceContainer.class, this);
		configure();
	}

	protected abstract void configure();

	public final <T> T provide(Class<T> type) {
		Object instance = instances.get(type);
		if (instance != null) {
			return (T) instance;
		}
		ListIterator<Class<?>> iter = lazyInstances.listIterator();
		while (iter.hasNext()) {
			Class<?> l = iter.next();
			if (l == type) {
				iter.remove();
				instance = createInstance(type);
				instances.put(type, instance);
				return (T) instance;
			}
		}
		return createInstance(type);
	}

	private <T> T createInstance(Class<T> type) {
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		if (constructors.length == 0) {
			throw new RuntimeException("Cannot provide " + type.getSimpleName());
		}
		Constructor constructor = constructors[0];
		Class<?>[] parameterTypes = constructor.getParameterTypes();
		Object[] params = new Object[parameterTypes.length];
		for (int i = parameterTypes.length - 1; i >= 0; i--) {
			params[i] = provide(parameterTypes[i]);
		}
		try {
			return	(T) constructor.newInstance(params);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public final <T> Binder<T> bind(Class<T> type) {
		lastType = type;
		return this;
	}

	@Override
	public final void asEagerSingleton() {
		instances.put(lastType, createInstance(lastType));
	}

	@Override
	public final void asLazySingleton() {
		lazyInstances.add(lastType);
	}

	@Override
	public final void to(Class type) {
		instances.put(lastType, createInstance(type));
	}

}
