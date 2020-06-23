package com.redhat.demo.message;

import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DumpSegmentMessage {
    private  String cacheName;
    private  Set<Integer> segments;

    public DumpSegmentMessage(String cacheName, Set<Integer> segments) {
    	this.cacheName = cacheName;
      this.segments = segments;
    }

    public String getCacheName() {
		return cacheName;
    }
    
    public Set<Integer> getSegments() {
		return segments;
    }

    public JsonObject toJsonObject (){
      JsonObject jsonToEncode = new JsonObject();
      jsonToEncode.put("cacheName", cacheName);
      JsonArray array = new JsonArray();
      for(Integer segment: segments) {
          array.add(segment);
      }
      jsonToEncode.put("segments", array);

      return jsonToEncode;
    }
}