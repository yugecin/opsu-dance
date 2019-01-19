// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

/**
 * actual collection to hold all the nodes because ArrayList is too limiting
 */
class NodeCollection
{
	int size;
	Node[] nodes;

	NodeCollection(int initialSize)
	{
		this.nodes = new Node[initialSize];
	}

	void clear()
	{
		for (int i = this.size; i > 0;) {
			this.nodes[--i] = null;
		}
		this.size = 0;
	}

	void ensureCapacity(int size)
	{
		if (this.nodes.length < size) {
			final Node[] n = new Node[size];
			System.arraycopy(this.nodes, 0, n, 0, this.size);
			this.nodes = n;
		}
	}

	void add(Node node)
	{
		this.nodes[this.size++] = node;
	}

	boolean isEmpty()
	{
		return this.size == 0;
	}

	void shiftRight(int from, int amount)
	{
		System.arraycopy(this.nodes, from, this.nodes, from + amount, this.size - from);
		this.size += amount;
	}

	void shiftLeft(int from, int amount)
	{
		System.arraycopy(this.nodes, from, this.nodes, from - amount, this.size - from);
		this.size -= amount;
		for (int i = this.size + amount; i > this.size;) {
			this.nodes[--i] = null;
		}
	}
}
