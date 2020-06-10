package com.redhat.demo.resource;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.redhat.demo.service.BrokerService;
import com.redhat.demo.service.DataGridService;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Multi;

@Path("/broker")
public class BrokerResource {

    @Inject
    @RestClient
    BrokerService brokerService;

    @Inject
	DataGridService dataGridService;
    
    @GET
    @Path("/dump")
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> dumpCache(@QueryParam(value = "name") String name) throws InterruptedException {
        Map<SocketAddress, Set<Integer>> segmentsByAddress = dataGridService.getSegments(name);

        return Multi.createFrom().iterable(segmentsByAddress.values())
                        .onItem().apply(segments -> brokerService.getBySegment(name, segments));
    }   
}