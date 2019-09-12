package solrconnect


import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.apache.solr.common.SolrDocument

import scala.collection.concurrent.TrieMap
import scala.collection.JavaConverters._

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.SortClause
import org.apache.solr.common.params.CursorMarkParams

object SolrClient extends Logging {

  val clients = TrieMap[String, CloudSolrClient]()

  def getClient(zkHost:String, chroot:String):CloudSolrClient = {
    val key = zkHost + chroot
    log.info(s"Requesting new client for zkHost:$key from cache")

    clients.get(key) match {
      case Some(client) => client
      case None =>
        log.info(s"No existing client found for zkHost=$key in cache. Creating a new client.")
        val client = new CloudSolrClient.Builder(zkHost.split(",").toList.asJava, java.util.Optional.ofNullable(chroot))
          .withConnectionTimeout(30000)
          .withSocketTimeout(60000)
          .build()

        clients.put(key, client)
        getClient(zkHost, chroot)
    }
  }

  def querySolr(client:CloudSolrClient, query:String, numRows:Int, cursorMark:String): (String, List[SolrDocument]) = {
    val q = new SolrQuery(query).setRows(numRows).setSort(SortClause.asc("id"))
    q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark)
    log.info("Solr Query: " + q.toString)

    val rsp = client.query(q)

    val nextCursorMark = rsp.getNextCursorMark
    val docs = rsp.getResults.asScala.toList

    (nextCursorMark, docs)
  }

}


