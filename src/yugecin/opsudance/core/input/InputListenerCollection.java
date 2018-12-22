// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

import java.util.Iterator;

/**
 * Collection backed by an array that can have a primary listener.
 * Iteration order is: primary listener, last added - first added
 */
public class InputListenerCollection<T> implements Iterable<T>
{
	private int size;
	private Object[] array;

	public InputListenerCollection(T primaryListener)
	{
		this.array = new Object[10];
		this.array[0] = primaryListener;
		this.size = 1;
	}

	public void add(T listener)
	{
		if (this.size == this.array.length) {
			final Object[] newArray = new Object[this.array.length * 2];
			System.arraycopy(this.array, 0, newArray, 0, this.array.length);
			this.array = newArray;
		}
		this.array[this.size] = this.array[this.size - 1];
		this.array[this.size - 1] = listener;
		this.size++;
	}

	public void remove(T listener)
	{
		for (int i = size - 2; i >= 0; i--) {
			if (this.array[i] == listener) {
				size--;
				for (; i < size; i++) {
					this.array[i] = this.array[i + 1];
				}
				return;
			}
		}
	}

	/**
	 * does not remove the primary listener
	 */
	public void clear()
	{
		if (this.size > 0) {
			this.array[0] = this.array[this.size - 1];
		}
		this.size = 1;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new Iterator<T>()
		{
			private int previousIdx = size;

			@Override
			public boolean hasNext()
			{
				return previousIdx > 0;
			}

			@SuppressWarnings("unchecked")
			@Override
			public T next()
			{
				return (T) array[--previousIdx];
			}
		};
	}
}
