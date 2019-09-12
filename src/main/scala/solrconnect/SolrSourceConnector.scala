package solrconnect

import java.util

import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef, ConfigException}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector
import com.sun.xml.internal.ws.util.VersionUtil

class SolrSourceConnector extends SourceConnector with Logging {

  private var topicPrefix:String = _
  private var zkHost:String = _
  private var zkChroot:String = _
  private var collectionName:String = _
  private var batchSize:String = _
  private var query:String = _

  val CONFIG_DEF:ConfigDef = {
    new ConfigDef()
      .define("topicPrefix", Type.STRING, Importance.HIGH, "Prefix to add to collection name to get topic name")
      .define("zkHost", Type.STRING, Importance.HIGH, "Zookeeper host comma separated")
      .define("zkChroot", Type.STRING, Importance.HIGH, "Chroot if available")
      .define("collectionName", Type.STRING, Importance.HIGH, "Name of collection to index")
      .define("batchSize", Type.STRING, Importance.LOW, "Number of docs to read in each batch")
      .define("query", Type.STRING, Importance.HIGH, "Solr query filter while fetching docs")
  }

  override def start(props: util.Map[String, String]): Unit = {
    val parsedConfig: AbstractConfig = new AbstractConfig(CONFIG_DEF, props)
    topicPrefix = parsedConfig.getString("topicPrefix")
    zkHost = parsedConfig.getString("zkHost")
    zkChroot = parsedConfig.getString("zkChroot")
    collectionName = parsedConfig.getString("collectionName")
    batchSize = parsedConfig.getString("batchSize")
    query = parsedConfig.getString("query")

  }

  override def taskClass(): Class[_ <: Task] = classOf[SolrSourceTask]

  override def taskConfigs(i: Int): util.List[util.Map[String, String]] = {
    val configs = new java.util.ArrayList[java.util.Map[String, String]]()

    val config = new java.util.HashMap[String, String]()
    config.put("topicPrefix", topicPrefix)
    config.put("zkHost", zkHost)
    config.put("zkChroot", zkChroot)
    config.put("collectionName", collectionName)
    config.put("batchSize", batchSize)
    config.put("query", query)
    configs.add(config)

    configs
  }

  override def stop(): Unit = {
    log.info("Stopping connector. Closing all client connections")
    SolrClient.clients.values.foreach(_.close)
  }

  override def version(): String = {
    try {
      classOf[VersionUtil].getPackage.getImplementationVersion
    } catch {
      case ex: Exception =>
        "0.0.1"
    }
  }

  override def config(): ConfigDef = CONFIG_DEF

}
