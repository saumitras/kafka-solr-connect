name := "KafkaConnectSolrSource"

version := "1.0"

scalaVersion := "2.12.3"

resolvers ++= Seq(
  "Typesafe" at "https://repo.typesafe.com/typesafe/releases/",
  "confluent" at "http://packages.confluent.io/maven/"
)

libraryDependencies ++= Seq(
  "org.apache.kafka" % "connect-api" % "2.2.1" withSources(),
  //"org.apache.solr" % "solr-core" % "8.2.0",
  //"org.apache.solr" % "solr-common" % "1.3.0",
  "org.apache.solr" % "solr-solrj" % "8.2.0",
  "commons-io" % "commons-io" % "2.4"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("org", "log4j", xs@_*) => MergeStrategy.first
  case x => (assemblyMergeStrategy in assembly).value(x)
}

