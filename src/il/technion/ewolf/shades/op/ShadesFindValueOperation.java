package il.technion.ewolf.shades.op;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyColorComparator;
import il.technion.ewolf.kbr.KeyComparator;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.ShadesFindNodeRequest;
import il.technion.ewolf.kbr.openkad.msg.ShadesFindNodeResponse;
import il.technion.ewolf.kbr.openkad.msg.StoreMessage;
import il.technion.ewolf.kbr.openkad.net.Communicator;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;
import il.technion.ewolf.kbr.openkad.op.FindValueOperation;
import il.technion.ewolf.shades.buckets.ColoredKBuckets;
import il.technion.utils.PrintsManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Find value operation according to the shades algorithm.
 * 
 * 
 * TODO: Write protocol!
 * 
 * @author Gil, and Yoav.
 * 
 */
public class ShadesFindValueOperation extends FindValueOperation implements CompletionHandler<KadMessage, Node> {

	// state
	private List<Node> knownClosestNodes;
	private List<Node> coloredNodes;
	private Node closestSidewalkNodeQueried;
	private final Set<Node> alreadyQueried;
	private final Set<Node> querying;
	private Node nodeThatReturnedCachedResults = null;
	private List<Node> toShareWith;
	private boolean isPopular;
	private final AtomicInteger nrMsgsSent;

	private final boolean limitedNrSidewalks;
	private final int maxNrSidewalks;
	private int nrSidewalks;

	// dependencies
	private final Provider<ShadesFindNodeRequest> findNodeRequestProvider;
	private final Provider<MessageDispatcher<Node>> msgDispatcherProvider;
	private final Provider<StoreMessage> storeMessageProvider;
	private final ColoredKBuckets coloredKBuckets;
	private final int colorModulu;
	private final Node localNode;
	private final int kBucketSize;
	private final Communicator kadServer;
	private final KadCache cache;
	private final int nrShare;
	private final int nrColors;
	private KeyComparator keyComparator;
	private KeyColorComparator colorComparator;

	// testing
	private final AtomicInteger nrLocalCacheHits;
	private final AtomicInteger nrRemoteCacheHits;
	private final boolean activatePopularity;

	private final boolean inBucketColor;
	private final PrintsManager printsManager;

	//A lock for all side walk operations
	//Only one thread set it to true, and when a reply is returned it set back to false
	private AtomicBoolean pendingSidewalkMessage;

	// Statistics
	private final AtomicInteger nrFirstSideSteps;
	private final AtomicInteger nrSecondSideSteps;
	private final AtomicInteger nrFirstSideStepHits;
	private final AtomicInteger nrFirstSideStepMisses;
	private final AtomicInteger nrSecondSideStepHits;
	private final AtomicInteger nrSecondSideStepMisses;
	private final AtomicInteger nrMsgsBeforeFirstSideStep;
	private final AtomicInteger nrMsgsBeforeSecondSideStep;
	private final AtomicBoolean countIsColorExists;
	private final AtomicInteger nrColorNodeFound;
	private final AtomicInteger nrNoColorNodeFound;

	private final Provider<KadNode> kadNodeProvider;


	@Inject
	ShadesFindValueOperation(final Provider<KadNode> kadNodeProvider, @Named("openkad.local.node") final Node localNode,
			@Named("openkad.local.color") final int localColor, @Named("openkad.bucket.kbuckets.maxsize") final int kBucketSize,
			@Named("openkad.color.slack.modulu") final int colorModulu, @Named("openkad.cache.share") final int nrShare,
			@Named("openkad.color.nrcolors") final int nrColors, final Provider<ShadesFindNodeRequest> findNodeRequestProvider,
			final Provider<MessageDispatcher<Node>> msgDispatcherProvider, final Provider<StoreMessage> storeMessageProvider,
			final Communicator kadServer, final ColoredKBuckets coloredKBuckets, final KadCache cache,
			@Named("openkad.testing.nrLocalCacheHits") final AtomicInteger nrLocalCacheHits,
			@Named("openkad.testing.nrRemoteCacheHits") final AtomicInteger nrRemoteCacheHits,
			@Named("shades.feature.popularity") final boolean activatePopularity,
			@Named("shades.feature.limitedNrSidewalks") final boolean limitedNrSidewalks,
			@Named("shades.feature.maxNrSidewalks") final int maxNrSidewalks,
			@Named("shades.feature.inBucketColor") final boolean inBucketColor,
			@Named("openkad.shades.nrFirstSideSteps") final AtomicInteger nrFirstSideSteps,
			@Named("openkad.shades.nrSecondSideSteps") final AtomicInteger nrSecondSideSteps,
			@Named("openkad.shades.nrFirstSideStepHits") final AtomicInteger nrFirstSideStepHits,
			@Named("openkad.shades.nrFirstSideStepMisses") final AtomicInteger nrFirstSideStepMisses,
			@Named("openkad.shades.nrSecondSideStepHits") final AtomicInteger nrSecondSideStepHits,
			@Named("openkad.shades.nrSecondSideStepMisses") final AtomicInteger nrSecondSideStepMisses,
			@Named("openkad.shades.nrMsgsBeforeFirstSideStep") final AtomicInteger nrMsgsBeforeFirstSideStep,
			@Named("openkad.shades.nrMsgsBeforeSecondSideStep") final AtomicInteger nrMsgsBeforeSecondSideStep,
			@Named("openkad.shades.nrColorNodeFound") final AtomicInteger nrColorNodeFound,
			@Named("openkad.shades.nrNoColorNodeFound") final AtomicInteger nrNoColorNodeFound,
			final PrintsManager printsManager

			) {
		this.kadNodeProvider = kadNodeProvider;
		this.localNode = localNode;
		this.kBucketSize = kBucketSize;
		this.coloredKBuckets = coloredKBuckets;
		this.colorModulu = colorModulu;
		this.findNodeRequestProvider = findNodeRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.nrShare = nrShare;
		this.storeMessageProvider = storeMessageProvider;
		this.kadServer = kadServer;
		this.cache = cache;
		this.nrColors = nrColors;
		this.nrLocalCacheHits = nrLocalCacheHits;
		this.nrRemoteCacheHits = nrRemoteCacheHits;
		this.nrMsgsSent = new AtomicInteger();
		this.nrSidewalks = 0;
		this.alreadyQueried = new HashSet<Node>();
		this.querying = new HashSet<Node>();
		this.toShareWith = new LinkedList<Node>();
		this.isPopular = true;
		this.closestSidewalkNodeQueried = null;
		this.pendingSidewalkMessage = new AtomicBoolean(false);

		this.activatePopularity = activatePopularity;
		this.maxNrSidewalks = maxNrSidewalks;
		this.limitedNrSidewalks = limitedNrSidewalks;
		this.inBucketColor = inBucketColor;
		this.printsManager = printsManager;

		// statistics
		this.nrFirstSideSteps = nrFirstSideSteps;
		this.nrSecondSideSteps = nrSecondSideSteps;
		this.nrFirstSideStepHits = nrFirstSideStepHits;
		this.nrFirstSideStepMisses = nrFirstSideStepMisses;
		this.nrSecondSideStepHits = nrSecondSideStepHits;
		this.nrSecondSideStepMisses = nrSecondSideStepMisses;
		this.nrMsgsBeforeFirstSideStep = nrMsgsBeforeFirstSideStep;
		this.nrMsgsBeforeSecondSideStep = nrMsgsBeforeSecondSideStep;
		this.countIsColorExists = new AtomicBoolean(true);
		this.nrColorNodeFound = nrColorNodeFound;
		this.nrNoColorNodeFound = nrNoColorNodeFound;
	}

	@Override
	public FindValueOperation setKey(final Key key) {
		this.keyComparator = new KeyComparator(key);
		this.colorComparator = new KeyColorComparator(key, this.nrColors);
		return super.setKey(key);
	}

	@Override
	public int getNrQueried() {
		return this.alreadyQueried.size();
	}

	/**
	 * Returns the closest node that is not in querying or alreadyQueried lists
	 */
	private synchronized Node getFirstUnqueriedNode(final List<Node> list) {
		for (int i = 0; i < list.size(); ++i) {
			final Node n = list.get(i);
			if (!this.querying.contains(n) && !this.alreadyQueried.contains(n)) {
				this.querying.add(n);
				return n;
			}
		}
		return null;
	}

	/**
	 * Returns the closest node from knownClosestNodes list that is not in
	 * querying or alreadyQueried lists Gives priority to nodes having the same
	 * color as the key (kaleidoscope)
	 */
	private synchronized Node getFirstUnqueriedNodeFromKnownClosestNodesByColor() {
		List<Node> allUnqueried = new ArrayList<Node>();
		allUnqueried.addAll(this.knownClosestNodes);
		allUnqueried.removeAll(this.querying);
		allUnqueried.removeAll(this.alreadyQueried);

		if (allUnqueried.isEmpty())
			return null;

		Node $ = null;

		if (allUnqueried.size() > 1)
			allUnqueried = sort(allUnqueried, on(Node.class).getKey(), this.colorComparator);
		$ = allUnqueried.get(0);

		// if the best we could find is not in the right color, then continue
		// with the normal kademila lookup
		if ($.getKey().getColor(this.nrColors) != this.key.getColor(this.nrColors))
			return getFirstUnqueriedNode(this.knownClosestNodes);

		this.querying.add($);
		return $;
	}

	/**
	 * Returns the closest node from coloredNodes list that is not in querying
	 * or alreadyQueried lists and is closer then previous queried sidewalk node
	 */
	private synchronized Node getFirstUnqueriedNodeFromColoredNodesAdvancing() {

		//While sidewalkMsgSent is set to true, only one thread tries to do a side walk
		if(!pendingSidewalkMessage.compareAndSet(false, true))
			return null;

		if (!shouldWalkSideways() || this.coloredNodes.isEmpty()){
			pendingSidewalkMessage.set(false);
			return null;
		}

		final Node n = this.coloredNodes.get(0);
		if (this.querying.contains(n) || this.alreadyQueried.contains(n)){
			pendingSidewalkMessage.set(false);
			return null;
		}

		if (this.closestSidewalkNodeQueried != null)
			if (this.keyComparator.compare(this.closestSidewalkNodeQueried.getKey(), n.getKey()) <= 0){
				pendingSidewalkMessage.set(false);
				return null;
			}

		this.closestSidewalkNodeQueried = n;
		this.querying.add(n);

		return n;
	}

	private boolean hasMoreToQuery() {
		return !this.querying.isEmpty() || !this.alreadyQueried.containsAll(this.knownClosestNodes);
	}
	
	private ShadesFindNodeRequest createShadesFindNodeRequest(final Node to){
		final ShadesFindNodeRequest shadesFindNodeRequest = this.findNodeRequestProvider.get().setSearchCache(true)
				.setKey(this.key).setIsJoinPhase(false);
		
		BitSet bs = coloredKBuckets.getAvailableColors();
		if(bs.nextClearBit(0) < nrColors){
			//The Palette is not full
			shadesFindNodeRequest.setAvailableColors(bs);
		}
		
		return shadesFindNodeRequest;
	}

	private boolean trySendFindNode(final Node to) {
		ShadesFindNodeRequest request = createShadesFindNodeRequest(to);
		
		return this.msgDispatcherProvider.get().addFilter(new IdMessageFilter(request.getId()))
				.addFilter(new TypeMessageFilter(ShadesFindNodeResponse.class)).setConsumable(true).setCallback(to, this)
				.trySend(to, request);
	}


	private void sendFindNode(final Node to) {
		ShadesFindNodeRequest request = createShadesFindNodeRequest(to);
		
		//Blocking according to concurrency
		//Bug?- not necessarily sending to the closest node. 
		//(since the blocking is when sending and not when taking the closest node)
		this.msgDispatcherProvider.get().addFilter(new IdMessageFilter(request.getId()))
		.addFilter(new TypeMessageFilter(ShadesFindNodeResponse.class)).setConsumable(true).setCallback(to, this)
		.send(to, request);
	}

	private List<Node> sortAndSlice(final List<Node> list, final int size) {
		final List<Node> tempList = sort(list, on(Node.class).getKey(), this.keyComparator);
		if (tempList.size() >= size)
			tempList.subList(size, tempList.size()).clear();
		return tempList;
	}

	@Override
	public List<Node> doFindValue() {
		// initialization - no concurrency yet. 
		boolean tryingToSendSidewalk = false;


		final List<Node> nodes = this.cache.search(this.key);
		if (nodes != null && nodes.size() >= this.kBucketSize) {
			this.nrLocalCacheHits.incrementAndGet();
			return nodes;
		}

		this.knownClosestNodes = this.coloredKBuckets.getClosestNodesByKey(this.key, this.kBucketSize);
		this.knownClosestNodes.add(this.localNode);
		this.alreadyQueried.add(this.localNode);

		final Collection<Node> bootstrap = getBootstrap();
		bootstrap.removeAll(this.knownClosestNodes);
		this.knownClosestNodes.addAll(bootstrap);
		this.knownClosestNodes = sortAndSlice(this.knownClosestNodes, this.kBucketSize);
		this.coloredNodes = this.coloredKBuckets.getNodesByColor(this.key.getColor(this.nrColors));
		this.coloredNodes = sortAndSlice(this.coloredNodes, 1);

		if (this.countIsColorExists.getAndSet(false))
			if (this.coloredNodes.isEmpty())
				this.nrNoColorNodeFound.incrementAndGet();
			else
				this.nrColorNodeFound.incrementAndGet();

		do {
			Node node = getFirstUnqueriedNodeFromColoredNodesAdvancing();
			if (node == null){
				if (this.inBucketColor)
					node = getFirstUnqueriedNodeFromKnownClosestNodesByColor();
				else
					node = getFirstUnqueriedNode(this.knownClosestNodes);
			} else
			{
				tryingToSendSidewalk = true;
			}

			synchronized (this) {
				// check if finished already...
				if (!hasMoreToQuery() || this.nodeThatReturnedCachedResults != null)
					break;

				if (node != null){
					if(!trySendFindNode(node)){
						try {
							if(this.querying.size() == 1)
							{
								// 15/5: Gil - we change the statistics - before we actually send. 
								// this time we are sending. 
								tryingToSendSidewalk = updateSidewalkStats(tryingToSendSidewalk);
								//If only I try to send I send and block
								this.nrMsgsSent.incrementAndGet();
								sendFindNode(node);
							}
							else
							{
								//If we just canceled the sidewalk - we want to correctly notifiy it.
								//this time we are not sending. 
								if(closestSidewalkNodeQueried!= null && closestSidewalkNodeQueried.getKey().equals(node.getKey()))
								{
									this.closestSidewalkNodeQueried = null;
									pendingSidewalkMessage.set(false);
									tryingToSendSidewalk = false;
								}

								this.querying.remove(node);
								wait();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}else{
						// this means that we actuallys sent a message - if it was a sidewalk 
						// we update its stats. 
						tryingToSendSidewalk = updateSidewalkStats(tryingToSendSidewalk);
					}
				}
				else // node == null
					if (!this.querying.isEmpty())
						try {
							wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
			}
		} while (true);

		this.knownClosestNodes = Collections.unmodifiableList(this.knownClosestNodes);
		//sortAndSlice(this.knownClosestNodes,this.kBucketSize);
		// if(this.localColor == key.getColor(nrColors))
		this.cache.insert(this.key, this.knownClosestNodes);
		sendStoreResults();

		if (this.nodeThatReturnedCachedResults != null)
			this.nrRemoteCacheHits.incrementAndGet();

		return this.knownClosestNodes;
	}

	private boolean updateSidewalkStats(boolean tryingToSendSidewalk) {
		if(tryingToSendSidewalk){
			final int currNrSidewalks = ++this.nrSidewalks;
			if (currNrSidewalks == 1)
				this.nrFirstSideSteps.incrementAndGet();
			if (currNrSidewalks == 2)
				this.nrSecondSideSteps.incrementAndGet();
			this.nrMsgsSent.incrementAndGet();
			tryingToSendSidewalk =false;
		}
		return tryingToSendSidewalk;
	}

	private boolean shouldWalkSideways() {
		if (this.isPopular && ((this.nrMsgsSent.get() % this.colorModulu) == 0)) {
			if (!this.limitedNrSidewalks)
				return true;

			if (this.nrSidewalks < this.maxNrSidewalks)
				return true;
		}
		return false;
	}

	synchronized private void sendStoreResults() {
		final StoreMessage storeMessage = this.storeMessageProvider.get().setKey(this.key).setNodes(this.knownClosestNodes);
		//this code is to be removed for experiments without storage!!!
		if (this.nodeThatReturnedCachedResults == null){
			toShareWith.removeAll(this.knownClosestNodes);
			for (final Node n : this.knownClosestNodes)
				try {
					this.kadServer.send(n, storeMessage);
				} catch (final Exception e) { }
		}


		if (this.toShareWith.size() == 0 || this.nrShare <= 0)
			return;

		this.toShareWith.remove(this.nodeThatReturnedCachedResults);
		this.toShareWith = sortAndSlice(this.toShareWith, this.nrShare);


		// if (this.toShareWith.isEmpty()) {
		// printsManager.prinIncrementt(3, "ShadesFindValueOperation",
		// "no one to share with");
		// } else {
		// printsManager.printList(3, "ShadesFindValueOperation",
		// "sharing with: ", toShareWith);
		// }

		for (final Node n : this.toShareWith)
			try {
				this.kadServer.send(n, storeMessage);
			} catch (final Exception e) {
			}

	}

	@Override
	public synchronized void completed(final KadMessage msg, final Node n) {
		notifyAll();
		this.querying.remove(n);
		this.alreadyQueried.add(n);

		final ShadesFindNodeResponse shadesFindNodeResponse = (ShadesFindNodeResponse) msg;

		// if(printsManager.isDebugMode())
		// printsManager.print(3, "ShadesFindValueOperation", "response from: "
		// + n.getKey() + " isCacheHit: " +
		// shadesFindNodeResponse.isCachedResults());

		if (n.getKey().getColor(this.nrColors) == this.key.getColor(this.nrColors)) {
			// Popularity
			if (this.activatePopularity && this.isPopular && !shadesFindNodeResponse.isPopular())
				this.isPopular = false;


		}
		// Sharing relative to need.
		if (shadesFindNodeResponse.isNeeded())
			this.toShareWith.add(n);

		//Update colored nodes
		final List<Node> colorNodesFromResponse = shadesFindNodeResponse.getColorNodes();
		if (colorNodesFromResponse != null) {
			colorNodesFromResponse.removeAll(this.querying);
			colorNodesFromResponse.removeAll(this.alreadyQueried);
			colorNodesFromResponse.removeAll(this.coloredNodes);
			// add to Palette
			if (colorNodesFromResponse != null)
				for (final Node node : colorNodesFromResponse)
					this.coloredKBuckets.insertToPalette(this.kadNodeProvider.get().setNode(node));
						this.coloredNodes.addAll(colorNodesFromResponse);
						this.coloredNodes = sortAndSlice(this.coloredNodes, 1);
		}

		// The sidewalk has returned an answer
		if (this.closestSidewalkNodeQueried != null && this.closestSidewalkNodeQueried.compareTo(n) == 0) {
			updateSidewalkStatistics(n, shadesFindNodeResponse);

			//Enable more side walks
			this.pendingSidewalkMessage.set(false);
		}

		if (this.nodeThatReturnedCachedResults != null)
			return;

		final List<Node> nodesFromResponse = shadesFindNodeResponse.getNodes();
		if (nodesFromResponse != null) {
			nodesFromResponse.removeAll(this.knownClosestNodes);
			this.knownClosestNodes.addAll(nodesFromResponse);
			this.knownClosestNodes = sortAndSlice(this.knownClosestNodes, this.kBucketSize);
		}

		if (shadesFindNodeResponse.isCachedResults()) {
			this.nodeThatReturnedCachedResults = n;
			return;
		}
	}

	private void updateSidewalkStatistics(final Node n, final ShadesFindNodeResponse shadesFindNodeResponse) {
		final int currNrSidewalks = this.nrSidewalks;

		if (currNrSidewalks == 1) {
			if (shadesFindNodeResponse.isCachedResults())
				this.nrFirstSideStepHits.incrementAndGet();
			else
				this.nrFirstSideStepMisses.incrementAndGet();
			this.nrMsgsBeforeFirstSideStep.addAndGet(this.nrMsgsSent.get());
		}
		if (currNrSidewalks == 2) {
			if (shadesFindNodeResponse.isCachedResults())
				this.nrSecondSideStepHits.incrementAndGet();
			else
				this.nrSecondSideStepMisses.incrementAndGet();
			this.nrMsgsBeforeSecondSideStep.addAndGet(this.nrMsgsSent.get());
		}
	}

	@Override
	public synchronized void failed(final Throwable exc, final Node n) {
		notifyAll();
		this.querying.remove(n);
		this.alreadyQueried.add(n);
		this.printsManager.exception("ShadesFindValueOperation", exc);
	}
}
