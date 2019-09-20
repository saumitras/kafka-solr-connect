# kafka-solr-connect

Kafka Source Connector implementation to read data from *Solr* and write to *Kafka*.

## Setup
**Note**: Please be informed that the steps mentioned below are minimalistic. They may change from one environment to another. They are mentioned here only for the purpose of reference.  

### Zookeeper
- Create a Zookeeper configuration file `zoo.cfg` from `zoo_sample.cfg`.
- Update the `data` dir. Configured as `dataDir=/var/zookeeper` in our case.
- Start Zookeeper `zookeeper-3.4.14/bin/zk_server.sh start`.
- Launch `zookeeper-3.4.14/bin/zkCli.sh` to configure Znodes.
    ```
    [zk: localhost:2181(CONNECTED) 0] create /solr ''
    Created /solr
    [zk: localhost:2181(CONNECTED) 1] create /kafka '' 
    ```

### Kafka
- Update `kafka_2.12-2.3.0/config/server.properties` configuration file. Set `zookeeper.connect=localhost:2181/kafka`.
- Start Kafka `kafka_2.12-2.3.0/bin/kafka-server-start.sh -daemon kafka_2.12-2.3.0/config/server.properties `.

### Solr
- Update `solr-8.2.0/bin/solr.in.sh`. Set `ZK_HOST="localhost:2181/solr"`.
- Start Solr `solr-8.2.0/bin/solr -c`.
- Create a new configset `myconfigset`.
    ```
    solr-8.2.0/server/scripts/cloud-scripts$ ./zkcli.sh -zkhost localhost:2181/solr \
        -cmd upconfig -confdir /opt/solr-8.2.0/server/solr/configsets/sample_techproducts_configs/conf \
        -confname myconfigset
    ```
 - Create a collection `mycollection` using Solr's web interface `http://localhost:8983/solr/#/~collections` that based out of configset `myconfigset`.
 
 ## Build Connector Jar
 The connector jar build in the following steps will be used by Kafka to facilitate data flow from Solr to Kafka.
 - Clone repository `git@github.com:saumitras/kafka-solr-connect.git`.
 - Run `sbt assembly` to build the fat jar under `kafka-solr-connect/target/scala-2.12/`.
 - Move the jar to `kafka-solr-connect/dist/` directory.
 - Update connector configuration properties files.
    ```
    # connect-solr-source.properties
    zkChroot=/solr
    collectionName=mycollection
    
    # connect-standalone.properties
    plugin.path=kafka-solr-connect/dist # Provide absolute path to the connector jar.
    offset.storage.file.filename=/tmp/connect.offsets # This location must be change=d to a non tmp directory location in production.
    ```
 
 ## Start Connector
 Please be informed that the order in which the properties files `connect-standalone.properties` and `connect-solr-source.properties` are supplied to the `connect-standalone.sh` is important.
    ```
    kafka_2.12-2.3.0/bin/connect-standalone.sh kafka-solr-connect/dist/resources/connect-standalone.properties kafka-solr-connect/dist/resources/connect-solr-source.properties
    ```

## Verify Data Flow
- Create a JSON document in Solr `{"id":"doc01"}`.
- Verify Kafka topic creation.
    ```
    kafka_2.12-2.3.0/bin/kafka-topics.sh --list --zookeeper localhost:2181/kafka 
    solr_mycollection
    ```
- Verify message in the Kafka Topic.
    ```
    kafka_2.12-2.3.0/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic solr_mycollection --from-beginning
    Struct{_version_=1644922692298604544,id=doc01}
    ```
