package com.redhat.demo.service;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.redhat.demo.model.Datapoint;

import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DataGridService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("CacheService");
	
	private static AtomicInteger atomicCount = new AtomicInteger();

	private RemoteCacheManager remoteCacheManager; 
	
	private static final String CACHE_XML_CONFIG =
         "<infinispan><cache-container>" +
			"  <distributed-cache-configuration name=\"%s\" statistics=\"false\" statistics-available=\"false\" segments=\"512\" owners=\"1\">" +
			"		<memory><binary strategy=\"NONE\">" +
			"		</binary></memory>" +
			"		<encoding>" +
      		"			<key media-type=\"text/plain; charset=UTF-8\"/>" +
      		"			<value media-type=\"application/x-protostream\"/>" +
	  		"		</encoding>" +
			"  </distributed-cache-configuration>" +
		"</cache-container></infinispan>";
	
	@PostConstruct
    private void init() throws IOException {
        LOGGER.info("init HotRod client");
		
		ConfigurationBuilder builder = new ConfigurationBuilder();
		Properties properties = new Properties();

		try {
			properties.load(getClass().getResourceAsStream("/hotrod-client.properties"));
			builder.withProperties(properties);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

		remoteCacheManager = new RemoteCacheManager(builder.build());
		
		// Get the serialization context of the client
		SerializationContext ctx = MarshallerUtil.getSerializationContext(remoteCacheManager);
	
		// Use ProtoSchemaBuilder to define a Protobuf schema on the client
		ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
		String fileName = "datapoint.proto";
		String protoFile = protoSchemaBuilder
				.fileName(fileName)
				.addClass(Datapoint.class)
				.packageName("datapoint")
				.build(ctx);
	
		// Retrieve metadata cache
		RemoteCache<String, String> metadataCache =
		remoteCacheManager.getCache(PROTOBUF_METADATA_CACHE_NAME);
	
		// Define the new schema on the server too
		metadataCache.put(fileName, protoFile);
	}
	
	
	void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Starting Quarkus app... " + remoteCacheManager.getConfiguration().toString());
    }
	
	public boolean createCache(String name) {
		try {
			remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
				.getOrCreateCache(name, new XMLStringConfiguration(String.format(CACHE_XML_CONFIG, name)));
			return true;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return false;
		}
		
	}
	
	public boolean removeCache(String name) {
		try {
			remoteCacheManager.administration().removeCache(name);
			return true;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return false;
		}
		
	}
	
	public String fillStringCache(int numentries, String name, int threadNum) {
		LOGGER.info("Fill Cache '"+ name+ "' with String Value");
		RemoteCache<String, String> cache = remoteCacheManager.getCache(name);

		if(cache != null) {
			long start = Instant.now().toEpochMilli();

			int putBatch = 10000;
			Map<String, String> mapToPut = new HashMap<>(putBatch);
			// Now we actually populate the cache
			for (int i = 1; i < numentries + 1; ++i) {
				String key = "SourceSignal$"+ i;
				Datapoint datapoint = 
					new Datapoint(key, "RTU$" + i, Instant.now().toEpochMilli(), i, i);
				mapToPut.put(key, datapoint.toString());
				if (i % putBatch == 0) {
					cache.putAll(mapToPut);
					mapToPut.clear();
				}
			}
			if (!mapToPut.isEmpty()) {
				cache.putAll(mapToPut);
			}

	        long end = Instant.now().toEpochMilli();
	        LOGGER.debug("Fill Cache Time:" + (end - start));
	        return "Filled cache with "+numentries+" entries in "+(end - start)+" ms";
		}
		return null;
	}

	public String fillProtoCache(int numentries, String name, int threadNum) {
		LOGGER.info("Fill Cache '"+ name+ "' with Protobuf Value");
		RemoteCache<String, Datapoint> cache = remoteCacheManager.getCache(name);

		if(cache != null) {
			long start = Instant.now().toEpochMilli();

			int putBatch = 10000;
			Map<String, Datapoint> mapToPut = new HashMap<>(putBatch);
			// Now we actually populate the cache
			for (int i = 1; i < numentries + 1; ++i) {
				String key = "SourceSignal$"+ i;
				Datapoint datapoint = 
					new Datapoint(key, "RTU$" + i, Instant.now().toEpochMilli(), i, i);
				mapToPut.put(key, datapoint);
				if (i % putBatch == 0) {
					cache.putAll(mapToPut);
					mapToPut.clear();
				}
			}
			if (!mapToPut.isEmpty()) {
				cache.putAll(mapToPut);
			}

	        long end = Instant.now().toEpochMilli();
	        LOGGER.debug("Fill Cache Time:" + (end - start));
	        return "Filled cache with "+numentries+" entries in "+(end - start)+" ms";
		}
		return null;
	}

	// public String fillCache(int numentries, String name, int threadNum) {
	// 	LOGGER.info("Fill Cache '"+ name+ "' with " + numentries +" entries...");
	// 	int putBatch = 10000;
	// 	int nbProcs = Runtime.getRuntime().availableProcessors();
	// 	RemoteCache<String, Datapoint> cache = remoteCacheManager.getCache(name);

	// 	if(cache != null) {
	// 		long start = Instant.now().toEpochMilli();
	// 		// ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
			
	// 		Map<String, Datapoint> mapToPut = new HashMap<>(putBatch);

	// 		// Now we actually populate the cache
	// 		for (int i = 1; i < numentries + 1; ++i) {
	// 			String key = "SourceSignal$"+ i;
	// 			Datapoint datapoint = 
	// 					new Datapoint(key, "RTU$" + i, Instant.now().toEpochMilli(), i, i);
				
	// 			// Now we actually populate the cache
	// 			mapToPut.put(key, "datapoint");
	// 			if (i % putBatch == 0) {
	// 				cache.putAll(mapToPut);
	// 				mapToPut.clear();
	// 			}

	// 			if (!mapToPut.isEmpty()) {
	// 				cache.putAll(mapToPut);
	// 			}

	// 			// executorService.submit(() -> {
	// 			// 	LOGGER.debug("Writing by Thread ID: " + Thread.currentThread().getName());
	// 			// 	long subStart = Instant.now().toEpochMilli();
	// 			// 	cache.put(key, datapoint);
	// 			// 	long subEnd = Instant.now().toEpochMilli();
	// 			// 	LOGGER.debug("Writing by Thread ID: " + Thread.currentThread().getName() + " in "+(subEnd - subStart)+" ms");
	// 			// });
	// 		}

	// 		// executorService.shutdown();
			
	// 		// try {
	// 		// 	if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
	// 		// 		executorService.shutdownNow();
	// 		// 	}
				
	// 		// } catch (InterruptedException ex) {
	// 		// 	executorService.shutdownNow();
	// 		// 	Thread.currentThread().interrupt();
	// 		// }

	//         long end = Instant.now().toEpochMilli();
	//         return "Filled cache with "+numentries+" entries in "+(end - start)+" ms with " + nbProcs + " cores";
	// 	}
	// 	return null;
	// }

	public String dumpCache(String name, int threadNum) {
		LOGGER.info("Dump Cache '"+ name+ "'");

		int nbProcs = Runtime.getRuntime().availableProcessors();
		RemoteCache<String, Object> cache = remoteCacheManager.getCache(name);
		atomicCount.set(0);
		if(cache != null) {
			
	        int batchSize = 8192;
	        CacheTopologyInfo cacheTopologyInfo = cache.getCacheTopologyInfo();
			Map<SocketAddress, Set<Integer>> segmentsByAddress = cacheTopologyInfo.getSegmentsPerServer();

			ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
			
			long start = Instant.now().toEpochMilli();

			for (Set<Integer> segments: segmentsByAddress.values()) {
				// for (Integer oneSegment: segments) {
				// 	Set<Integer> oneSegmentSet = new HashSet<Integer>();
				// 	oneSegmentSet.add(oneSegment);

					executorService.submit(() -> {
						LOGGER.debug("Dumping by Thread ID: " + Thread.currentThread().getName());
						long subStart = Instant.now().toEpochMilli();
						try (CloseableIterator<Map.Entry<Object, Object>> iterator = cache.retrieveEntries(null,segments, batchSize)) {
							
							while (iterator.hasNext()) {
								iterator.next();
								atomicCount.incrementAndGet();
							}
						}
						long subEnd = Instant.now().toEpochMilli();
						LOGGER.debug("Dumped by Thread ID: " + Thread.currentThread().getName() + " in "+(subEnd - subStart)+" ms");
					});
				// }
			}

			executorService.shutdown();
			
			try {
				if (!executorService.awaitTermination(120, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
				
			} catch (InterruptedException ex) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
			long end = Instant.now().toEpochMilli();

	        return "Dumped " + atomicCount.get() + " entrie(s) in "+(end - start)+" ms with " + nbProcs + " core(s)";
		}
		return null;
		
	}

	public String clearCache(String name) {
		RemoteCache<String, Datapoint> cache = remoteCacheManager.getCache(name);
		try {
			cache.clear();
			return "Cache cleared";
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return "Error clearing cache";
		}
		
	}
	
	public void startCacheManager() {
		remoteCacheManager.start();
	}
	
	public void stopCacheManager() {
		remoteCacheManager.stop();
	}
}