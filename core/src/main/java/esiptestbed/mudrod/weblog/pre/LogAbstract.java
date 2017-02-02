package esiptestbed.mudrod.weblog.pre;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.spark.Partition;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram.Order;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import esiptestbed.mudrod.discoveryengine.DiscoveryStepAbstract;
import esiptestbed.mudrod.driver.ESDriver;
import esiptestbed.mudrod.driver.SparkDriver;
import esiptestbed.mudrod.main.MudrodConstants;
import esiptestbed.mudrod.weblog.partition.KGreedyPartitionSolver;
import esiptestbed.mudrod.weblog.partition.ThePartitionProblemSolver;
import esiptestbed.mudrod.weblog.partition.logPartitioner;
import scala.Tuple2;

public class LogAbstract extends DiscoveryStepAbstract {

  private static final Logger LOG = LoggerFactory.getLogger(LogAbstract.class);

  public String logIndex = null;
  public String httpType = null;
  public String ftpType = null;
  public String cleanupType = null;
  public String sessionStats = null;
  public int partition = 96;

  public LogAbstract(Properties props, ESDriver es, SparkDriver spark) {
    super(props, es, spark);
    // TODO Auto-generated constructor stub

    if (props != null) {
      initLogIndex();
    }
  }

  protected void initLogIndex() {
    logIndex = props.getProperty(MudrodConstants.LOG_INDEX)
        + props.getProperty(MudrodConstants.TIME_SUFFIX);
    httpType = props.getProperty(MudrodConstants.HTTP_TYPE_PREFIX);
    ftpType = props.getProperty(MudrodConstants.FTP_TYPE_PREFIX);
    cleanupType = props.getProperty(MudrodConstants.CLEANUP_TYPE_PREFIX);
    sessionStats = props.getProperty(MudrodConstants.SESSION_STATS_PREFIX);

    InputStream settingsStream = getClass().getClassLoader()
        .getResourceAsStream(ES_SETTINGS);
    InputStream mappingsStream = getClass().getClassLoader()
        .getResourceAsStream(ES_MAPPINGS);
    JSONObject settingsJSON = null;
    JSONObject mappingJSON = null;

    try {
      settingsJSON = new JSONObject(IOUtils.toString(settingsStream));
    } catch (JSONException | IOException e1) {
      LOG.error("Error reading Elasticsearch settings!", e1);
    }

    try {
      mappingJSON = new JSONObject(IOUtils.toString(mappingsStream));
    } catch (JSONException | IOException e1) {
      LOG.error("Error reading Elasticsearch mappings!", e1);
    }

    try {
      if (settingsJSON != null && mappingJSON != null) {
        this.es.putMapping(logIndex, settingsJSON.toString(),
            mappingJSON.toString());
      }
    } catch (IOException e) {
      LOG.error("Error entering Elasticsearch Mappings!", e);
    }
  }

  @Override
  public Object execute() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object execute(Object o) {
    // TODO Auto-generated method stub
    return null;
  }

  public JavaRDD<String> getUserRDD(String... type) {

    // List<String> users = getUsers();
    // JavaRDD<String> userRDD = spark.sc.parallelize(users, this.partition);

    Map<String, Double> userDocs = getUserDocs(type);
    JavaRDD<String> userRDD = parallizeUsers(userDocs);
    return userRDD;
  }

  public List<String> getUsers(String type) {

    Terms users = this.getUserTerms(type);
    List<String> userList = new ArrayList<String>();
    for (Terms.Bucket entry : users.getBuckets()) {
      String ip = (String) entry.getKey();
      userList.add(ip);
    }

    return userList;
  }

  public Terms getUserTerms(String... type) {

    int docCount = es.getDocCount(logIndex, type);

    SearchResponse sr = es.getClient().prepareSearch(logIndex).setTypes(type)
        .setQuery(QueryBuilders.matchAllQuery()).setSize(0)
        .addAggregation(
            AggregationBuilders.terms("Users").field("IP").size(docCount))
        .execute().actionGet();
    Terms users = sr.getAggregations().get("Users");

    return users;
  }

  public Map<String, Double> getUserDocs(String... type) {

    Terms users = this.getUserTerms(type);
    Map<String, Double> userList = new HashMap<String, Double>();
    for (Terms.Bucket entry : users.getBuckets()) {
      String ip = (String) entry.getKey();
      Long count = entry.getDocCount();
      userList.put(ip, Double.valueOf(count));
    }

    return userList;
  }

  public Map<String, Long> getUserDailyDocs() {

    int docCount = es.getDocCount(logIndex, httpType);

    AggregationBuilder dailyAgg = AggregationBuilders.dateHistogram("by_day")
        .field("Time").dateHistogramInterval(DateHistogramInterval.DAY)
        .order(Order.COUNT_DESC);

    SearchResponse sr = es.getClient().prepareSearch(logIndex)
        .setTypes(httpType).setQuery(QueryBuilders.matchAllQuery())
        .setSize(0).addAggregation(AggregationBuilders.terms("Users")
            .field("IP").size(docCount).subAggregation(dailyAgg))
        .execute().actionGet();
    Terms users = sr.getAggregations().get("Users");
    Map<String, Long> userList = new HashMap<String, Long>();
    for (Terms.Bucket user : users.getBuckets()) {
      String ip = (String) user.getKey();

      System.out.println(ip);

      Histogram agg = user.getAggregations().get("by_day");
      List<? extends Histogram.Bucket> dateList = agg.getBuckets();
      int size = dateList.size();
      for (int i = 0; i < size; i++) {
        Long count = dateList.get(i).getDocCount();
        String date = dateList.get(i).getKey().toString();

        System.out.println(date);
        System.out.println(count);
      }
    }

    return userList;
  }

  protected void checkUserPartition(JavaRDD<String> userRDD) {
    System.out.println("hhhhh");
    List<Partition> partitios = userRDD.partitions();
    System.out.println(partitios.size());
    int[] partitionIds = new int[partitios.size()];
    for (int i = 0; i < partitios.size(); i++) {
      int index = partitios.get(i).index();
      partitionIds[i] = index;
    }

    List<String>[] userIPs = userRDD.collectPartitions(partitionIds);
    for (int i = 0; i < userIPs.length; i++) {
      List<String> iuser = userIPs[i];
      System.out.println(i + " partition");
      System.out.println(iuser.toString());
    }
  }

  public JavaRDD<String> parallizeUsers(Map<String, Double> userDocs) {

    // prepare list for parallize
    List<Tuple2<String, Double>> list = new ArrayList<Tuple2<String, Double>>();
    for (String user : userDocs.keySet()) {
      list.add(new Tuple2<String, Double>(user, userDocs.get(user)));
    }

    // group users
    ThePartitionProblemSolver solution = new KGreedyPartitionSolver();
    Map<String, Integer> UserGroups = solution.solve(userDocs, this.partition);

    JavaPairRDD<String, Double> pairRdd = spark.sc.parallelizePairs(list);
    JavaPairRDD<String, Double> userPairRDD = pairRdd
        .partitionBy(new logPartitioner(UserGroups, this.partition));

    // repartitioned user RDD
    JavaRDD<String> userRDD = userPairRDD.keys();
    // checkUserPartition(userRDD);

    return userRDD;
  }

  public Terms getSessionTerms() {

    int docCount = es.getDocCount(this.logIndex, this.cleanupType);

    SearchResponse sr = es.getClient().prepareSearch(this.logIndex)
        .setTypes(this.cleanupType).setQuery(QueryBuilders.matchAllQuery())
        .addAggregation(AggregationBuilders.terms("Sessions").field("SessionID")
            .size(docCount))
        .execute().actionGet();

    Terms Sessions = sr.getAggregations().get("Sessions");
    return Sessions;
  }

  public List<String> getSessions() {

    Terms Sessions = this.getSessionTerms();
    List<String> sessionList = new ArrayList<String>();
    for (Terms.Bucket entry : Sessions.getBuckets()) {
      if (entry.getDocCount() >= 3 && !entry.getKey().equals("invalid")) {
        String session = (String) entry.getKey();
        sessionList.add(session);
      }
    }

    return sessionList;
  }
}
