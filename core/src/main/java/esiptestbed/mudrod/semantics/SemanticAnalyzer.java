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
package esiptestbed.mudrod.semantics;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.distributed.CoordinateMatrix;

import esiptestbed.mudrod.discoveryengine.MudrodAbstract;
import esiptestbed.mudrod.driver.ESDriver;
import esiptestbed.mudrod.driver.SparkDriver;
import esiptestbed.mudrod.utils.LinkageTriple;
import esiptestbed.mudrod.utils.MatrixUtil;
import esiptestbed.mudrod.utils.SimilarityUtil;

/**
 * ClassName: SemanticAnalyzer Function: Semantic analyzer
 */
public class SemanticAnalyzer extends MudrodAbstract {

  /**
   * Creates a new instance of SemanticAnalyzer.
   *
   * @param props
   *          the Mudrod configuration
   * @param es
   *          the Elasticsearch drive
   * @param spark
   *          the spark drive
   */
  public SemanticAnalyzer(Properties props, ESDriver es, SparkDriver spark) {
    super(props, es, spark);
  }

  /**
   * CalTermSimfromMatrix: Calculate term similarity from matrix.
   *
   * @param csvFileName
   *          csv file of matrix, each row is a term, and each column is a
   *          dimension in feature space
   * @return Linkage triple list
   */
  public List<LinkageTriple> calTermSimfromMatrix(String csvFileName) {
    return this.calTermSimfromMatrix(csvFileName, 1);
  }

  public List<LinkageTriple> calTermSimfromMatrix(String csvFileName,
      int skipRow) {

    JavaPairRDD<String, Vector> importRDD = MatrixUtil.loadVectorFromCSV(spark,
        csvFileName, skipRow);
    CoordinateMatrix simMatrix = SimilarityUtil
        .CalSimilarityFromVector(importRDD.values());
    JavaRDD<String> rowKeyRDD = importRDD.keys();
    return SimilarityUtil.MatrixtoTriples(rowKeyRDD, simMatrix);
  }

  public List<LinkageTriple> calTermSimfromMatrix(String csvFileName,
      int simType, int skipRow) {

    JavaPairRDD<String, Vector> importRDD = MatrixUtil.loadVectorFromCSV(spark,
        csvFileName, skipRow);
    JavaRDD<LinkageTriple> triples = SimilarityUtil
        .CalSimilarityFromVector(importRDD, simType);

    return triples.collect();
  }

  public void saveToES(List<LinkageTriple> triple_List, String index,
      String type) {
    try {
      LinkageTriple.insertTriples(es, triple_List, index, type);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Method of saving linkage triples to Elasticsearch.
   * @param tripleList linkage triple list
   * @param index index name
   * @param type type name
   * @param bTriple bTriple
   * @param bSymmetry bSymmetry
   */
  public void saveToES(List<LinkageTriple> tripleList, String index,
      String type, boolean bTriple, boolean bSymmetry) {
    try {
      LinkageTriple.insertTriples(es, tripleList, index, type, bTriple,
          bSymmetry);
    } catch (IOException e) {
      e.printStackTrace();

    }
  }
}
