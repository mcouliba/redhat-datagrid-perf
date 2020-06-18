package com.redhat.demo.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.redhat.demo.service.BrokerService;
import com.redhat.demo.service.DataGridService;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.commons.util.IntSet;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/broker")
public class BrokerResource {
    @Inject 
    ManagedExecutor managedExecutor;
    
    @Inject
    @RestClient
    BrokerService brokerService;

    @Inject
	DataGridService dataGridService;
    
    @GET
    @Path("/dump")
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> dumpCache(@QueryParam(value = "name") String name, @QueryParam(value = "size") int size) throws InterruptedException {
        List<Set<Integer>> segmentSet = dataGridService.getSegments(name, size);

        return Multi.createFrom().iterable(segmentSet)
                        .onItem().produceUni(segments -> brokerService.getBySegment(name, segments))
                        .merge();
    }   
}