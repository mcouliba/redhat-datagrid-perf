package com.redhat.demo.service;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.demo.model.Datapoint;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.IntSets;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DataGridService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("DataGrid");
	
	private static final int BATCH_SIZE = 8192;

	private static AtomicInteger atomicCount = new AtomicInteger();

	private RemoteCacheManager remoteCacheManager; 
	
	@ConfigProperty(name = "application.podname")
	private String podname;

	// private static Map<String, String> concurrentHashMap = new ConcurrentHashMap<String, String>();
	private static Map<String, String> concurrentHashMap = Collections.synchronizedMap(new HashMap<String, String>());

	@Inject 
	ManagedExecutor managedExecutor;

	private static final String CACHE_XML_CONFIG =
         "<infinispan><cache-container>" +
			"  <distributed-cache-configuration name=\"%s\" segments=\"%d\" owners=\"%d\">" +
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
	
	public boolean createCache(String name, int segments, int owners) {
		try {
			remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
				.getOrCreateCache(name, new XMLStringConfiguration(String.format(CACHE_XML_CONFIG, name, segments, owners)));
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

			Map<String, String> mapToPut = new HashMap<>(BATCH_SIZE);
			// Now we actually populate the cache
			for (int i = 1; i < numentries + 1; ++i) {
				String key = "SourceSignal$"+ i;
				Datapoint datapoint = 
					new Datapoint(key, "RTU$" + i, Instant.now().toEpochMilli(), i, i);
				mapToPut.put(key, datapoint.toString());
				if (i % BATCH_SIZE == 0) {
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

	public String fillProtoCache(int numentries, String name) {
		LOGGER.info("Fill Cache '"+ name+ "' with Protobuf Value");
		RemoteCache<String, Datapoint> cache = remoteCacheManager.getCache(name);

		if(cache != null) {
			long start = Instant.now().toEpochMilli();

			Map<String, Datapoint> mapToPut = new HashMap<>(BATCH_SIZE);
			// Now we actually populate the cache
			for (int i = 1; i < numentries + 1; ++i) {
				String key = "SourceSignal$"+ i;
				Datapoint datapoint = 
					new Datapoint(key, "RTU$" + i, Instant.now().toEpochMilli(), i, i);
				mapToPut.put(key, datapoint);
				if (i % BATCH_SIZE == 0) {
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
	
	public String dumpCache(String name, int threadNum) throws InterruptedException {
		LOGGER.info("Dump Cache '"+ name+ "'");

		int nbProcs = Runtime.getRuntime().availableProcessors();
		RemoteCache<String, Object> cache = remoteCacheManager.getCache(name);
		atomicCount.set(0);
		concurrentHashMap.clear();

		if(cache != null) {
			
	        CacheTopologyInfo cacheTopologyInfo = cache.getCacheTopologyInfo();
			Map<SocketAddress, Set<Integer>> segmentsByAddress = cacheTopologyInfo.getSegmentsPerServer();

			ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
			
			long readStart = Instant.now().toEpochMilli();

			for (Set<Integer> segments: segmentsByAddress.values()) {
				for (Integer oneSegment: segments) {
					Set<Integer> oneSegmentSet = new HashSet<Integer>();
					oneSegmentSet.add(oneSegment);

					executorService.execute(() -> {
						LOGGER.debug("Dumping segments");
						long subStart = Instant.now().toEpochMilli();
						// try (CloseableIterator<Map.Entry<Object, Object>> iterator = cache.retrieveEntries(null,segments, BATCH_SIZE)) {
						try (CloseableIterator<Map.Entry<Object, Object>> iterator = cache.retrieveEntries(null,oneSegmentSet, BATCH_SIZE)) {	
							while (iterator.hasNext()) {
								Entry<Object, Object> entry = iterator.next();
								Datapoint datapoint = (Datapoint) entry.getValue();

								atomicCount.incrementAndGet();
								String strDatapoint = datapoint.toString();
								concurrentHashMap.put(datapoint.getSignalSource(), strDatapoint);
							}
						}
						long subEnd = Instant.now().toEpochMilli();
						LOGGER.debug("Dumped segments in "+(subEnd - subStart)+" ms");
					});
				}
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

			long readEnd = Instant.now().toEpochMilli();

			return "Dumped " + concurrentHashMap.size() + " entrie(s) in "+(readEnd - readStart)+" ms with " + nbProcs + " core(s) ";
		}
		return null;
	}

	public String dumpSegment(String name, Set<Integer> segments) throws InterruptedException {
		LOGGER.debug("Dump Segment '"+ name+ "'");
		long readStart = Instant.now().toEpochMilli();

		RemoteCache<String, Object> cache = remoteCacheManager.getCache(name);
		// CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(managedExecutor);
	   
		if(cache != null) {
			// for (Integer oneSegment: segments) {
			// 	Set<Integer> oneSegmentSet = new HashSet<Integer>();
			// 	oneSegmentSet.add(oneSegment);
				// completionService.submit(() -> {
					LOGGER.debug("Dumping segments");
					int count = 0;
					long subStart = Instant.now().toEpochMilli();
					try (CloseableIterator<Map.Entry<Object, Object>> iterator = cache.retrieveEntries(null,segments, BATCH_SIZE)) {	
						while (iterator.hasNext()) {
							Entry<Object, Object> entry = iterator.next();
							Datapoint datapoint = (Datapoint) entry.getValue();
							// String strDatapoint = datapoint.toString();

							count++;
						}
					}
					long subEnd = Instant.now().toEpochMilli();
					LOGGER.debug("Dumped segments with " + count + " entries in "+(subEnd - subStart)+" ms");
					// return count;
				// });
			// }
			
			// return Uni.createFrom().item(() -> {
			// 			Integer count = null;
			// 			try {
			// 				LOGGER.info("LA");
			// 				count = completionService.take().get();
			// 				LOGGER.info("HERE");
			// 			}
			// 			catch(Exception e) {
			// 				LOGGER.error("Error when getting segments dumped");
			// 			}
			// 			return count;
			// 		}).map(count -> {
			// 				long readEnd = Instant.now().toEpochMilli();
			// 				LOGGER.info("again");
			// 				return "Dumped " + count + " entrie(s) in "+(readEnd - readStart)+" ms by " + podname;
			// 			});
			int received = 0;
			// int globalCount = 0;
			int globalCount = count;
			boolean errors = false;

			// while(received < segments.size() && !errors) {
			// 	Future<Integer> resultFuture = completionService.take(); //blocks if none available

			// 	try {
			// 		globalCount += resultFuture.get();
			// 		received ++;
			// 	}
			// 	catch(Exception e) {
			// 		LOGGER.error("Error when getting segments dumped");
			// 		errors = true;
			// 	}
			// }

			long readEnd = Instant.now().toEpochMilli();
			return "Dumped " + globalCount + " entrie(s) in "+(readEnd - readStart)+" ms by " + podname;
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


	public List<Set<Integer>> getSegments(String name, int size) throws InterruptedException {
		RemoteCache<String, Object> cache = remoteCacheManager.getCache(name);
		List<Set<Integer>> result = null;

		if(cache != null) {
	        CacheTopologyInfo cacheTopologyInfo = cache.getCacheTopologyInfo();
			if (size == 1) {
   				result = Collections.singletonList(IntSets.immutableRangeSet(cacheTopologyInfo.getNumSegments()));
			} else {
				Map<SocketAddress, Set<Integer>> segmentsByAddress = cacheTopologyInfo.getSegmentsPerServer();

				Set<Integer> uniqueSegments = new HashSet<Integer>();

				for (Set<Integer> segments: segmentsByAddress.values()) {
					segments.removeAll(uniqueSegments);
					uniqueSegments.addAll(segments);
				};

				result = new ArrayList<Set<Integer>>(size);

				for (int i = 0; i < size; i++){
					result.add(new HashSet<Integer>());
				}

				int index = 0;
				for (Integer segment : uniqueSegments){
					result.get(index % size).add(segment);
					index++;
				};
			}
		}

		return result;
	}
}