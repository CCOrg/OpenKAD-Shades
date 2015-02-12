package il.technion.ewolf.shades.buckets;

import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.bucket.Bucket;
import il.technion.ewolf.kbr.openkad.bucket.KadBuckets;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.utils.PrintsManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class ShadesBucketsWithColorRefAndPalette extends KadBuckets implements ColoredKBuckets {

	private final NodesByColor nodesByColor; // Reference to nodes in kbuckets by color
	private final ColorPalette colorPalette; // Palette
	private final int nrColors;

	@Inject
	public ShadesBucketsWithColorRefAndPalette(final KeyFactory keyFactory, final Provider<KadNode> kadNodeProvider,
			final Provider<MessageDispatcher<Object>> msgDispatcherProvider,
			@Named("openkad.bucket.kbuckets") final Provider<Bucket> kBucketProvider,
			@Named("openkad.local.node") final Node localNode, @Named("openkad.color.nrcolors") final int nrColors,
			final ColorPalette colorPalette, final NodesByColor nodesByColor, final PrintsManager printsManager) {
		super(keyFactory, kadNodeProvider, msgDispatcherProvider, kBucketProvider, localNode, nrColors);
		this.nrColors = nrColors;
		this.nodesByColor = nodesByColor;
		this.colorPalette = colorPalette;
	}

	@Override
	public List<Node> getNodesByColor(final int color) {
		final Set<Node> result = new HashSet<Node>();
		colorPalette.addNodesTo(color, result);
		result.addAll(nodesByColor.getNodesByColor(color));
		return new ArrayList<Node>(result);
	}

	@Override
	public void insert(final KadNode node) {
		colorPalette.insert(node);
		super.insert(node);
	}

	@Override
	public BitSet getAvailableColors() {
		return colorPalette.getAvailableColors();
	}

	@Override
	public void insertToPalette(final KadNode node) {
		colorPalette.insert(node);
	}
}
