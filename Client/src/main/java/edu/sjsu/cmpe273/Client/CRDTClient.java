package edu.sjsu.cmpe273.Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class CRDTClient {
	private List<DistributedCacheService> distributedServers;

	public CRDTClient() {
		this.distributedServers = new ArrayList<DistributedCacheService>();
	}

	public void addServer(DistributedCacheService cache){
		distributedServers.add(cache);
	}
	
	public boolean put(long key, String value) throws Exception {
		final AtomicInteger writeCount = new AtomicInteger(0);
		final ArrayList<DistributedCacheService> writeServers = new ArrayList<DistributedCacheService>(3);
		for (final DistributedCacheService cache : distributedServers) {
			Future<HttpResponse<JsonNode>> future = Unirest.put(cache.getCacheServerUrl()+ "/cache/{key}/{value}")
					.header("accept", "application/json")
					.routeParam("key", Long.toString(key))
					.routeParam("value", value)
					.asJsonAsync(new Callback<JsonNode>() {

						public void failed(UnirestException e) {
							System.out.println("The request has failed ");
						}

						public void completed(HttpResponse<JsonNode> response) {
							int count = writeCount.incrementAndGet();
							writeServers.add(cache);
							System.out.println("The request is successful ");
						}

						public void cancelled() {
							System.out.println("The request has been cancelled");
						}

					});
		}
		if (writeCount.intValue() > 1) {
			return true;
		} else {
			for (final DistributedCacheService cache : writeServers) {
				Future<HttpResponse<JsonNode>> future = Unirest.get(cache.getCacheServerUrl() + "/cache/{key}")
						.header("accept", "application/json")
						.routeParam("key", Long.toString(key))
						.asJsonAsync(new Callback<JsonNode>() {

							public void failed(UnirestException e) {
								System.out.println("Delete failed..."+cache.getCacheServerUrl());
							}

							public void completed(HttpResponse<JsonNode> response) {
								System.out.println("Delete is successful "+cache.getCacheServerUrl());
							}

							public void cancelled() {
								System.out.println("The request has been cancelled");
							}
					});
			}
			Unirest.shutdown();
			return false;
		}
	}
	
	public String get(long key) throws Exception {
		final Map<DistributedCacheService, String> getResults = new HashMap<DistributedCacheService, String>();
		for (final DistributedCacheService cache : distributedServers) {
			Future<HttpResponse<JsonNode>> future = Unirest.get(cache.getCacheServerUrl() + "/cache/{key}")
					.header("accept", "application/json")
					.routeParam("key", Long.toString(key))
					.asJsonAsync(new Callback<JsonNode>() {

						public void failed(UnirestException e) {
							System.out.println("The request has failed");
						}

						public void completed(HttpResponse<JsonNode> response) {
							getResults.put(cache, response.getBody().getObject().getString("value"));
							System.out.println("The request is successful ");
						}

						public void cancelled() {
							System.out.println("The request has been cancelled");
						}
				});
		}
		final Map<String, Integer> cMap = new HashMap<String, Integer>();
		int maxCount = 0;
		for (String value : getResults.values()) {
			int count = 1;
			if (cMap.containsKey(value)) {
				count = cMap.get(value);
				count++;
			}
			if (maxCount < count)
				maxCount = count;
			cMap.put(value, count);
		}
		String value = this.findKeyForValue(cMap, maxCount);
		// Read Repair
		if (maxCount != this.distributedServers.size()) {
			for (Entry<DistributedCacheService, String> cacheData : getResults.entrySet()) {
				if (!value.equals(cacheData.getValue())) {
					HttpResponse<JsonNode> response = Unirest.put(cacheData.getKey() + "/cache/{key}/{value}")
							.header("accept", "application/json")
							.routeParam("key", Long.toString(key))
							.routeParam("value", value)
							.asJson();
				}
			}
			for (DistributedCacheService cache : this.distributedServers) {
				if (getResults.containsKey(cache)) continue;
				HttpResponse<JsonNode> response = Unirest.put(cache.getCacheServerUrl() + "/cache/{key}/{value}")
						.header("accept", "application/json")
						.routeParam("key", Long.toString(key))
						.routeParam("value", value)
						.asJson();
			}
		} else {
			System.out.println("No Read Repair");
		}
		Unirest.shutdown();
		return value;
	}

	public String findKeyForValue(Map<String, Integer> map, int value) {
		for (Entry<String, Integer> entry : map.entrySet()) {
			if (value == entry.getValue()) 
				return entry.getKey();
		}
		return null;
	}
}