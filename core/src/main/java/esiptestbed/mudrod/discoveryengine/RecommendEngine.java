package esiptestbed.mudrod.discoveryengine;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import esiptestbed.mudrod.driver.ESDriver;
import esiptestbed.mudrod.driver.SparkDriver;
import esiptestbed.mudrod.recommendation.pre.ImportMetadata;
import esiptestbed.mudrod.recommendation.pre.MetadataTFIDFGenerator;
import esiptestbed.mudrod.recommendation.pre.NormalizeVariables;
import esiptestbed.mudrod.recommendation.pre.SessionCooccurence;
import esiptestbed.mudrod.recommendation.process.AbstractBasedSimilarity;
import esiptestbed.mudrod.recommendation.process.VariableBasedSimilarity;
import esiptestbed.mudrod.recommendation.process.sessionBasedCF;

public class RecommendEngine extends DiscoveryEngineAbstract {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory
      .getLogger(RecommendEngine.class);

  public RecommendEngine(Properties props, ESDriver es, SparkDriver spark) {
    super(props, es, spark);
    // TODO Auto-generated constructor stub
    LOG.info("Started Mudrod Recommend Engine.");
  }

  @Override
  public void preprocess() {
    // TODO Auto-generated method stub
    LOG.info(
        "*****************Recommendation preprocessing starts******************");

    startTime = System.currentTimeMillis();

    DiscoveryStepAbstract harvester = new ImportMetadata(this.props, this.es,
        this.spark);
    harvester.execute();

    DiscoveryStepAbstract tfidf = new MetadataTFIDFGenerator(this.props,
        this.es, this.spark);
    tfidf.execute();

    DiscoveryStepAbstract sessionMatrixGen = new SessionCooccurence(this.props,
        this.es, this.spark);
    sessionMatrixGen.execute();

    DiscoveryStepAbstract transformer = new NormalizeVariables(this.props,
        this.es, this.spark);
    transformer.execute();

    endTime = System.currentTimeMillis();

    LOG.info(
        "*****************Recommendation preprocessing  ends******************Took {}s {}",
        (endTime - startTime) / 1000);
  }

  @Override
  public void process() {
    // TODO Auto-generated method stub
    LOG.info(
        "*****************Recommendation processing starts******************");

    startTime = System.currentTimeMillis();

    DiscoveryStepAbstract tfCF = new AbstractBasedSimilarity(this.props,
        this.es, this.spark);
    tfCF.execute();

    DiscoveryStepAbstract cbCF = new VariableBasedSimilarity(this.props,
        this.es, this.spark);
    cbCF.execute();

    DiscoveryStepAbstract sbCF = new sessionBasedCF(this.props, this.es,
        this.spark);
    sbCF.execute();

    endTime = System.currentTimeMillis();

    LOG.info(
        "*****************Recommendation processing ends******************Took {}s {}",
        (endTime - startTime) / 1000);
  }

  @Override
  public void output() {
    // TODO Auto-generated method stub

  }

}
