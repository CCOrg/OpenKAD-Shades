package il.technion.ewolf.tinyLFU;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.tinyLFU.LFU.LFUCache;

import java.util.List;

import com.google.inject.Inject;

public class TinyLfuKadCacheWrapper implements KadCache {

	LFUCache<Key> tinyLfu;

	@Inject
	public TinyLfuKadCacheWrapper(final LFUCache<Key> tinyLfu) {

		this.tinyLfu = tinyLfu;
	}

	@Override
	public void insert(final Key key, final List<Node> nodes) {
		this.tinyLfu.insert(key, nodes);
	}

	@Override
	public List<Node> search(final Key key) {
		return this.tinyLfu.search(key);
	}

	@Override
	public void clear() {
		this.tinyLfu.clear();
	}

}
