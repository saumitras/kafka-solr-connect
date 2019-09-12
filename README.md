# kafka-connect-source-example

Basic example of Kafka Source Connector to read data from Solr and write to Kafka

How to run:

- Clone the repository
- `sbt assembly` to get fat jar in `kafka-connect-solr-source/target/scala-2.12/`
- Use worker and connector config file in src/main/resources to start the connector. Modify the config values as needed.
