package il.technion.ewolf.shades.handlers;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyComparator;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.handlers.AbstractHandler;
import il.technion.ewolf.kbr.openkad.handlers.FindNodeHandler;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.ShadesFindNodeRequest;
import il.technion.ewolf.kbr.openkad.msg.ShadesFindNodeResponse;
import il.technion.ewolf.kbr.openkad.net.Communicator;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.MessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;
import il.technion.ewolf.shades.buckets.ColoredKBuckets;
import il.technion.ewolf.shades.sharingPolicy.SharingPolicy;
import il.technion.utils.PrintsManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Handle find node requests by giving the known closest nodes to the requested
 * key from the KAndColorBuckets data structure, in addition in shades we also
 * return correctly shaded nodes. TODO: implement.
 * 
 * @author yoav.kantor@gmail.com
 * 
 */
public class ShadesFindNodeHandler extends AbstractHandler implements FindNodeHandler {
	private final Communicator kadServer;
	private final Node localNode;
	private final KadCache cache;
	private final ColoredKBuckets coloredKBuckets;
	private final int kBucketSize;
	private final int nrColors;

	private final AtomicInteger nrFindnodeHits;
	private final AtomicInteger nrFindnodeMiss;
	private final SharingPolicy sharingPolicy;
	private final PrintsManager printsManager;

	@Inject
	ShadesFindNodeHandler(final Provider<MessageDispatcher<Void>> msgDispatcherProvider, final Communicator kadServer,
			@Named("openkad.local.node") final Node localNode, final KadCache cache, final ColoredKBuckets coloredKBuckets,
			@Named("openkad.color.nrcolors") final int nrColors, @Named("openkad.bucket.kbuckets.maxsize") final int kBucketSize,
			@Named("openkad.testing.nrFindnodeHits") final AtomicInteger nrFindnodeHits,
			@Named("openkad.testing.nrFindnodeMiss") final AtomicInteger nrFindnodeMiss, final SharingPolicy sharingPolicy,
			final PrintsManager printsManager) {

		super(msgDispatcherProvider);
		this.kadServer = kadServer;
		this.localNode = localNode;
		this.cache = cache;
		this.coloredKBuckets = coloredKBuckets;
		this.kBucketSize = kBucketSize;
		this.nrFindnodeHits = nrFindnodeHits;
		this.nrFindnodeMiss = nrFindnodeMiss;
		this.nrColors = nrColors;
		this.sharingPolicy = sharingPolicy;
		this.printsManager = printsManager;
	}

	@Override
	public void completed(final KadMessage msg, final Void attachment) {
		final ShadesFindNodeRequest shadesFindNodeRequest = ((ShadesFindNodeRequest) msg);
		final Key key = shadesFindNodeRequest.getKey();
		final ShadesFindNodeResponse shadesFindNodeResponse = shadesFindNodeRequest.generateResponse(this.localNode)
				.setCachedResults(false);

		if (shadesFindNodeRequest.shouldSearchCache()) {
			final List<Node> cachedResults = this.cache.search(key);
			if (cachedResults == null) {
				this.printsManager.print(3, "ShadesFindNodeHandler", "request from: " + shadesFindNodeRequest.getSrc() + " key: "
						+ shadesFindNodeRequest.getKey() + " cache miss");
				this.nrFindnodeMiss.incrementAndGet();
			} else {
				this.printsManager.print(3, "ShadesFindNodeHandler", "request from: " + shadesFindNodeRequest.getSrc() + " key: "
						+ shadesFindNodeRequest.getKey() + " cache hit");
				this.nrFindnodeHits.incrementAndGet();
				shadesFindNodeResponse.setNodes(new ArrayList<Node>(cachedResults)).setCachedResults(true);
			}
		}

		if (!shadesFindNodeResponse.isCachedResults()) {
			final List<Node> colorNodes = new ArrayList<Node>(30);

			if (shadesFindNodeRequest.getIsJoinPhase())
				// In join phase, help the src node know as many nodes with the
				// same color
				colorNodes.addAll(this.coloredKBuckets.getNodesByColor(shadesFindNodeRequest.getSrc().getKey().getColor(nrColors)));
			else {
				// Give the src node the closest node with the same color as the
				// searched key
				List<Node> colorNodesAsKey = this.coloredKBuckets.getNodesByColor(key.getColor(nrColors));
				colorNodesAsKey = sort(colorNodesAsKey, on(Node.class).getKey(), new KeyComparator(key));
				if (colorNodesAsKey.size() >= 1) {
					colorNodesAsKey.subList(1, colorNodesAsKey.size()).clear();
					colorNodes.add(colorNodesAsKey.get(0));
				}
			}

			// Help the src node fill the palette
			final BitSet hisAvailableColors = shadesFindNodeRequest.getAvailableColors();
			if(hisAvailableColors!=null){
				//System.out.println("hisAvailableColors: " + hisAvailableColors.toString());
				final BitSet colorsToReturn = coloredKBuckets.getAvailableColors();
				//System.out.println("mineAvailableColors: " + colorsToReturn.toString());
				if (hisAvailableColors != null && colorsToReturn != null) {
					colorsToReturn.andNot(hisAvailableColors);
					//System.out.println("to return: " + colorsToReturn.toString());
					int prevIndex = 0;
					for (int i = 0; i < nrColors; i++) {
						final int nextSetBit = colorsToReturn.nextSetBit(prevIndex);
						//System.out.println("nextSetBit: " + nextSetBit);
						if (nextSetBit < 0 || nextSetBit >= nrColors)
							break;

						final List<Node> colorNodesByBit = this.coloredKBuckets.getNodesByColor(nextSetBit);
						if (colorNodesByBit == null || colorNodesByBit.isEmpty()) {
							System.err.println("Error: with nextSetBit, empty temp");
							break;
						}

						colorNodes.add(colorNodesByBit.get(0));
						prevIndex = nextSetBit + 1;
					}
				}
			}

			shadesFindNodeResponse.setNodes(this.coloredKBuckets.getClosestNodesByKey(key, this.kBucketSize))
			.setColorNodes(colorNodes).setNeeded(this.sharingPolicy.isNeeded(key))
			.setIsPopular(this.sharingPolicy.isPopular(key));
		}

		try {
			this.kadServer.send(msg.getSrc(), shadesFindNodeResponse);
		} catch (final IOException e) {
			// could not send back a response
			// nothing to do
			e.printStackTrace();
		}
	}
	@Override
	public void failed(final Throwable exc, final Void attachment) {
		this.printsManager.exception("ShadesFindNodeHandler", exc);
	}

	@Override
	protected Collection<MessageFilter> getFilters() {
		// only accept FindNodeRequests messages
		return Arrays.asList(new MessageFilter[]{new TypeMessageFilter(ShadesFindNodeRequest.class)});
	}
}
