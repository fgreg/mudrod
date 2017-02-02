/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esiptestbed.mudrod.weblog.pre;

import java.util.Properties;

import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import esiptestbed.mudrod.driver.ESDriver;
import esiptestbed.mudrod.driver.SparkDriver;

/**
 * Supports ability to remove raw logs after processing is finished
 */
public class RemoveRawLog extends LogAbstract {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(RemoveRawLog.class);

  public RemoveRawLog(Properties props, ESDriver es, SparkDriver spark) {
    super(props, es, spark);
  }

  @Override
  public Object execute() {
    LOG.info("Starting raw log removal.");
    startTime = System.currentTimeMillis();
    es.deleteAllByQuery(logIndex, httpType, QueryBuilders.matchAllQuery());
    es.deleteAllByQuery(logIndex, ftpType, QueryBuilders.matchAllQuery());
    endTime = System.currentTimeMillis();
    es.refreshIndex();
    LOG.info("Raw log removal complete. Time elapsed {} seconds.",
        (endTime - startTime) / 1000);
    return null;
  }

  @Override
  public Object execute(Object o) {
    return null;
  }

}
