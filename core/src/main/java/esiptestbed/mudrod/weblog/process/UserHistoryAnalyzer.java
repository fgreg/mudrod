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
package esiptestbed.mudrod.weblog.process;

import java.util.List;
import java.util.Properties;

import esiptestbed.mudrod.discoveryengine.DiscoveryStepAbstract;
import esiptestbed.mudrod.driver.ESDriver;
import esiptestbed.mudrod.driver.SparkDriver;
import esiptestbed.mudrod.semantics.SemanticAnalyzer;
import esiptestbed.mudrod.utils.LinkageTriple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports ability to calculate term similarity based on user history
 */
public class UserHistoryAnalyzer extends DiscoveryStepAbstract {
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(UserHistoryAnalyzer.class);
  
  public UserHistoryAnalyzer(Properties props, ESDriver es,
      SparkDriver spark) {
    super(props, es, spark);
  }

  /**
   * Method of executing user history analyzer
   */
  @Override
  public Object execute() {
    LOG.info("Starting UserHistoryAnalyzer...");
    startTime = System.currentTimeMillis();

    SemanticAnalyzer sa = new SemanticAnalyzer(props, es, spark);
    List<LinkageTriple> tripleList = sa
        .calTermSimfromMatrix(props.getProperty("userHistoryMatrix"));
    sa.saveToES(tripleList, props.getProperty("indexName"),
        props.getProperty("userHistoryLinkageType"));

    endTime = System.currentTimeMillis();
    es.refreshIndex();
    LOG.info("UserHistoryAnalyzer complete. Time elapsed: {}s"
        , (endTime - startTime) / 1000);
    return null;
  }

  @Override
  public Object execute(Object o) {
    return null;
  }

}
