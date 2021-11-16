/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.jraft.rhea.main;

import com.alipay.sofa.jraft.rhea.client.DefaultRheaKVStore;
import com.alipay.sofa.jraft.rhea.client.RheaKVStore;
import com.alipay.sofa.jraft.rhea.client.pd.PlacementDriverClient;
import com.alipay.sofa.jraft.rhea.errors.NotLeaderException;
import com.alipay.sofa.jraft.rhea.options.RegionEngineOptions;
import com.alipay.sofa.jraft.rhea.options.RheaKVStoreOptions;
import com.alipay.sofa.jraft.util.BytesUtil;
import com.alipay.sofa.jraft.util.Endpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class KVClusterNode {
    private static final Logger               LOG    = LoggerFactory.getLogger(KVClusterNode.class);

    private volatile String                   tempDbPath;
    private volatile String                   tempRaftPath;
    private CopyOnWriteArrayList<RheaKVStore> stores = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            LOG.error("Usage: com.alipay.sofa.jraft.rhea.main.KVClusterNode <ConfigFilePath>");
            System.exit(1);
        }
        String confPath = args[0];
        KVClusterNode kvClusterNode = new KVClusterNode();
        try {
            kvClusterNode.start(confPath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void start(final String inputConf) throws IOException, InterruptedException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final RheaKVStoreOptions opts = mapper.readValue(new File(inputConf), RheaKVStoreOptions.class);

        System.out.println("RheaKVTestCluster init ...");
        String dbPath = "rhea_pd_db";
        File file = new File(dbPath);
        if (file.exists()) {
            FileUtils.forceDelete(file);
        }
        file = new File(dbPath);
        if (file.mkdir()) {
            this.tempDbPath = file.getAbsolutePath();
            System.out.println("make dir: " + this.tempDbPath);
        }

        String raftPath = "rhea_pd_raft";
        file = new File(raftPath);
        if (file.exists()) {
            FileUtils.forceDelete(file);
        }
        file = new File(raftPath);
        if (file.mkdir()) {
            this.tempRaftPath = file.getAbsolutePath();
            System.out.println("make dir: " + this.tempRaftPath);
        }

        final Set<Long> regionIds = new HashSet<>();
        for (final RegionEngineOptions rOpts : opts.getStoreEngineOptions().getRegionEngineOptionsList()) {
            regionIds.add(rOpts.getRegionId());
        }

        final RheaKVStore rheaKVStore = new DefaultRheaKVStore();
        if (rheaKVStore.init(opts)) {
            stores.add(rheaKVStore);
        } else {
            System.err.println("Fail to init rhea kv store witch conf: " + inputConf);
        }

        Thread.sleep(10000);

        final PlacementDriverClient pdClient = stores.get(0).getPlacementDriverClient();
        for (final Long regionId : regionIds) {
            final Endpoint leader = pdClient.getLeader(regionId, true, 10000);
            System.out.println("The region " + regionId + " leader is: " + leader);
        }
    }

    protected void shutdown() throws IOException {
        System.out.println("RheaKVTestCluster shutdown ...");
        for (RheaKVStore store : stores) {
            store.shutdown();
        }
        if (this.tempDbPath != null) {
            System.out.println("removing dir: " + this.tempDbPath);
            FileUtils.forceDelete(new File(this.tempDbPath));
        }
        if (this.tempRaftPath != null) {
            System.out.println("removing dir: " + this.tempRaftPath);
            FileUtils.forceDelete(new File(this.tempRaftPath));
        }
        System.out.println("RheaKVTestCluster shutdown complete");
    }

    protected RheaKVStore getLeaderStore(long regionId) {
        for (int i = 0; i < 20; i++) {
            for (RheaKVStore store : stores) {
                if (((DefaultRheaKVStore) store).isLeader(regionId)) {
                    return store;
                }
            }
            System.out.println("fail to find leader, try again");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                // ignored
            }
        }
        throw new NotLeaderException("no leader");
    }

    protected RheaKVStore getFollowerStore(long regionId) {
        for (int i = 0; i < 20; i++) {
            for (RheaKVStore store : stores) {
                if (!((DefaultRheaKVStore) store).isLeader(regionId)) {
                    return store;
                }
            }
            System.out.println("fail to find follower, try again");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                // ignored
            }
        }
        throw new NotLeaderException("no follower");
    }

    public void putAndGetValue() {
        final RheaKVStore store = getLeaderStore(ThreadLocalRandom.current().nextInt(1, 2));
        final String key = UUID.randomUUID().toString();

        String finalStrResult = "";
        String element = UUID.randomUUID().toString();

        for (int i = 0; i < 100; i++) {
            finalStrResult += element;
        }

        final byte[] value = BytesUtil.writeUtf8(finalStrResult);
        store.bPut(key, value);
        System.out.println("====> Key:" + key);
    }

}
