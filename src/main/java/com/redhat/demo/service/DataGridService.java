package com.redhat.demo.service;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
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

import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.util.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DataGridService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("CacheService");
	
	private static AtomicInteger count = new AtomicInteger();

	private RemoteCacheManager remoteCacheManager; 
	
	private static final String CACHE_XML_CONFIG =
         "<infinispan><cache-container>" +
			"  <distributed-cache-configuration name=\"spectrum\" statistics=\"false\" statistics-available=\"false\" segments=\"512\" owners=\"1\">" +
			"  </distributed-cache-configuration>" +
		"</cache-container></infinispan>";
			   
	// @Inject
	// public DataGridService(RemoteCacheManager remoteCacheManager){
	// 	this.remoteCacheManager = remoteCacheManager;
	// }
	
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
		
	}
	
	
	void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Starting Quarkus app... " + remoteCacheManager.getConfiguration().toString());
    }
	
	public boolean createCache(String name) {
		try {
			remoteCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
				.getOrCreateCache(name, new XMLStringConfiguration(CACHE_XML_CONFIG));
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
	
	public String fillCache(int numentries, String name) {
		RemoteCache<String, String> cache = retrieveRemoteCache(name);

		if(cache != null) {
			long start = Instant.now().toEpochMilli();

			int putBatch = 10000;
			Map<String, String> mapToPut = new HashMap<>(putBatch);
			// Now we actually populate the cache
			for (int i = 1; i < numentries + 1; ++i) {
				mapToPut.put("ID" + i, "SourceSignal$"+ i +";RTU0000X;98798798;192;20.98237\n");
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
	
	public String dumpSingleThread(String name) {
		RemoteCache<String, String> cache = retrieveRemoteCache(name);
		if(cache != null) {
			long start = Instant.now().toEpochMilli();
	        int batchSize = 512;
			StringBuffer sb = new StringBuffer(100 * 1024 * 1024);
			
	        try (CloseableIterator<Entry<Object, Object>> iterator = cache.retrieveEntries(null, null, batchSize)) {
				iterator.forEachRemaining(e -> {
					sb.append(e.getValue().toString());
				});
	        } catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
	        
			long end = Instant.now().toEpochMilli();
			
	        return "Dumped entries in "+(end - start)+" ms\n" + sb.toString();
		}
		return null;
	}

	public String dumpMultiThread(String name) {
		RemoteCache<String, String> cache = retrieveRemoteCache(name);

		if(cache != null) {
			
	        int batchSize = 512;
	        CacheTopologyInfo cacheTopologyInfo = cache.getCacheTopologyInfo();
			Map<SocketAddress, Set<Integer>> segmentsByAddress = cacheTopologyInfo.getSegmentsPerServer();

			ExecutorService executorService = Executors.newFixedThreadPool(256);
			
			for (Set<Integer> segments: segmentsByAddress.values()) {
				for (Integer segment: segments){
					Set<Integer> segmentToRetrieve = new HashSet<Integer>();
					segmentToRetrieve.add(segment);

					executorService.submit(() -> {
						int count = 0;
						long start = Instant.now().toEpochMilli();
						try (CloseableIterator<Map.Entry<Object, Object>> iterator = cache.retrieveEntries(null,segmentToRetrieve, batchSize)) {
							
							while (iterator.hasNext()) {
								iterator.next();
								count++;
							}
						}
						long end = Instant.now().toEpochMilli();
						LOGGER.info("Dump of " + count + " entries by Thread ID: " + Thread.currentThread().getName() + " in " + (end - start) + " ms");
					});
				}
				
			}
			
	        return "Dumped done";
		}
		return null;
		
	}
	
	public String dumpBySegment(String name, Set<Integer>  segments) {
		RemoteCache<String, String> cache = retrieveRemoteCache(name);
		if(cache != null) {
			long start = Instant.now().toEpochMilli();
	        int batchSize = 4096;
			StringBuffer sb = new StringBuffer(100 * 1024 * 1024);
			
	        try (CloseableIterator<Entry<Object, Object>> iterator = cache.retrieveEntries(null, segments, batchSize)) {
				iterator.forEachRemaining(e -> {
					sb.append(e.getValue().toString()).append("\n");
				});
	        } catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
	        
			long end = Instant.now().toEpochMilli();

			LOGGER.info("Dumped "+count+" entries in "+(end - start)+" ms");
	        return sb.toString();
		}
		return null;
	}

	public String clearCache(String name) {
		RemoteCache<String, String> cache = retrieveRemoteCache(name);
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
	
	private RemoteCache<String, String> retrieveRemoteCache(String name){
		return remoteCacheManager.getCache(name);
	}
	
	public Map<SocketAddress, Set<Integer>> getCacheSegments(String name) {

		RemoteCache<String, String> cache = retrieveRemoteCache(name);
		CacheTopologyInfo cacheTopologyInfo = cache.getCacheTopologyInfo();
		Map<SocketAddress, Set<Integer>> segmentsByAddress = cacheTopologyInfo.getSegmentsPerServer();
		return segmentsByAddress;
	}
}