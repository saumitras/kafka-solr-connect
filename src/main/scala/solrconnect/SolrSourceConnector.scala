package solrconnect

import java.util
import solrconnect.Constants.Props._
import solrconnect.Constants.Documentation._
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector
import com.sun.xml.internal.ws.util.VersionUtil

class SolrSourceConnector extends SourceConnector with ConnectorLogger {

  private var topicPrefix: String = _
  private var zkHost: String = _
  private var zkChroot: String = _
  private var collectionName: String = _
  private var batchSize: String = _
  private var query: String = _

  private val CONFIG_DEF: ConfigDef = {
    new ConfigDef()
      .define(TOPIC_PREFIX, Type.STRING, Importance.HIGH, PREFIX_TO_ADD_TO_COLLECTION_NAME_TO_GET_TOPIC_NAME)
      .define(ZK_HOST, Type.STRING, Importance.HIGH, ZOOKEEPER_HOST_COMMA_SEPARATED)
      .define(ZK_CHROOT, Type.STRING, Importance.HIGH, CHROOT_IF_AVAILABLE)
      .define(COLLECTION_NAME, Type.STRING, Importance.HIGH, NAME_OF_COLLECTION_TO_INDEX)
      .define(BATCH_SIZE, Type.STRING, Importance.LOW, NUMBER_OF_DOCS_TO_READ_IN_EACH_BATCH)
      .define(QUERY, Type.STRING, Importance.HIGH, SOLR_QUERY_FILTER_WHILE_FETCHING_DOCS)
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

  override def start(props: util.Map[String, String]): Unit = {
    val parsedConfig: AbstractConfig = new AbstractConfig(CONFIG_DEF, props)
    topicPrefix = parsedConfig.getString(TOPIC_PREFIX)
    zkHost = parsedConfig.getString(ZK_HOST)
    zkChroot = parsedConfig.getString(ZK_CHROOT)
    collectionName = parsedConfig.getString(COLLECTION_NAME)
    batchSize = parsedConfig.getString(BATCH_SIZE)
    query = parsedConfig.getString(QUERY)
  }

  override def stop(): Unit = {
    log.info("Stopping connector. Closing all client connections")
    SolrClient.closeClients()
  }

  override def taskClass(): Class[_ <: Task] = classOf[SolrSourceTask]

  override def taskConfigs(i: Int): util.List[util.Map[String, String]] = {
    val configs = new java.util.ArrayList[java.util.Map[String, String]]()

    val config = new java.util.HashMap[String, String]()
    config.put(TOPIC_PREFIX, topicPrefix)
    config.put(ZK_HOST, zkHost)
    config.put(ZK_CHROOT, zkChroot)
    config.put(COLLECTION_NAME, collectionName)
    config.put(BATCH_SIZE, batchSize)
    config.put(QUERY, query)
    configs.add(config)

    configs
  }

}
