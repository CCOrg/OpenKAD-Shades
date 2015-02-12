package il.technion.ewolf.shades.sharingPolicy;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.tinyLFU.LFU.TinyLfuCache;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class LfuSharingPolicy implements SharingPolicy {
	private final TinyLfuCache<Key> tinyLfu;
	@Inject
	public LfuSharingPolicy(final TinyLfuCache<Key> tinyLfu){
		this.tinyLfu = tinyLfu;
	}

	@Override
	public boolean isNeeded(final Key key) {
		return this.tinyLfu.isNeeded(key);
	}

	@Override
	public boolean isPopular(final Key key) {
		if (!this.tinyLfu.isFull())
			return true;

		return (this.tinyLfu.howmany(key) > (this.tinyLfu.howmany(this.tinyLfu.getLeastFrequentlyUsed()) / 2));
	}

}
