package il.technion.ewolf.shades.op;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyComparator;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.bucket.KBuckets;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.ShadesFindNodeRequest;
import il.technion.ewolf.kbr.openkad.msg.ShadesFindNodeResponse;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;
import il.technion.ewolf.kbr.openkad.op.FindNodeOperation;
import il.technion.ewolf.shades.buckets.ColoredKBuckets;
import il.technion.utils.PrintsManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Find node operation as defined in the kademlia algorithm
 * 
 * @author eyal.kibbar@gmail.com
 * 
 */
public class ShadesFindNodeOperation implements CompletionHandler<KadMessage, Node>, FindNodeOperation {

	// state
	private List<Node> knownClosestNodes;
	private Key key;
	private final Set<Node> alreadyQueried;
	private final Set<Node> querying;
	private int nrQueried;

	// dependencies
	private final Provider<ShadesFindNodeRequest> findNodeRequestProvider;
	private final Provider<MessageDispatcher<Node>> msgDispatcherProvider;
	private final int kBucketSize;
	private final KBuckets kBuckets;
	private final Node localNode;
	private KeyComparator keyComparator;
	private final ColoredKBuckets coloredKBuckets;
	private final Provider<KadNode> kadNodeProvider;
	private final PrintsManager printsManager;
	@Inject
	ShadesFindNodeOperation(final Provider<KadNode> kadNodeProvider, @Named("openkad.local.node") final Node localNode,
			@Named("openkad.bucket.kbuckets.maxsize") final int kBucketSize,
			final Provider<ShadesFindNodeRequest> findNodeRequestProvider,
			final Provider<MessageDispatcher<Node>> msgDispatcherProvider, final KBuckets kBuckets,
			final ColoredKBuckets coloredKBuckets, final PrintsManager printsManager) {
		this.kadNodeProvider = kadNodeProvider;
		this.localNode = localNode;
		this.kBucketSize = kBucketSize;
		this.kBuckets = kBuckets;
		this.findNodeRequestProvider = findNodeRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.coloredKBuckets = coloredKBuckets;
		this.printsManager = printsManager;
		this.alreadyQueried = new HashSet<Node>();
		this.querying = new HashSet<Node>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.op.FindNodeOperation#getNrQueried()
	 */
	@Override
	public int getNrQueried() {
		return this.nrQueried;
	}

	private synchronized Node takeUnqueried() {
		for (int i = 0; i < this.knownClosestNodes.size(); ++i) {
			final Node n = this.knownClosestNodes.get(i);
			if (!this.querying.contains(n) && !this.alreadyQueried.contains(n)) {
				this.querying.add(n);
				return n;
			}
		}
		return null;
	}

	private boolean hasMoreToQuery() {
		return !this.querying.isEmpty() || !this.alreadyQueried.containsAll(this.knownClosestNodes);
	}

	private void sendFindNode(final Node to) {
		final ShadesFindNodeRequest shadesFindNodeRequest = this.findNodeRequestProvider.get().setSearchCache(false)
				.setKey(this.key).setIsJoinPhase(true).setAvailableColors(coloredKBuckets.getAvailableColors());

		this.msgDispatcherProvider.get().addFilter(new IdMessageFilter(shadesFindNodeRequest.getId()))
				.addFilter(new TypeMessageFilter(ShadesFindNodeResponse.class)).setConsumable(true).setCallback(to, this)
				.send(to, shadesFindNodeRequest);
	}

	@Override
	public FindNodeOperation setKey(final Key key) {
		this.keyComparator = new KeyComparator(key);
		this.key = key;
		return this;
	}

	private List<Node> sortAndSlice(final List<Node> list, final int size) {
		final List<Node> tempList = sort(list, on(Node.class).getKey(), this.keyComparator);
		if (tempList.size() >= size)
			tempList.subList(size, tempList.size()).clear();
		return tempList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.op.FindNodeOperation#doFindNode()
	 */
	@Override
	public List<Node> doFindNode() {
		this.knownClosestNodes = this.kBuckets.getClosestNodesByKey(this.key, this.kBucketSize);
		this.knownClosestNodes.add(this.localNode);
		this.alreadyQueried.add(this.localNode);
		this.knownClosestNodes = sortAndSlice(this.knownClosestNodes, this.kBucketSize);

		do {
			final Node n = takeUnqueried();

			if (n != null)
				try {
					sendFindNode(n);
				} catch (final Throwable e) {
					e.printStackTrace();
				}
			else
				synchronized (this) {
					if (!this.querying.isEmpty())
						try {
							wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
				}

			synchronized (this) {
				if (!hasMoreToQuery())
					break;
			}

		} while (true);

		this.knownClosestNodes = Collections.unmodifiableList(this.knownClosestNodes);

		synchronized (this) {
			this.nrQueried = this.alreadyQueried.size() - 1 + this.querying.size();
		}

		return this.knownClosestNodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * il.technion.ewolf.kbr.openkad.op.FindNodeOperation#completed(il.technion
	 * .ewolf.kbr.openkad.msg.KadMessage, il.technion.ewolf.kbr.Node)
	 */
	@Override
	public synchronized void completed(final KadMessage msg, final Node n) {
		notifyAll();
		this.querying.remove(n);
		this.alreadyQueried.add(n);

		final ShadesFindNodeResponse shadesFindNodeResponse = (ShadesFindNodeResponse) msg;
		final List<Node> nodesFromResponse = shadesFindNodeResponse.getNodes();
		// this.printsManager.print(3, "ShadesFindNodeOperation",
		// "nodesFromResponse size: " + nodesFromResponse.size());
		if (nodesFromResponse != null) {
			nodesFromResponse.removeAll(this.querying);
			nodesFromResponse.removeAll(this.alreadyQueried);
			nodesFromResponse.removeAll(this.knownClosestNodes);
			this.knownClosestNodes.addAll(nodesFromResponse);
			this.knownClosestNodes = sortAndSlice(this.knownClosestNodes, this.kBucketSize);
		}

		final List<Node> colorNodesFromResponse = shadesFindNodeResponse.getColorNodes();
		// this.printsManager.print(3, "ShadesFindNodeOperation",
		// "colorNodesFromResponse size: " + colorNodesFromResponse.size());
		if (colorNodesFromResponse != null)
			for (final Node node : colorNodesFromResponse)
				this.coloredKBuckets.insertToPalette(this.kadNodeProvider.get().setNode(node));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * il.technion.ewolf.kbr.openkad.op.FindNodeOperation#failed(java.lang.Throwable
	 * , il.technion.ewolf.kbr.Node)
	 */
	@Override
	public synchronized void failed(final Throwable exc, final Node n) {
		this.printsManager.exception("ShadesFindNodeOperation", exc);
		notifyAll();
		this.querying.remove(n);
		this.alreadyQueried.add(n);
	}
}
