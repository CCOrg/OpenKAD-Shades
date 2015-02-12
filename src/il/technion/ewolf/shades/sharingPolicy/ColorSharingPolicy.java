package il.technion.ewolf.shades.sharingPolicy;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;

public class ColorSharingPolicy implements SharingPolicy{

	private final Node localNode;
	private final int nrColors;

	@Inject
	public ColorSharingPolicy(
			@Named("openkad.local.node") Node localNode, 
			@Named("openkad.color.nrColors") int nrColors){
		this.localNode = localNode;
		this.nrColors = nrColors;
	}
	
	@Override
	public boolean isNeeded(Key key) {	
		return localNode.getKey().getColor(nrColors) == key.getColor(nrColors);
	}

	@Override
	public boolean isPopular(Key key) {
		return true;
	}
}
