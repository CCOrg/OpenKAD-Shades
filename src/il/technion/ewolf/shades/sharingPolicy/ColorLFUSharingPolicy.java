package il.technion.ewolf.shades.sharingPolicy;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.tinyLFU.LFU.TinyLfuCache;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ColorLFUSharingPolicy implements SharingPolicy {
	private final TinyLfuCache<Key> tinyLfu;
	private final int localColor;
	private final int nrColors;

	@Inject
	public ColorLFUSharingPolicy(final TinyLfuCache<Key> tinyLfu, @Named("openkad.local.node") final Node localNode,
			@Named("openkad.color.nrcolors") final int nrColors) {
		this.tinyLfu = tinyLfu;
		this.localColor = localNode.getKey().getColor(nrColors);
		this.nrColors = nrColors;
	}

	@Override
	public boolean isNeeded(final Key key) {
		if (key.getColor(this.nrColors) != this.localColor)
			return false;
		return this.tinyLfu.isNeeded(key);
	}

	@Override
	public boolean isPopular(final Key key) {
		if (key.getColor(this.nrColors) != this.localColor)
			return false;

		if (!this.tinyLfu.isFull())
			return true;

		return (this.tinyLfu.howmany(key) > (this.tinyLfu.howmany(this.tinyLfu.getLeastFrequentlyUsed()) / 2));
	}

}
