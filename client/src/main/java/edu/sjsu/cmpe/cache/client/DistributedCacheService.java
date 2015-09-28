package edu.sjsu.cmpe.cache.client;

import java.util.concurrent.Future;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Distributed cache service
 * 
 */
public class DistributedCacheService implements CacheServiceInterface {
	 private final String serverUrl;
	    private CallbackInterface callBck;
	    public DistributedCacheService(String serverUrl) {
	        this.serverUrl = serverUrl;
	    }
	    public DistributedCacheService(String serverUrl, CallbackInterface callbk) {
	        this.serverUrl = serverUrl;
	        this.callBck = callbk;
	    }
	    @Override
	    public String get(long key) {
	        Future<HttpResponse<JsonNode>> future = Unirest.get(this.serverUrl + "/cache/{key}")
	                .header("accept", "application/json")
	                .routeParam("key", Long.toString(key))
	                .asJsonAsync(new Callback<JsonNode>() {
	                    public void failed(UnirestException e) {
	                        callBck.getFailed(e);
	                    }
	                    public void completed(HttpResponse<JsonNode> response) {
	                        callBck.getSuccess(response, serverUrl);
	                    }
	                    public void cancelled() {
	                        System.out.println("Request Cancelled...");
	                    }

	                });

	        return null;
	    }

	  
	    @Override
	    public void put(long key, String value) {
	        Future<HttpResponse<JsonNode>> future = Unirest.put(this.serverUrl + "/cache/{key}/{value}")
	                .header("accept", "application/json")
	                .routeParam("key", Long.toString(key))
	                .routeParam("value", value)
	                .asJsonAsync(new Callback<JsonNode>() {

	                    public void failed(UnirestException e) {
	                        callBck.putFailed(e);
	                    }

	                    public void completed(HttpResponse<JsonNode> response) {
	                        callBck.putSuccess(response, serverUrl);
	                    }

	                    public void cancelled() {
	                        System.out.println("Request Cancelled...");
	                    }

	                });
	    }

	    @Override
	    public void delete(long key) {
	        HttpResponse<JsonNode> response = null;
	        try {
	            response = Unirest
	                    .delete(this.serverUrl + "/cache/{key}")
	                    .header("accept", "application/json")
	                    .routeParam("key", Long.toString(key))
	                    .asJson();
	        } catch (UnirestException e) {
	            System.err.println(e);
	        }

	        System.out.println("response is --- " + response);

	        if (response == null || response.getCode() != 204) {
	            System.out.println("Delete failed...");
	        } else {
	            System.out.println("Deleted " + key + " from " + this.serverUrl);
	        }

	    }
}
