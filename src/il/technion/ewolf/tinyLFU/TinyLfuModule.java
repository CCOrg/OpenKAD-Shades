package il.technion.ewolf.tinyLFU;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.tinyLFU.LFU.LazyLFU;
import il.technion.ewolf.tinyLFU.LFU.LFUCache;
import il.technion.ewolf.tinyLFU.filter.Histogram;
import il.technion.ewolf.tinyLFU.filter.TinyLfuFilter;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class TinyLfuModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(new TypeLiteral<Histogram<Key>>() {
		}).annotatedWith(Names.named("tinyLfu.histogram")).to(new TypeLiteral<TinyLfuFilter<Key>>() {
		}).in(Scopes.SINGLETON);

		bind(new TypeLiteral<LFUCache<Key>>() {
		}).to(new TypeLiteral<LazyLFU<Key>>() {
		}).in(Scopes.SINGLETON);

		// bind(new TypeLiteral<TinyLfuCache<Key>>() {
		// }).in(Scopes.SINGLETON);

	}

}
