package il.technion.ewolf.shades.buckets;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.bucket.KBuckets;

import java.util.BitSet;
import java.util.List;

public interface ColoredKBuckets extends KBuckets {
	public List<Node> getNodesByColor(int color);
	public BitSet getAvailableColors();
	public void insertToPalette(KadNode node);
}
