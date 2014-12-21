package edu.sjsu.cmpe273.Client;

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Cache Client...");
        CRDTClient crdt = new CRDTClient();
        DistributedCacheService cache1 = new DistributedCacheService(
				"http://localhost:3000");
		DistributedCacheService cache2 = new DistributedCacheService(
				"http://localhost:3001");
		DistributedCacheService cache3 = new DistributedCacheService(
				"http://localhost:3002");
        crdt.addServer(cache1);
        crdt.addServer(cache2);
        crdt.addServer(cache3);
        System.out.println("Step 1:");
        boolean putStatus = crdt.put(1, "a");
        if (putStatus) {
			System.out.println("put(1 => a) success.");
			System.out.println("Sleeping!!");
        	Thread.sleep(30000);
			System.out.println("Step 2:");
        	putStatus = crdt.put(1, "b");
        	if (putStatus) {
				System.out.println("put(1 => b) success");
				System.out.println("Sleeping!!");
            	Thread.sleep(30000);
				System.out.println("Step 3:");
            	String value = crdt.get(1);
				System.out.println("get(1) => " + value);
        	} else {
				System.out.println("put(1 => b) fail");
        	}
        } else {
			System.out.println("put(1 => a) fail.");
        }	
        System.out.println("Exiting Cache Client...");
        
    }

}
