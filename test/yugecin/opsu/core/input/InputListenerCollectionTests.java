// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsu.core.input;

import org.junit.Before;
import org.junit.Test;
import yugecin.opsudance.core.input.InputListenerCollection;

import java.util.Iterator;

import static org.junit.Assert.*;

public class InputListenerCollectionTests
{
	boolean primaryListenerWasCalled;
	final Runnable primaryListener = () -> primaryListenerWasCalled = true;
	private InputListenerCollection<Runnable> collection;

	@Before
	public void setup()
	{
		this.primaryListenerWasCalled = false;
		this.collection = new InputListenerCollection<>(primaryListener);
	}

	@Test
	public void primary_listener_should_be_called_first()
	{
		final Runnable secondary = () -> fail("primary should've been called");
		this.collection.add(secondary);

		assertTrue(this.collection.iterator().hasNext());
		this.collection.iterator().next().run();
		assertTrue(this.primaryListenerWasCalled);
	}

	@Test
	public void listeners_should_be_called_from_last_added()
	{
		final int[] number = { 0 };

		final Runnable firstadded = () -> number[0] = 1;
		final Runnable lastadded = () -> number[0] = 2;
		
		this.collection.add(firstadded);
		this.collection.add(lastadded);

		final Iterator<Runnable> iter = this.collection.iterator();
		iter.next().run();
		assertTrue(this.primaryListenerWasCalled);
		assertTrue(number[0] == 0);
		iter.next().run();
		assertTrue(number[0] == 2);
		iter.next().run();
		assertTrue(number[0] == 1);
		assertFalse(iter.hasNext());
	}

	@Test
	public void resize_on_full_backing_array()
	{
		final int[] number = { 0 };

		final Runnable listener = () -> number[0]++;
		for (int i = 0; i < 21; i++) {
			this.collection.add(listener);
		}
		
		for (Runnable l : this.collection) {
			l.run();
		}

		assertTrue(number[0] == 21);
	}

	@Test
	public void removing_removes_the_correct_listener()
	{
		final Runnable one = () -> {};
		final Runnable two = () -> fail("this listener should've been removed");
		final Runnable tri = () -> {};

		this.collection.add(one);
		this.collection.add(two);
		this.collection.add(tri);

		this.collection.remove(two);

		for (Runnable l : this.collection) {
			l.run();
		}
	}

	@Test
	public void removing_all_listeners_is_ok()
	{
		final Runnable single = () -> {};

		this.collection.add(single);

		this.collection.remove(single);

		for (Runnable l : this.collection) {
			l.run();
		}

		assertTrue(this.primaryListenerWasCalled);
	}

	@Test
	public void remove_listener_while_backing_array_is_full()
	{
		final int[] number = { 0 };

		Runnable last = null;
		for (int i = 0; i < 10; i++) {
			last = () -> number[0]++;
			this.collection.add(last);
		}

		this.collection.remove(last);

		for (Runnable l : this.collection) {
			l.run();
		}

		assertTrue(number[0] == 9);
	}

	@Test
	public void double_remove_should_have_no_effect()
	{
		final int[] number = { 0 };

		final Runnable one = () -> number[0]++;
		final Runnable two = () -> fail("this listener should've been removed");
		final Runnable tri = () -> number[0]++;

		this.collection.add(one);
		this.collection.add(two);
		this.collection.add(tri);

		this.collection.remove(two);
		this.collection.remove(two);
		this.collection.remove(two);
		this.collection.remove(two);
		this.collection.remove(two);
		this.collection.remove(two);

		for (Runnable l : this.collection) {
			l.run();
		}

		assertTrue(number[0] == 2);
		assertTrue(this.primaryListenerWasCalled);
	}

	@Test
	public void clear_should_remove_all_except_primary()
	{
		final Runnable one = () -> fail("this listener should've been removed");
		final Runnable two = () -> fail("this listener should've been removed");
		final Runnable tri = () -> fail("this listener should've been removed");

		this.collection.add(one);
		this.collection.add(two);
		this.collection.add(tri);

		this.collection.clear();

		for (Runnable l : this.collection) {
			l.run();
		}

		assertTrue(this.primaryListenerWasCalled);
	}
}
