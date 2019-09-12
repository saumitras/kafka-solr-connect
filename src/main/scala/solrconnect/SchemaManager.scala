package solrconnect

import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrDocument

import scala.collection.JavaConverters._
import scala.util.Try

case class SolrFieldInfo(name:String, datatype:String, isMultivalued:Boolean, isIndexed:Boolean, isStored:Boolean)

object SchemaManager {

  var SOLR_SCHEMA:Schema = _

  def initSchema(zkHost:String, zkChroot:String, collectionName:String) = {
    SOLR_SCHEMA = solrToKafkaSchema(getSchema(zkHost, zkChroot, collectionName))
  }

  def getSchema(zkHost:String, chroot:String, collection:String):List[SolrFieldInfo] = {
    val client = SolrClient.getClient(zkHost, chroot)
    client.setDefaultCollection(collection)

    val req = new SchemaRequest.Fields()
    val resp = req.process(client)

    val fields = resp.getFields.asScala

    val parsedFields = fields.map { f =>
      val name = f.get("name").toString
      val datatype = f.get("type").toString
      val isMultivalued = Try(f.get("multiValued").toString.toBoolean).getOrElse(false)
      val isIndexed = Try(f.get("indexed").toString.toBoolean).getOrElse(false)
      val isStored = Try(f.get("stored").toString.toBoolean).getOrElse(false)
      val isRequired = Try(f.get("required").toString.toBoolean).getOrElse(false)

      SolrFieldInfo(name, datatype, isMultivalued, isIndexed, isStored)
    }

    parsedFields.toList
  }


  def solrToKafkaSchema(fields:List[SolrFieldInfo]) = {
    val schema = SchemaBuilder.struct().name("data").version(1)
    fields.foreach { f =>
      val datatype = f.datatype.toUpperCase match {
        case "STRING" => Schema.STRING_SCHEMA
        case "PINT" => Schema.INT32_SCHEMA
        case "PLONG" => Schema.INT64_SCHEMA
        case _ => Schema.STRING_SCHEMA
      }

      schema.field(f.name, datatype)
    }
    schema.build()
  }




  def solrDocToKafkaMsg(doc:SolrDocument) = {

    val fields = SOLR_SCHEMA.fields().asScala.toList

    val struct = new Struct(SOLR_SCHEMA)

    fields.map { f =>
      val fieldName = f.name

      if(doc.getFieldValue(fieldName) != null) {
        f.schema match {
          case Schema.STRING_SCHEMA =>
            struct.put(fieldName, doc.getFieldValue(fieldName).toString)
          case Schema.INT32_SCHEMA =>
            struct.put(fieldName, doc.getFieldValue(fieldName).toString.toInt)
          case Schema.INT64_SCHEMA =>
            struct.put(fieldName, doc.getFieldValue(fieldName).toString.toLong)
          case _ =>
            struct.put(fieldName, doc.getFieldValue(fieldName).toString)
        }
      }
    }

    struct

  }

}
