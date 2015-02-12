package il.technion.ewolf.tinyLFU;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.tinyLFU.LFU.LFUCache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TinyLfuKadCacheAndStorageWrapperColorful implements KadCache {

	private final LFUCache<Key> tinyLfu;
	private final ConcurrentHashMap<Key, List<Node>> storage;
	private final Node localNode;
	private final int nrColors;

	@Inject
	public TinyLfuKadCacheAndStorageWrapperColorful(final LFUCache<Key> tinyLfu, @Named("openkad.local.node") final Node localNode,
			@Named("openkad.color.nrcolors") final int nrColors) {

		this.tinyLfu = tinyLfu;
		this.storage = new ConcurrentHashMap<Key, List<Node>>();
		this.localNode = localNode;
		this.nrColors = nrColors;

	}

	private boolean isKeyInNodes(final Key key, final List<Node> nodes) {
		for (final Node n : nodes)
			if (key.equals(n.getKey()))
				return true;
				return false;
	}

	@Override
	public void insert(final Key key, final List<Node> nodes) {
		try
		{
			if(nodes == null)
				return;
			if (isKeyInNodes(this.localNode.getKey(), nodes))
				this.storage.put(key, nodes);
			else
				this.tinyLfu.insert(key, nodes);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public List<Node> search(final Key key) {
		try
		{
			final List<Node> value = this.storage.get(key);
			if (value != null)
				return value;
			if (key.getColor(this.nrColors) == this.localNode.getKey().getColor(this.nrColors))
				return this.tinyLfu.search(key);

			return this.tinyLfu.NoAddSearch(key);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.tinyLFU.LfuLeastFrequency#getLeastFrequency()
	 */

	@Override
	public void clear() {
		try{
			this.tinyLfu.clear();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
