package com.redhat.demo.initializer;

import com.redhat.demo.model.Datapoint;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

// @AutoProtoSchemaBuilder(includeClasses = { Datapoint.class }, schemaPackageName = "datapoint")
interface DatapointContextContextInitializer extends SerializationContextInitializer {
    
}
