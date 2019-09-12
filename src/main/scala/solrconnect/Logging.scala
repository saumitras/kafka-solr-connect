package solrconnect

import org.slf4j.LoggerFactory
import org.apache.solr.common.params.SolrParams

trait Logging {
  val log = LoggerFactory.getLogger(this.getClass)
}

