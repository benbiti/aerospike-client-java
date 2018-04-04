/*
 * Copyright 2012-2018 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.aerospike.benchmarks;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.util.RandomShift;
import com.aerospike.client.util.Util;

import java.util.Map;

/**
 * Synchronous read/write task.
 */
public class RWTaskSync extends RWTask implements Runnable {

	private final AerospikeClient client;

	public RWTaskSync(AerospikeClient client, Arguments args, CounterStore counters, long keyStart, long keyCount) {
		super(args, counters, keyStart, keyCount);
		this.client = client;
	}
	
	public void run() {
		RandomShift random = RandomShift.instance();
		Node[] nodes = client.getNodes();
		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];
			System.out.println("Node Info Begin: " + node.toString());
		}
		while (valid) {
			runCommand(random);
			
			// Throttle throughput
			if (args.throughput > 0) {
				int transactions;
				if (counters.transaction.latency != null) {
					// Measure the transactions as per one "business" transaction
					transactions = counters.transaction.count.get();
				}
				else {
					transactions = counters.write.count.get() + counters.read.count.get();
				}
				if (transactions > args.throughput) {
					long millis = counters.periodBegin.get() + 1000L - System.currentTimeMillis();                                        

					if (millis > 0) {
						Util.sleep(millis);
					}
				}
			}
		}
		nodes = client.getNodes();
		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];
			System.out.println("Node Info End: " + node.toString());
		}
	}

	@Override
	protected void put(WritePolicy writePolicy, Key key, Bin[] bins) {
		if (counters.write.latency != null) {
			long begin = System.nanoTime();
			client.put(writePolicy, key, bins);
			long elapsed = System.nanoTime() - begin;
			counters.write.count.getAndIncrement();			
			counters.write.latency.add(elapsed);
		}
		else {
			client.put(writePolicy, key, bins);
			counters.write.count.getAndIncrement();			
		}
	}

	@Override
	protected void putItems(WritePolicy writePolicy, Key key, Map map) {
		if (counters.write.latency != null) {
			long begin = System.nanoTime();
			client.operate(writePolicy, key,
					MapOperation.putItems(MapPolicy.Default, "0", map));
			long elapsed = System.nanoTime() - begin;
			counters.write.count.getAndIncrement();
			counters.write.latency.add(elapsed);
		}
		else {
			client.operate(writePolicy, key,
					MapOperation.putItems(MapPolicy.Default, "0", map));
			counters.write.count.getAndIncrement();
		}
	}
	
	@Override
	protected void add(Key key, Bin[] bins) {
		if (counters.write.latency != null) {
			long begin = System.nanoTime();
			client.add(writePolicyGeneration, key, bins);
			long elapsed = System.nanoTime() - begin;
			counters.write.count.getAndIncrement();			
			counters.write.latency.add(elapsed);
		}
		else {
			client.add(writePolicyGeneration, key, bins);
			counters.write.count.getAndIncrement();
		}
	}

	@Override
	protected void get(Key key, String binName) {
		Record record;
		
		if (counters.read.latency != null) {
			long begin = System.nanoTime();
			record = client.get(args.readPolicy, key, binName);
			long elapsed = System.nanoTime() - begin;
			counters.read.latency.add(elapsed);
		}
		else {
			record = client.get(args.readPolicy, key, binName);
		}		
		processRead(key, record);
	}
	
	@Override
	protected void get(Key key) {
		Record record;
		
		if (counters.read.latency != null) {
			long begin = System.nanoTime();
			record = client.get(args.readPolicy, key);
			long elapsed = System.nanoTime() - begin;
			counters.read.latency.add(elapsed);
		}
		else {
			record = client.get(args.readPolicy, key);
		}	
		processRead(key, record);
	}
	
	@Override
	protected void get(Key[] keys, String binName) {
		Record[] records;
		
		if (counters.read.latency != null) {
			long begin = System.nanoTime();
			records = client.get(args.batchPolicy, keys, binName);
			long elapsed = System.nanoTime() - begin;
			counters.read.latency.add(elapsed);
		}
		else {
			records = client.get(args.batchPolicy, keys, binName);
		}
	
		for (int i = 0; i < keys.length; i++) {
			processRead(keys[i], records[i]);
		}
	}

	@Override
	protected void get(Key[] keys) {
		Record[] records;
		
		if (counters.read.latency != null) {
			long begin = System.nanoTime();
			records = client.get(args.batchPolicy, keys);
			long elapsed = System.nanoTime() - begin;
			counters.read.latency.add(elapsed);
		}
		else {
			records = client.get(args.batchPolicy, keys);
		}
	
		for (int i = 0; i < keys.length; i++) {
			processRead(keys[i], records[i]);
		}
	}	
}
