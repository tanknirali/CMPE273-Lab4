package edu.sjsu.cmpe.cache.client;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.*;
import java.lang.InterruptedException;
import java.io.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Options;


public class NewClient implements CallbackInterface {

    private ConcurrentHashMap<String, CacheServiceInterface> serversList;
    private ArrayList<String> serverSuccess;
    private ConcurrentHashMap<String, ArrayList<String>> resultsDict;

    private static CountDownLatch cntDownLatch;

    public NewClient() {

        serversList = new ConcurrentHashMap<String, CacheServiceInterface>(3);
        CacheServiceInterface cache0 = new DistributedCacheService("http://localhost:3000", this);
        CacheServiceInterface cache1 = new DistributedCacheService("http://localhost:3001", this);
        CacheServiceInterface cache2 = new DistributedCacheService("http://localhost:3002", this);
        serversList.put("http://localhost:3000", cache0);
        serversList.put("http://localhost:3001", cache1);
        serversList.put("http://localhost:3002", cache2);
    }

    // Callbacks
    @Override
    public void putFailed(Exception e) {
        System.out.println("Request failed...");
        cntDownLatch.countDown();
    }

    @Override
    public void putSuccess(HttpResponse<JsonNode> response, String serverUrl) {
        int getCode = response.getCode();
        System.out.println("Put success --- " + getCode + " on server ---" + serverUrl);
        serverSuccess.add(serverUrl);
        cntDownLatch.countDown();
    }

    @Override
    public void getFailed(Exception e) {
        System.out.println("Get Request failed...");
        cntDownLatch.countDown();
    }

    @Override
    public void getSuccess(HttpResponse<JsonNode> response, String serverUrl) {

        String value = null;
        if (response != null && response.getCode() == 200) {
            value = response.getBody().getObject().getString("value");
                System.out.println("The Value from server --- " + serverUrl + "is --- " + value);
            ArrayList ServerWithValue = resultsDict.get(value);
            if (ServerWithValue == null) {
                ServerWithValue = new ArrayList(3);
            }
            ServerWithValue.add(serverUrl);

            // Save Arraylist of servers into dictResults
            resultsDict.put(value, ServerWithValue);
        }

        cntDownLatch.countDown();
    }



    public boolean put(long key, String value) throws InterruptedException {
        serverSuccess = new ArrayList(serversList.size());
        cntDownLatch = new CountDownLatch(serversList.size());

        for (CacheServiceInterface cache : serversList.values()) {
            cache.put(key, value);
        }

        cntDownLatch.await();

        boolean isSuccess = Math.round((float)serverSuccess.size() / serversList.size()) == 1;

        if (! isSuccess) {
            // Send delete for the same key
            delete(key, value);
        }
        return isSuccess;
    }

    public void delete(long key, String value) {

        for (final String serverUrl : serverSuccess) {
            CacheServiceInterface server = serversList.get(serverUrl);
            server.delete(key);
        }
    }
    public String get(long key) throws InterruptedException {
        resultsDict = new ConcurrentHashMap<String, ArrayList<String>>();
        cntDownLatch = new CountDownLatch(serversList.size());

        for (final CacheServiceInterface server : serversList.values()) {
            server.get(key);
        }
        cntDownLatch.await();

        // Take the first element
        String rightValue = resultsDict.keys().nextElement();

        // Discrepancy in results (either more than one value gotten, or null gotten somewhere)
        if (resultsDict.keySet().size() > 1 || resultsDict.get(rightValue).size() != serversList.size()) {

            ArrayList<String> maxValues = maxKeyForTable(resultsDict);

            if (maxValues.size() == 1) {

                rightValue = maxValues.get(0);

                ArrayList<String> repairServer = new ArrayList(serversList.keySet());
                repairServer.removeAll(resultsDict.get(rightValue));
                for (String serverUrl : repairServer) {

                    System.out.println("fixing : " + serverUrl + " value: " + rightValue);
                    CacheServiceInterface server = serversList.get(serverUrl);
                    server.put(key, rightValue);

                }

            } else {

            }
        }

        return rightValue;

    }


    // Returns array of keys with the maximum value
    // If array contains only 1 value, then it is the highest value in the hash map
    public ArrayList<String> maxKeyForTable(ConcurrentHashMap<String, ArrayList<String>> table) {
        ArrayList<String> maxKeys= new ArrayList<String>();
        int maxValue = -1;
        for(Map.Entry<String, ArrayList<String>> entry : table.entrySet()) {
            if(entry.getValue().size() > maxValue) {
                maxKeys.clear(); 
                maxKeys.add(entry.getKey());
                maxValue = entry.getValue().size();
            }
            else if(entry.getValue().size() == maxValue)
            {
                maxKeys.add(entry.getKey());
            }
        }
        return maxKeys;
    }
}