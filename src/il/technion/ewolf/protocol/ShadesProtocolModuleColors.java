package il.technion.ewolf.protocol;

import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNet;
import il.technion.ewolf.kbr.openkad.NodeStorage;
import il.technion.ewolf.kbr.openkad.bucket.Bucket;
import il.technion.ewolf.kbr.openkad.bucket.KBuckets;
import il.technion.ewolf.kbr.openkad.bucket.StableBucket;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.handlers.FindNodeHandler;
import il.technion.ewolf.kbr.openkad.msg.PingRequest;
import il.technion.ewolf.kbr.openkad.msg.ShadesFindNodeRequest;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.op.FindNodeOperation;
import il.technion.ewolf.kbr.openkad.op.FindValueOperation;
import il.technion.ewolf.shades.buckets.AtomicReferenceBucket;
import il.technion.ewolf.shades.buckets.ColorPalette;
import il.technion.ewolf.shades.buckets.ColoredKBuckets;
import il.technion.ewolf.shades.buckets.NodesByColor;
import il.technion.ewolf.shades.buckets.ShadesBucketsWithColorRefAndPalette;
import il.technion.ewolf.shades.handlers.ShadesFindNodeHandler;
import il.technion.ewolf.shades.op.ShadesFindNodeOperation;
import il.technion.ewolf.shades.op.ShadesFindValueOperation;
import il.technion.ewolf.shades.sharingPolicy.ColorLFUSharingPolicy;
import il.technion.ewolf.shades.sharingPolicy.SharingPolicy;
import il.technion.ewolf.tinyLFU.TinyLfuKadCacheAndStorageWrapperColorful;
import il.technion.utils.PrintsManager;

import java.util.BitSet;
import java.util.concurrent.ExecutorService;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class ShadesProtocolModuleColors extends AbstractModule {

	@Override
	protected void configure() {
		bind(ShadesFindNodeRequest.class);
		bind(NodesByColor.class).in(Scopes.SINGLETON);
		bind(ColorPalette.class).in(Scopes.SINGLETON);
		bind(ShadesBucketsWithColorRefAndPalette.class).in(Scopes.SINGLETON);
		bind(KBuckets.class).to(ShadesBucketsWithColorRefAndPalette.class).in(Scopes.SINGLETON);
		bind(ColoredKBuckets.class).to(ShadesBucketsWithColorRefAndPalette.class).in(Scopes.SINGLETON);

		bind(KadCache.class).to(TinyLfuKadCacheAndStorageWrapperColorful.class).in(Scopes.SINGLETON);
		bind(NodeStorage.class).to(ShadesBucketsWithColorRefAndPalette.class).in(Scopes.SINGLETON);
		bind(FindNodeOperation.class).to(ShadesFindNodeOperation.class);
		bind(FindNodeHandler.class).to(ShadesFindNodeHandler.class);
		bind(SharingPolicy.class).to(ColorLFUSharingPolicy.class).in(Scopes.SINGLETON);

		bind(FindValueOperation.class).annotatedWith(Names.named("openkad.op.findvalue")).to(ShadesFindValueOperation.class);
		bind(FindValueOperation.class).annotatedWith(Names.named("openkad.op.lastFindValue")).to(ShadesFindValueOperation.class);

		bind(KeybasedRouting.class).to(KadNet.class).in(Scopes.SINGLETON);
	}

	@Provides
	@Singleton
	@Named("shades.bucket.availableColors")
	BitSet provideAvailableColorsBitSet(@Named("openkad.color.nrcolors") final int nrColors) {
		return new BitSet(nrColors);
	}

	// Creating a StableBucket sized according to
	// "shades.bucket.mycolorbucket.maxsize"
	@Provides
	@Named("shades.bucket.mycolorbucket")
	Bucket provideMyColorBucket(@Named("shades.bucket.mycolorbucket.maxsize") final int maxSize,
			@Named("openkad.bucket.valid_timespan") final long validTimespan,
			@Named("openkad.executors.ping") final ExecutorService pingExecutor, final Provider<PingRequest> pingRequestProvider,
			final Provider<MessageDispatcher<Void>> msgDispatcherProvider, final PrintsManager printsManager) {
		return new StableBucket(maxSize, validTimespan, pingExecutor, pingRequestProvider, msgDispatcherProvider);
	}

	@Provides
	@Named("shades.bucket.complementarycolorbucket")
	Bucket provideComplementaryColorBucket() {
		return new AtomicReferenceBucket();
	}

	// Creating an array of buckets, one for each color
	@Provides
	@Singleton
	@Named("shades.bucket.colorBuckets")
	Bucket[] provideColorBuckets(@Named("openkad.color.nrcolors") final int nrColors,
			@Named("openkad.local.color") final int localColor,
			@Named("shades.bucket.mycolorbucket") final Provider<Bucket> myColorBucketProvider,
			@Named("shades.bucket.complementarycolorbucket") final Provider<Bucket> complementaryColorBucketProvider) {
		final Bucket[] colorBuckets = new Bucket[nrColors];
		for (int i = 0; i < nrColors; i++)
			if (i == localColor)
				colorBuckets[i] = myColorBucketProvider.get();
			else
				colorBuckets[i] = complementaryColorBucketProvider.get();
		return colorBuckets;
	}

}
