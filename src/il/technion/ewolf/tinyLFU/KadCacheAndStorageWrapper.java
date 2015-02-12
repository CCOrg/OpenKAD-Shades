package il.technion.ewolf.tinyLFU;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.tinyLFU.LFU.LFUCache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class KadCacheAndStorageWrapper implements KadCache {

	
	private final ConcurrentHashMap<Key, List<Node>> storage;
	private final Node localNode;
	private KadCache Cache;
	@Inject
	public KadCacheAndStorageWrapper(@Named("openkad.inner.cache") final KadCache lruCache, @Named("openkad.local.node") final Node localNode,
			@Named("openkad.color.nrcolors") final int nrColors) {

		this.Cache = lruCache;
		this.storage = new ConcurrentHashMap<Key, List<Node>>();
		this.localNode = localNode;

	}

	private static boolean isKeyInNodes(final Key key, final List<Node> nodes) {
		for (Node node : nodes) {
			if(node.getKey().equals(key))
				return true;
		}
		return false;
	}

	@Override
	public void insert(final Key key, final List<Node> nodes) {
		if (isKeyInNodes(this.localNode.getKey(), nodes)){
			//this.storage.put(key, nodes);
			System.out.println("Storage Insert! "+ key);
		}
		else
			this.Cache.insert(key, nodes);
	}

	@Override
	public List<Node> search(final Key key) {
		final List<Node> value = this.storage.get(key);
		if (value != null)
			return value;
		return this.Cache.search(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.tinyLFU.LfuLeastFrequency#getLeastFrequency()
	 */

	@Override
	public void clear() {
		this.Cache.clear();
	}

}
