package il.technion.ewolf.shades.sharingPolicy;

import il.technion.ewolf.kbr.Key;

public interface SharingPolicy {
	public boolean isNeeded(Key key);
	public boolean isPopular(Key key);
}
