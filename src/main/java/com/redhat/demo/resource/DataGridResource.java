package com.redhat.demo.resource;

import java.net.SocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.demo.service.BrokerService;
import com.redhat.demo.service.DataGridService;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/cache")
public class DataGridResource {
    
    @Inject
    @RestClient
    BrokerService brokerService;

    @Inject
	DataGridService dataGridService;
    
    @GET
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCache(@QueryParam(value = "name") String name) {
    	return Response.ok(dataGridService.createCache(name)).build();
    }
    
    @GET
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeCache(@QueryParam(value = "name") String name) {
    	return Response.ok(dataGridService.removeCache(name)).build();
    }
    
    @GET
    @Path("/fill")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fillCache(@QueryParam(value = "entries") int entries, @QueryParam(value = "name") String name) {
        return Response.ok(dataGridService.fillCache(entries, name)).build();
    }
    
    @GET
    @Path("/dump/singlethread")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dumpSingleThread(@QueryParam(value = "name") String name) {
        return Response.ok(dataGridService.dumpSingleThread(name)).build();
    }
    
    @GET
    @Path("/dump/multithread")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dumpMultiThread(@QueryParam(value = "name") String name,@QueryParam(value = "threadNum") int threadNum) {
        return Response.ok(dataGridService.dumpMultiThread(name, threadNum)).build();
    }

    @GET
    @Path("/dump/bysegment")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dumpBySegment(@QueryParam(value = "name") String name, @QueryParam(value = "segment")Set<Integer> segments) {
        return Response.ok(dataGridService.dumpBySegment(name, segments)).build();
    }

    @GET
    @Path("/dump/multiprocess")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dumpMultiprocess(@QueryParam(value = "name") String name) {

        long start = Instant.now().toEpochMilli();
        Map<SocketAddress, Set<Integer>> segmentsByAddress = dataGridService.getCacheSegments(name);

        List<CompletionStage<String>> results = new ArrayList<CompletionStage<String>>();

        for (Set<Integer> segments: segmentsByAddress.values()) {
            CompletionStage<String> result = brokerService.dumpBySegment(name, segments);
            result.thenAccept(res -> {
                System.out.println(">>>>>>> " + res);
            });
        }
       
//     .thenApply(avoid -> allFutures  //start to collect them
//             .stream()
//             .flatMap(f -> f.join().stream()) //get List from feature. Here these cars has been obtained, therefore non blocking
//             .collect(Collectors.toList())
// );

        long end = Instant.now().toEpochMilli();


        return Response.ok("Dumped MultiProcess in "+(end - start)+" ms").build();
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearCache(@QueryParam(value = "name") String name) {
        return Response.ok(dataGridService.clearCache(name)).build();
    }
    
    @GET
    @Path("/segments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSegments(@QueryParam(value = "name") String name) {
        return Response.ok(dataGridService.getCacheSegments(name)).build();
    }
}