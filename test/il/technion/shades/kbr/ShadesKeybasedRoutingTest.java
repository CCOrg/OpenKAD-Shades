package il.technion.shades.kbr;

import il.technion.ewolf.kbr.DefaultMessageHandler;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.RandomKeyFactory;
import il.technion.ewolf.protocol.KadCacheProtocolModule;
import il.technion.ewolf.protocol.LocalCacheProtocolModule;
import il.technion.ewolf.protocol.ShadesProtocolModule;
import il.technion.ewolf.shades.ShadesFrameworkModule;
import il.technion.ewolf.tinyLFU.TinyLfuModule;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
public class ShadesKeybasedRoutingTest {

	@Test
	public void the2NodesShouldFindEachOther() throws Throwable {
		final int basePort = 10000;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i = 0; i < 2; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "1")
							.setProperty("openkad.bucket.kbuckets.maxsize", "3").setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(),
					new ShadesProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}

		kbrs.get(1).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + basePort + "/")));
		System.out.println("finished joining");

		for (int i = 0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}

		List<Node> findNode = kbrs.get(1).findNode(kbrs.get(0).getLocalNode().getKey());
		for (final Node node : findNode)
			System.out.println(node.toString());
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(1));

		findNode = kbrs.get(0).findNode(kbrs.get(0).getLocalNode().getKey());
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(1));

		findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey());
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(1));

		findNode = kbrs.get(1).findNode(kbrs.get(1).getLocalNode().getKey());
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(1));

		System.out.println(findNode);

	}

	@Test
	public void the16NodesShouldFindEachOther() throws Throwable {
		final int basePort = 16100;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i = 0; i < 16; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "2")
							.setProperty("openkad.bucket.kbuckets.maxsize", "3").setProperty("openkad.color.nrcolors", "7")
							.setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(),
					new ShadesProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + i - 1;
			System.out.println(i + " ==> " + (i - 1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");

		// kbrs.get(0).findNode(kbrs.get(0).getKeyFactory().generate());

		for (int i = 0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}
		for (int j = 0; j < kbrs.size(); ++j) {
			final Set<List<Node>> findNodeResults = new HashSet<List<Node>>();
			for (int i = 0; i < kbrs.size(); ++i) {
				final List<Node> findNode = kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey());
				System.out.println(findNode);
				findNodeResults.add(findNode);
			}

			if (findNodeResults.size() != 1)
				for (final List<Node> n : findNodeResults)
					System.err.println(n);
			Assert.assertEquals(1, findNodeResults.size());
		}
	}

	/*
	 * @Test public void the64NodesShouldFindEachOtherAsynchronously() throws
	 * Throwable { int basePort = 10800; List<KeybasedRouting> kbrs = new
	 * ArrayList<KeybasedRouting>(); for (int i=0; i < 64; ++i) { Injector
	 * injector = Guice.createInjector(new KadNetModule()
	 * .setProperty("openkad.keyfactory.keysize", "2")
	 * .setProperty("openkad.bucket.kbuckets.maxsize", "5")
	 * .setProperty("openkad.seed", ""+(i+basePort))
	 * .setProperty("openkad.net.udp.port", ""+(i+basePort))); KeybasedRouting
	 * kbr = injector.getInstance(KeybasedRouting.class); kbr.create();
	 * kbrs.add(kbr); }
	 * 
	 * for (int i=1; i < kbrs.size(); ++i) { int port = basePort + i -1;
	 * System.out.println(i+" ==> "+(i-1)); kbrs.get(i).join(Arrays.asList(new
	 * URI("openkad.udp://127.0.0.1:"+port+"/"))); }
	 * 
	 * System.out.println("finished joining");
	 * 
	 * 
	 * for (int i=0; i < kbrs.size(); ++i) { System.out.println(kbrs.get(i));
	 * System.out.println("======"); }
	 * 
	 * List<Future<List<Node>>> futures = new ArrayList<Future<List<Node>>>();
	 * 
	 * for (int j=0; j < kbrs.size(); ++j) { for (int i=0; i < kbrs.size(); ++i)
	 * { futures.add(kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey(),
	 * 5)); //System.out.println(findNode); //findNodeResults.add(findNode); } }
	 * int i=0; for (Future<List<Node>> f : futures) { System.out.println(i++);
	 * f.get(); } }
	 */

	@Test
	public void the64NodesShouldFindEachOther() throws Throwable {
		final int basePort = 10200;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		final List<Statistics> stats = new ArrayList<Statistics>();
		final Random rnd = new Random(10200);
		for (int i = 0; i < 64; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "3")
							.setProperty("openkad.bucket.kbuckets.maxsize", "3").setProperty("openkad.color.nrcolors", "100")
							.setProperty("openkad.cache.size", "10").setProperty("openkad.net.concurrency", "3")
							.setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(), new StatisticsModule(),
					new ShadesProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
			stats.add(injector.getInstance(Statistics.class));
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + rnd.nextInt(i);
			System.out.println(i + " ==> " + (port - basePort));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");

		for (int j = 0; j < stats.size(); ++j)
			stats.get(j).nrHandledMsgs.set(0);

		for (int i = 0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}

		// RandomKeyFactory randomKeyFactory = new RandomKeyFactory(3, rnd,
		// "sha-1");
		for (int j = 0; j < kbrs.size(); ++j) {
			System.out.println("j: " + j);
			final Set<List<Node>> findNodeResults = new HashSet<List<Node>>();
			for (int i = 0; i < kbrs.size(); ++i) {
				// System.out.println("=======================================================");
				final List<Node> findNode = kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey());
				// List<Node> findNode =
				// kbrs.get(i).findNode(randomKeyFactory.generate());
				System.out.println(findNode);
				// System.out.println("=======================================================");
				findNodeResults.add(findNode);
			}

			if (findNodeResults.size() != 1)
				for (final List<Node> r : findNodeResults)
					System.err.println(r);
			Assert.assertEquals(1, findNodeResults.size());
		}

		int total = 0;
		for (int j = 0; j < stats.size(); ++j) {
			final int curr = stats.get(j).nrHandledMsgs.get();
			System.out.println("node: " + j + " nrHandledMsgs: " + curr);
			total += curr;
		}
		System.out.println("total: " + total);
	}
	@Test
	public void the64NodesShouldFindEachOtherKadCache() throws Throwable {
		final int basePort = 11200;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		final List<Statistics> stats = new ArrayList<Statistics>();
		final Random rnd = new Random(10200);
		for (int i = 0; i < 64; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "3")
							.setProperty("openkad.bucket.kbuckets.maxsize", "3").setProperty("openkad.color.nrcolors", "100")
							.setProperty("openkad.cache.size", "100").setProperty("openkad.net.concurrency", "3")
							.setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(), new StatisticsModule(),
					new KadCacheProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
			stats.add(injector.getInstance(Statistics.class));
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + rnd.nextInt(i);
			System.out.println(i + " ==> " + (port - basePort));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");

		for (int j = 0; j < stats.size(); ++j)
			stats.get(j).nrHandledMsgs.set(0);

		for (int i = 0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}

		// RandomKeyFactory randomKeyFactory = new RandomKeyFactory(3, rnd,
		// "sha-1");
		for (int j = 0; j < kbrs.size(); ++j) {
			System.out.println("j: " + j);
			final Set<List<Node>> findNodeResults = new HashSet<List<Node>>();
			for (int i = 0; i < kbrs.size(); ++i) {
				// System.out.println("=======================================================");
				final List<Node> findNode = kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey());
				// List<Node> findNode =
				// kbrs.get(i).findNode(randomKeyFactory.generate());
				System.out.println(findNode);
				// System.out.println("=======================================================");
				findNodeResults.add(findNode);
			}

			if (findNodeResults.size() != 1)
				for (final List<Node> r : findNodeResults)
					System.err.println(r);
			Assert.assertEquals(1, findNodeResults.size());
		}

		int total = 0;
		for (int j = 0; j < stats.size(); ++j) {
			final int curr = stats.get(j).nrHandledMsgs.get();
			System.out.println("node: " + j + " nrHandledMsgs: " + curr);
			total += curr;
		}
		System.out.println("total: " + total);
	}

	@Test
	public void the64NodesShouldFindEachOtherLocalCache() throws Throwable {
		final int basePort = 18200;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		final List<Statistics> stats = new ArrayList<Statistics>();
		final Random rnd = new Random(10200);
		for (int i = 0; i < 64; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "3")
							.setProperty("openkad.bucket.kbuckets.maxsize", "3").setProperty("openkad.color.nrcolors", "100")
							.setProperty("openkad.cache.size", "100").setProperty("openkad.net.concurrency", "3")
							.setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(), new StatisticsModule(),
					new LocalCacheProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
			stats.add(injector.getInstance(Statistics.class));
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + rnd.nextInt(i);
			System.out.println(i + " ==> " + (port - basePort));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");

		for (int j = 0; j < stats.size(); ++j)
			stats.get(j).nrHandledMsgs.set(0);

		for (int i = 0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}

		// RandomKeyFactory randomKeyFactory = new RandomKeyFactory(3, rnd,
		// "sha-1");
		for (int j = 0; j < kbrs.size(); ++j) {
			System.out.println("j: " + j);
			final Set<List<Node>> findNodeResults = new HashSet<List<Node>>();
			for (int i = 0; i < kbrs.size(); ++i) {
				// System.out.println("=======================================================");
				final List<Node> findNode = kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey());
				// List<Node> findNode =
				// kbrs.get(i).findNode(randomKeyFactory.generate());
				System.out.println(findNode);
				// System.out.println("=======================================================");
				findNodeResults.add(findNode);
			}

			if (findNodeResults.size() != 1)
				for (final List<Node> r : findNodeResults)
					System.err.println(r);
			Assert.assertEquals(1, findNodeResults.size());
		}

		int total = 0;
		for (int j = 0; j < stats.size(); ++j) {
			final int curr = stats.get(j).nrHandledMsgs.get();
			System.out.println("node: " + j + " nrHandledMsgs: " + curr);
			total += curr;
		}
		System.out.println("total: " + total);
	}

	@Test
	public void the200NodesFindingRandomKeys() throws Throwable {
		final int basePort = 10600;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		final List<Statistics> stats = new ArrayList<Statistics>();
		final Random rnd = new Random(10200);
		for (int i = 0; i < 200; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "4")
							.setProperty("openkad.bucket.kbuckets.maxsize", "4").setProperty("openkad.bucket.colors.nrcolors", "7")
							.setProperty("openkad.color.allcolors", "30").setProperty("openkad.cache.size", "10")
							.setProperty("openkad.net.concurrency", "3").setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(), new StatisticsModule(),
					new ShadesProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
			stats.add(injector.getInstance(Statistics.class));
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + rnd.nextInt(i);
			System.out.println(i + " ==> " + (port - basePort));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");

		for (int j = 0; j < stats.size(); ++j)
			stats.get(j).nrHandledMsgs.set(0);

		for (int i = 0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}

		final RandomKeyFactory randomKeyFactory = new RandomKeyFactory(4, rnd, "sha-1");
		for (int j = 0; j < 1; ++j) {
			System.out.println("j: " + j);
			final Set<List<Node>> findNodeResults = new HashSet<List<Node>>();
			for (int i = 0; i < kbrs.size(); ++i) {
				System.out.println("=======================================================");
				// List<Node> findNode =
				// kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey());
				final List<Node> findNode = kbrs.get(i).findNode(randomKeyFactory.generate());
				System.out.println(findNode);
				System.out.println("=======================================================");
				findNodeResults.add(findNode);
			}

			if (findNodeResults.size() != 1)
				for (final List<Node> r : findNodeResults)
					System.err.println(r);
		}

		int total = 0;
		for (int j = 0; j < stats.size(); ++j) {
			final int curr = stats.get(j).nrHandledMsgs.get();
			System.out.println("node: " + j + " nrHandledMsgs: " + curr);
			total += curr;
		}
		System.out.println("total: " + total);
	}

	@Test(timeout = 5000)
	public void the2NodesShouldAbleToSendMessages() throws Throwable {
		final int basePort = 10300;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i = 0; i < 2; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "1")
							.setProperty("openkad.bucket.kbuckets.maxsize", "1").setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(), new StatisticsModule(),
					new ShadesProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + i - 1;
			System.out.println(i + " ==> " + (i - 1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");
		final AtomicBoolean isDone = new AtomicBoolean(false);
		kbrs.get(1).register("tag", new DefaultMessageHandler() {

			@Override
			public void onIncomingMessage(final Node from, final String tag, final Serializable content) {
				Assert.assertEquals("msg", content);
				synchronized (isDone) {
					isDone.set(true);
					isDone.notifyAll();
				}
			}
		});

		final List<Node> findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey());

		System.out.println("sending msg");
		kbrs.get(0).sendMessage(findNode.get(0), "tag", "msg");

		synchronized (isDone) {
			while (!isDone.get())
				isDone.wait();
		}
	}
	enum Y {
		A, B, C
	}

	private static final class X implements Serializable {

		private static final long serialVersionUID = -5254444279440929179L;

		int a;
		int b;
		String c;

		public X(final int a, final int b, final String c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}

		@Override
		public int hashCode() {
			return 0;
		}
		@Override
		public boolean equals(final Object obj) {
			if (obj == null || !getClass().equals(obj.getClass()))
				return false;
			final X x = (X) obj;
			return x.a == a && x.b == b && x.c.equals(c);
		}
	}

	@Test(timeout = 5000)
	public void the2NodesShouldAbleToSendArbitrarySerializableMessages() throws Throwable {
		final int basePort = 10400;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i = 0; i < 2; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "1")
							.setProperty("openkad.bucket.kbuckets.maxsize", "1").setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(), new StatisticsModule(),
					new ShadesProtocolModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + i - 1;
			System.out.println(i + " ==> " + (i - 1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");
		final X x = new X(1, 2, "aaa");
		final AtomicBoolean isDone = new AtomicBoolean(false);
		kbrs.get(1).register("tag", new DefaultMessageHandler() {

			@Override
			public void onIncomingMessage(final Node from, final String tag, final Serializable content) {
				Assert.assertEquals(x, content);
				synchronized (isDone) {
					isDone.set(true);
					isDone.notifyAll();
				}
			}
		});

		final List<Node> findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey());

		System.out.println("sending msg");
		kbrs.get(0).sendMessage(findNode.get(0), "tag", x);

		synchronized (isDone) {
			while (!isDone.get())
				isDone.wait();
		}
	}

	@Test(timeout = 5000)
	public void the2NodesShouldAbleToSendArbitrarySerializableRequests() throws Throwable {
		final int basePort = 10500;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i = 0; i < 2; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "1")
							.setProperty("openkad.bucket.kbuckets.maxsize", "1").setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(),
					new ShadesProtocolModule(), new StatisticsModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + i - 1;
			System.out.println(i + " ==> " + (i - 1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");
		final X x = new X(1, 2, "abc");
		kbrs.get(1).register("tag", new DefaultMessageHandler() {
			@Override
			public Serializable onIncomingRequest(final Node from, final String tag, final Serializable content) {
				Assert.assertEquals(x, content);
				return new X(3, 4, "edf");
			}
		});

		final List<Node> findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey());

		final Serializable res = kbrs.get(0).sendRequest(findNode.get(0), "tag", x).get();
		Assert.assertEquals(new X(3, 4, "edf"), res);
	}

	@Test(timeout = 30000)
	public void the16NodesShouldAbleToSendMessages() throws Throwable {
		final int basePort = 11600;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i = 0; i < 16; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "5")
							.setProperty("openkad.bucket.kbuckets.maxsize", "5").setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(),
					new ShadesProtocolModule(), new StatisticsModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + i - 1;
			System.out.println(i + " ==> " + (i - 1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");
		final AtomicBoolean isDone = new AtomicBoolean(false);
		kbrs.get(13).register("tag", new DefaultMessageHandler() {

			@Override
			public void onIncomingMessage(final Node from, final String tag, final Serializable content) {
				Assert.assertEquals("msg", content);
				System.out.println("got " + content);
				synchronized (isDone) {
					isDone.set(true);
					isDone.notifyAll();
				}
			}
		});

		final List<Node> findNode = kbrs.get(0).findNode(kbrs.get(13).getLocalNode().getKey());

		kbrs.get(2).sendMessage(findNode.get(0), "tag", "msg");

		synchronized (isDone) {
			while (!isDone.get())
				isDone.wait();
		}
	}

	@Test(timeout = 5000)
	public void the2NodesShouldAbleToSendRequest() throws Throwable {
		final int basePort = 11700;
		final List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i = 0; i < 2; ++i) {
			final Injector injector = Guice.createInjector(
					new ShadesFrameworkModule().setProperty("openkad.keyfactory.keysize", "1")
							.setProperty("openkad.bucket.kbuckets.maxsize", "1").setProperty("openkad.seed", "" + (i + basePort))
							.setProperty("openkad.net.udp.port", "" + (i + basePort)), new TinyLfuModule(),
					new ShadesProtocolModule(), new StatisticsModule());
			final KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}

		for (int i = 1; i < kbrs.size(); ++i) {
			final int port = basePort + i - 1;
			System.out.println(i + " ==> " + (i - 1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:" + port + "/")));
		}

		System.out.println("finished joining");

		kbrs.get(1).register("tag", new DefaultMessageHandler() {
			@Override
			public Serializable onIncomingRequest(final Node from, final String tag, final Serializable content) {
				Assert.assertEquals("msg", content);
				return "new_msg";
			}
		});

		final List<Node> findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey());

		final Serializable res = kbrs.get(0).sendRequest(findNode.get(0), "tag", "msg").get();
		Assert.assertEquals("new_msg", res);
	}

	@Test
	public void testKeys() throws Throwable {

		final Random rnd = new Random(0);
		final RandomKeyFactory randomKeyFactory = new RandomKeyFactory(3, rnd, "sha-1");
		System.out.println(randomKeyFactory.generate(0).toBinaryString());
		System.out.println(randomKeyFactory.generate(1).toBinaryString());
		System.out.println(randomKeyFactory.generate(2).toBinaryString());
		System.out.println(randomKeyFactory.generate(3).toBinaryString());
		System.out.println(randomKeyFactory.generate(4).toBinaryString());
		System.out.println(randomKeyFactory.generate(5).toBinaryString());
		System.out.println(randomKeyFactory.generate(6).toBinaryString());
		System.out.println(randomKeyFactory.generate(7).toBinaryString());
		System.out.println(randomKeyFactory.generate(8).toBinaryString());
		System.out.println(randomKeyFactory.generate(9).toBinaryString());
		System.out.println(randomKeyFactory.generate(10).toBinaryString());
		System.out.println(randomKeyFactory.generate(11).toBinaryString());

		System.out.println("=================================================");
		final int nrColors = 128;
		final int[] hist = new int[nrColors];
		Key key = randomKeyFactory.generate(8);
		for (int i = 0; i < 10000000; i++) {
			hist[key.getColor(nrColors)]++;
			key = randomKeyFactory.generate(8);
		}

		System.out.print("Histogram: ");
		for (int i = 0; i < nrColors; i++)
			System.out.print(hist[i] + ",");

	}
}
