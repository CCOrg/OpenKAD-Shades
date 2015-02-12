package il.technion.ewolf.shades.buckets;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.bucket.Bucket;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;

public class AtomicReferenceBucket implements Bucket {

	private final AtomicReference<KadNode> atomicReference;

	@Inject
	public AtomicReferenceBucket() {
		this.atomicReference = new AtomicReference<KadNode>();
		this.atomicReference.set(null);
	}

	@Override
	public void insert(final KadNode n) {
		atomicReference.set(n);
	}

	@Override
	public void addNodesTo(final Collection<Node> c) {
		final KadNode n = atomicReference.get();
		if (n != null)
			c.add(n.getNode());
	}

	@Override
	public void markDead(final Node n) {
		// atomicReference.set(null);
	}

}
