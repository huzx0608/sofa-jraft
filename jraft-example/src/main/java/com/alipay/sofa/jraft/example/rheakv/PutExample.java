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
package com.alipay.sofa.jraft.example.rheakv;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.alipay.sofa.jraft.util.BytesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.sofa.jraft.rhea.client.FutureHelper;
import com.alipay.sofa.jraft.rhea.client.RheaKVStore;
import com.alipay.sofa.jraft.rhea.storage.KVEntry;
import com.alipay.sofa.jraft.rhea.util.Lists;

import static com.alipay.sofa.jraft.util.BytesUtil.readUtf8;
import static com.alipay.sofa.jraft.util.BytesUtil.writeUtf8;

/**
 *
 * @author jiachun.fjc
 */
public class PutExample {

    private static final Logger LOG = LoggerFactory.getLogger(PutExample.class);

    public static void main(final String[] args) throws Exception {
        String confPath = args[0];
        final Client client = new Client();
        // client.init(confPath);
        client.init();
        put(client.getRheaKVStore());
        client.shutdown();
    }

    public static void put(final RheaKVStore rheaKVStore) {

        for (int i = 0; i < 100000000; i++) {
            final String key = UUID.randomUUID().toString();

            String finalStrResult = "";
            String element = UUID.randomUUID().toString();
            for (int j = 0; j < 100; j++) {
                finalStrResult += element;
            }
            final byte[] value = BytesUtil.writeUtf8(finalStrResult);
            boolean isSuccess = false;
            do {
                try {
                    isSuccess = rheaKVStore.bPut(key, value);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        if (!isSuccess) {
                            Thread.sleep(10);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } while (!isSuccess);
            if (i % 1000 == 0) {
                System.out.println("Write Value:Index:" + String.valueOf(i) + ",Key:" + key);
            }
        }
    }
}
