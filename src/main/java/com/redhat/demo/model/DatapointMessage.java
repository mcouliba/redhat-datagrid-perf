package com.redhat.demo.model;

public class DatapointMessage {
    private  String filename;
    private  Datapoint datapoint;

    public DatapointMessage(String filename, Datapoint datapoint) {
    	this.filename = filename;
        this.datapoint = datapoint;
     }

    public String getFilename() {
		return filename;
    }
    
    public void setFilename(String filename) {
		 this.filename = filename;
    }
    
    public Datapoint getDatapoint() {
		return datapoint;
    }
    
    public void setDatapoint(Datapoint datapoint) {
		 this.datapoint = datapoint;
	}
}