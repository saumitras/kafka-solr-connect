package solrconnect

import org.slf4j.{Logger, LoggerFactory}

trait ConnectorLogger {
  val log: Logger = LoggerFactory.getLogger(this.getClass)
}
