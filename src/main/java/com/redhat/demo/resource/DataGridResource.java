package com.redhat.demo.resource;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.demo.service.DataGridService;

import io.smallrye.mutiny.Uni;

@Path("/cache")
public class DataGridResource {

    @Inject
	DataGridService dataGridService;
    
    @GET
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCache(@QueryParam(value = "name") String name, @QueryParam(value = "segments") int segments, @QueryParam(value = "owners") int owners) {
    	return Response.ok(dataGridService.createCache(name, segments, owners)).build();
    }
    
    @GET
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeCache(@QueryParam(value = "name") String name) {
    	return Response.ok(dataGridService.removeCache(name)).build();
    }
    
    @GET
    @Path("/fill/string")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fillStringCache(@QueryParam(value = "entries") int entries, @QueryParam(value = "name") String name, @QueryParam(value = "threadNum") int threadNum) {
        return Response.ok(dataGridService.fillStringCache(entries, name, threadNum)).build();
    }

    @GET
    @Path("/fill/proto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fillProtoCache(@QueryParam(value = "entries") int entries, @QueryParam(value = "name") String name) {
        return Response.ok(dataGridService.fillProtoCache(entries, name)).build();
    }
    
    @GET
    @Path("/dump")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dumpCache(@QueryParam(value = "name") String name, @QueryParam(value = "threadNum") int threadNum) throws InterruptedException {
        return Response.ok(dataGridService.dumpCache(name, threadNum)).build();
    }

    @GET
    @Path("/dump/segment")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> dumpSegment(@QueryParam(value = "name") String name, @QueryParam(value = "segment") Set<Integer> segment) throws InterruptedException {
        return Uni.createFrom().item(Response.ok(dataGridService.dumpSegment(name, segment)).build());
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearCache(@QueryParam(value = "name") String name) {
        return Response.ok(dataGridService.clearCache(name)).build();
    }
}