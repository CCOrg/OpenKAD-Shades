package il.technion.ewolf.shades.buckets;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.bucket.Bucket;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.PingRequest;
import il.technion.ewolf.kbr.openkad.msg.PingResponse;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;
import il.technion.utils.PrintsManager;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * A StableBucket that keeps reference to the nodes he stores according to their
 * color see StableBucket for insertion algorithm
 */
public class ShadesStableBucketWithColorRef implements Bucket {

	// state
	private final List<KadNode> bucket;

	// dependencies
	private final int maxSize;
	private final long validTimespan;
	private final Provider<PingRequest> pingRequestProvider;
	private final Provider<MessageDispatcher<Void>> msgDispatcherProvider;
	private final ExecutorService pingExecutor;
	private final NodesByColor nodesByColor;
	private final PrintsManager printsManager;

	@Inject
	public ShadesStableBucketWithColorRef(final int maxSize, @Named("openkad.bucket.valid_timespan") final long validTimespan,
			@Named("openkad.executors.ping") final ExecutorService pingExecutor, final Provider<PingRequest> pingRequestProvider,
			final Provider<MessageDispatcher<Void>> msgDispatcherProvider, final NodesByColor nodesByColor,
			final PrintsManager printsManager) {

		this.maxSize = maxSize;
		this.bucket = new LinkedList<KadNode>();
		this.validTimespan = validTimespan;
		this.pingExecutor = pingExecutor;
		this.pingRequestProvider = pingRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.nodesByColor = nodesByColor;
		this.printsManager = printsManager;
	}

	@Override
	public synchronized void insert(final KadNode n) {
		final int i = this.bucket.indexOf(n);
		if (i != -1) {
			// found node in bucket

			// if heard from n (it is possible to insert n i never had
			// contact with simply by hearing about from another node)
			if (this.bucket.get(i).getLastContact() < n.getLastContact()) {
				final KadNode s = this.bucket.remove(i);
				s.setNodeWasContacted(n.getLastContact());
				this.bucket.add(s);
			}

		} else if (this.bucket.size() < this.maxSize) {
			// not found in bucket and there is enough room for n
			this.bucket.add(n);
			this.nodesByColor.add(n.getNode());
		} else {
			// n is not in bucket and bucket is full

			// don't bother to insert n if I never recved a msg from it
			if (n.hasNeverContacted())
				return;

			// check the first node, ping him if no one else is currently
			// pinging
			final KadNode inBucketReplaceCandidate = this.bucket.get(0);

			// the first node was only inserted indirectly (meaning, I never
			// recved
			// a msg from it !) and I did recv a msg from n.
			if (inBucketReplaceCandidate.hasNeverContacted()) {
				this.bucket.remove(inBucketReplaceCandidate);
				this.bucket.add(n);
				this.nodesByColor.remove(inBucketReplaceCandidate.getNode());
				this.nodesByColor.add(n.getNode());
				return;
			}

			// ping is still valid, don't replace
			if (inBucketReplaceCandidate.isPingStillValid(this.validTimespan))
				return;

			// send ping and act accordingly
			if (inBucketReplaceCandidate.lockForPing())
				sendPing(this.bucket.get(0), n);
		}
	}

	private void sendPing(final KadNode inBucket, final KadNode replaceIfFailed) {

		final PingRequest pingRequest = this.pingRequestProvider.get();

		final MessageDispatcher<Void> dispatcher = this.msgDispatcherProvider.get().setConsumable(true)
				.addFilter(new IdMessageFilter(pingRequest.getId())).addFilter(new TypeMessageFilter(PingResponse.class))
				.setCallback(null, new CompletionHandler<KadMessage, Void>() {

					@Override
					public void completed(final KadMessage msg, final Void nothing) {
						// ping was recved
						inBucket.setNodeWasContacted();
						inBucket.releasePingLock();
						synchronized (ShadesStableBucketWithColorRef.this) {
							if (ShadesStableBucketWithColorRef.this.bucket.remove(inBucket))
								ShadesStableBucketWithColorRef.this.bucket.add(inBucket);
						}
					}

					@Override
					public void failed(final Throwable exc, final Void nothing) {
						// ping was not recved
						synchronized (ShadesStableBucketWithColorRef.this) {
							// try to remove the already in bucket and
							// replace it with the new candidate that we
							// just heard from.
							if (ShadesStableBucketWithColorRef.this.bucket.remove(inBucket))
								// try insert the new candidate
								if (!ShadesStableBucketWithColorRef.this.bucket.add(replaceIfFailed))
									// candidate was already in bucket
									// return the inBucket to be the oldest node
									// in
									// the bucket since we don't want our bucket
									// to shrink unnecessarily
									ShadesStableBucketWithColorRef.this.bucket.add(0, inBucket);
								else {
									ShadesStableBucketWithColorRef.this.nodesByColor.remove(inBucket.getNode());
									ShadesStableBucketWithColorRef.this.nodesByColor.add(replaceIfFailed.getNode());
									ShadesStableBucketWithColorRef.this.printsManager.print(3, "ShadesStableBucketWithColorRef",
											"ping failed to: " + inBucket.getNode().getKey() + " replaced with: "
													+ replaceIfFailed.getNode().getKey());
								}
						}
						inBucket.releasePingLock();
					}
				});

		try {
			this.pingExecutor.execute(new Runnable() {

				@Override
				public void run() {
					dispatcher.send(inBucket.getNode(), pingRequest);
				}
			});
		} catch (final Exception e) {
			inBucket.releasePingLock();
		}
	}

	@Override
	public synchronized void markDead(final Node n) {
		for (int i = 0; i < this.bucket.size(); ++i) {
			final KadNode kadNode = this.bucket.get(i);
			if (kadNode.getNode().equals(n)) {
				// mark dead an move to front
				kadNode.markDead();
				this.bucket.remove(i);
				this.bucket.add(0, kadNode);
			}
		}
	}

	@Override
	public synchronized void addNodesTo(final Collection<Node> c) {
		for (final KadNode n : this.bucket)
			c.add(n.getNode());
	}

	@Override
	public synchronized String toString() {
		return this.bucket.toString();
	}

}
