/*
 * Copyright (c) 2015 MongoDB, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package tour;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterEvent;
import com.mongodb.event.ClusterListener;

public class ClusterListenerTour {
    public static void main(String[] args) throws InterruptedException {
        MongoClientOptions options = MongoClientOptions.builder()
                .addClusterListener(new ClusterListener() {
                    @Override
                    public void clusterOpened(final ClusterEvent event) {
                    }

                    @Override
                    public void clusterClosed(final ClusterEvent event) {
                    }

                    @Override
                    public void clusterDescriptionChanged(final ClusterDescriptionChangedEvent event) {
                        System.out.println("NEW CLUSTER DESCRIPTION: " + event.getClusterDescription().getShortDescription());
                    }
                }).build();

        MongoClient client = new MongoClient("localhost", options);
        Thread.sleep(Long.MAX_VALUE);
    }
}
