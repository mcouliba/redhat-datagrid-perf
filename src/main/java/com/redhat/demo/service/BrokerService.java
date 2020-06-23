package com.redhat.demo.service;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

public class BrokerService {
    
    @Inject
    @RestClient
    ClientService clientService;

    @ConsumeEvent(value = "dump-segments")
    Uni<String> consume(JsonObject jsonObject) {
        String cacheName = jsonObject.getString("cacheName");
        Set<Integer> segments = new HashSet<Integer>();
        for(Object segment: jsonObject.getJsonArray("segments")) {
            segments.add((Integer) segment);
        }
        return clientService.getBySegment(cacheName, segments);
    }
}