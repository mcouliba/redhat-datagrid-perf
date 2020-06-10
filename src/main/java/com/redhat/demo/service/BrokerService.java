package com.redhat.demo.service;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/cache")
@RegisterRestClient
public interface BrokerService {
	
	@GET
    @Path("/dump/segment")
    @Produces("application/json")
	String getBySegment(@QueryParam(value = "name") String name, @QueryParam(value = "segment") Set<Integer> Segment);
}