package com.redhat.demo.model;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class Datapoint {

    private final String signalSource;
    private final String rtuId;
    private final long linuxTimestamp;
    private final int qualityCode;
    private final double value;

    @ProtoFactory
    public Datapoint(String signalSource, String rtuId, long linuxTimestamp, int qualityCode, double value) {
    	this.signalSource = signalSource;
        this.rtuId = rtuId;
        this.linuxTimestamp = linuxTimestamp;
        this.qualityCode = qualityCode;
        this.value = value;
     }

    @ProtoField(number = 1, defaultValue = "0")
	public String getSignalSource() {
		return signalSource;
	}

    @ProtoField(number = 2, defaultValue = "0")
	public String getRtuId() {
		return rtuId;
	}

    @ProtoField(number = 3, defaultValue = "0")
	public long getLinuxTimestamp() {
		return linuxTimestamp;
	}

    @ProtoField(number = 4, defaultValue = "0")
	public int getQualityCode() {
		return qualityCode;
	}

    @ProtoField(number = 5, defaultValue = "0")
	public double getValue() {
		return value;
	}
    
    @Override
   public String toString() {
      return String.format("%s;%s;%d;%d;%.2f\n", signalSource, rtuId, linuxTimestamp, qualityCode, value);
   }
}