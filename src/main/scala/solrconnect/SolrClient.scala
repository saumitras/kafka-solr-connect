package solrconnect

import java.util.concurrent.ConcurrentHashMap

import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.apache.solr.common.SolrDocument
import scala.collection.JavaConverters._
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.SortClause
import org.apache.solr.common.params.CursorMarkParams

class SolrClient(zkHost: String, chroot: String, collectionName: String) extends Logging {

  val client: CloudSolrClient = new CloudSolrClient.Builder(zkHost.split(",").toList.asJava, java.util.Optional.ofNullable(chroot))
    .withConnectionTimeout(30000)
    .withSocketTimeout(60000)
    .build()

  client.setDefaultCollection(collectionName)

  def querySolr(query: String, numRows: Int, cursorMark: String): (String, List[SolrDocument]) = {
    val q = new SolrQuery(query).setRows(numRows).setSort(SortClause.asc("id"))
    q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark)

    log.info("Solr Query: " + q.toString)

    val queryResponse = client.query(q)

    val nextCursorMark = queryResponse.getNextCursorMark
    val docs = queryResponse.getResults.asScala.toList

    (nextCursorMark, docs)
  }

  def close(): Unit = {
    client.close()
  }
}

object SolrClient extends Logging {

  private val solrClientCache = new ConcurrentHashMap[String, SolrClient]()

  def apply(zkHost: String, chroot: String, collectionName: String): SolrClient = {
    val key = zkHost + chroot
    Option(solrClientCache.get(key)) match {
      case Some(solrClient: SolrClient) => solrClient
      case None =>
        val solrClient: SolrClient = new SolrClient(zkHost, chroot, collectionName)
        solrClientCache.put(key, solrClient)
        solrClient
    }

  }

  def close(): Unit = {
    solrClientCache.values().asScala.foreach(_.close())
  }

}
