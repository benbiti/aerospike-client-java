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

import com.aerospike.client.Bin;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.util.RandomShift;

import java.util.HashMap;
import java.util.Map;

public class Arguments {
	public String namespace;
	public String setName;
	public Workload workload;
	public DBObjectSpec[] objectSpec;
	public Policy readPolicy;
	public WritePolicy writePolicy;
	public WritePolicy updatePolicy;
	public WritePolicy replacePolicy;
	public BatchPolicy batchPolicy;
	public int batchSize;
	public int nBins;
	public int readPct;
	public int readMultiBinPct;
	public int writeMultiBinPct;
	public int throughput;
	public long transactionLimit;
	public boolean reportNotFound;
	public boolean debug;
	public TransactionalWorkload transactionalWorkload;
	public KeyType keyType;
	public Bin[] fixedBins;
	public Bin[] fixedBin;
	//Ai or appId
	//TODO: Use Random ai or appid pool?
	public static char[] aiChars =
			new String("0123456789abcdefghijklmnopqrstuvwzxy").toCharArray();

	public void setFixedBins() {
		// Fixed values are used when the extra random call overhead is not wanted
		// in the benchmark measurement.
		RandomShift random = new RandomShift();
		fixedBins = getBins(random, true, -1, "Test");
		fixedBin = new Bin[] {fixedBins[0]};
	}

	public Bin[] getBins(RandomShift random, boolean multiBin, long keySeed, String UserKey) {
		if (fixedBins != null) {
		    return (multiBin)? fixedBins : fixedBin;
		}
		
		int binCount = (multiBin)? nBins : 1;	
		Bin[] bins = new Bin[binCount];
		int specLength = objectSpec.length;
		
		for (int i = 0; i < binCount; i++) {
			String name = Integer.toString(i);
			// Use passed in value for 0th bin. Random for others.
			Value value = genValue(random, objectSpec[i % specLength],
							keySeed, UserKey);
			bins[i] = new Bin(name, value);
		}
		return bins;
	}

	public Map getBinMapItems(RandomShift random, boolean multiBin, long keySeed, String UserKey) {

		HashMap<Value, Value> map = new HashMap<>();
		StringBuilder sb = new StringBuilder( objectSpec[0 % objectSpec.length].size * 2 );
		sb.append(String.valueOf(random.nextInt(255)));
		sb.append(":abcdedfghijklmnop");
		//random 3 up to 35*35*35 = 42875 for mock ai and app id
		for (int j = 0; j < 3; j++) {
			sb.append(aiChars[random.nextInt(35)]);
		}
		String key_value = sb.toString();
		map.put(Value.get(key_value), Value.get("1"));
		map.put(Value.get("PK"), Value.get(UserKey));

		return map;
	}

	private static Value genValue(RandomShift random, DBObjectSpec spec, long keySeed, String UserKey) {
		switch (spec.type) {
		case 'I':
			if (keySeed == -1) {
				return Value.get(random.nextInt());
			} else {
				return Value.get(keySeed);
			}
			
		case 'B':
			byte[] ba = new byte[spec.size];
			random.nextBytes(ba);
			return Value.get(ba);
				
		case 'S':
			StringBuilder sb = new StringBuilder(spec.size);
            for (int i = 0; i < spec.size; i++) {
            	// Append ascii value between ordinal 33 and 127.
                sb.append((char)(random.nextInt(94) + 33));
            }
			return Value.get(sb.toString());
			
		case 'D':
			return Value.get(System.currentTimeMillis());

		case 'M':
			HashMap<String, String> map = new HashMap<>();
			StringBuilder sb2 = new StringBuilder(spec.size);
			for (int i = 0; i < 1024; i++) {
				sb2.setLength(0);
				for (int j = 0; j < spec.size; j++) {
					// Append ascii value in aiChars
					sb2.append((char)(random.nextInt(94) + 33));
				}
				String key_value = sb2.toString();
				map.put(key_value, "0");
			}
			return Value.get(map);

		case 'A':
			HashMap<String, String> mapA = new HashMap<>();
			StringBuilder sb3 = new StringBuilder(spec.size * 2);
			sb3.append(String.valueOf(random.nextInt(255)));
			sb3.append(":abcdedfghijklmnop");
			//random 3 up to 35*35*35 = 42875 for mock ai and app id
			for (int j = 0; j < 3; j++) {
				sb3.append(aiChars[random.nextInt(35)]);
			}
			String key_value = sb3.toString();
			mapA.put(key_value, "1");
			mapA.put("PK", UserKey);

			return Value.get(mapA);
		default:
			return Value.getAsNull();
		}
	}	
}
