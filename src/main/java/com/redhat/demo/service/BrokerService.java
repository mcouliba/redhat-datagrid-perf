package com.redhat.demo.service;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface BrokerService {

    @GET
    @Path("/dump/bysegment")
    @Produces("application/json")
    CompletionStage<String> dumpBySegment(@QueryParam(value = "name") String name
    , @QueryParam(value = "segment") Set<Integer> segments);

}