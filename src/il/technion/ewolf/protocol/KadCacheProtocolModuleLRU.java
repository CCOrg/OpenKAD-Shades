package il.technion.ewolf.protocol;

import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNet;
import il.technion.ewolf.kbr.openkad.NodeStorage;
import il.technion.ewolf.kbr.openkad.bucket.KBuckets;
import il.technion.ewolf.kbr.openkad.bucket.KadBuckets;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.cache.LRUKadCache;
import il.technion.ewolf.kbr.openkad.handlers.FindNodeHandler;
import il.technion.ewolf.kbr.openkad.handlers.KademliaFindNodeHandler;
import il.technion.ewolf.kbr.openkad.op.FindNodeOperation;
import il.technion.ewolf.kbr.openkad.op.FindValueOperation;
import il.technion.ewolf.kbr.openkad.op.KadFindNodeOperation;
import il.technion.ewolf.shades.op.StorageKadCacheFindValueOperation;
import il.technion.ewolf.tinyLFU.KadCacheAndStorageWrapper;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class KadCacheProtocolModuleLRU extends AbstractModule {

	@Override
	protected void configure() {
		bind(KadCache.class).to(KadCacheAndStorageWrapper.class).in(Scopes.SINGLETON);
		bind(KadCache.class).annotatedWith(Names.named("openkad.inner.cache")).to(LRUKadCache.class);
		
		bind(KadBuckets.class).in(Scopes.SINGLETON);
		bind(KBuckets.class).to(KadBuckets.class).in(Scopes.SINGLETON);
		bind(NodeStorage.class).to(KadBuckets.class).in(Scopes.SINGLETON);


		bind(FindNodeOperation.class).to(KadFindNodeOperation.class);
		bind(FindNodeHandler.class).to(KademliaFindNodeHandler.class);
		bind(FindValueOperation.class).annotatedWith(Names.named("openkad.op.findvalue")).to(
				StorageKadCacheFindValueOperation.class);
		bind(KeybasedRouting.class).to(KadNet.class).in(Scopes.SINGLETON);

		bind(FindValueOperation.class).annotatedWith(Names.named("openkad.op.lastFindValue")).to(
				StorageKadCacheFindValueOperation.class);

	}

}
