package solrconnect

import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrDocument
import solrconnect.Constants.SolrFieldAttribute._
import solrconnect.Constants.SolrDataType._
import scala.collection.JavaConverters._
import scala.util.Try

case class SolrFieldInfo(name: String, datatype: String, isMultivalued: Boolean, isIndexed: Boolean, isStored: Boolean)

class SchemaManager(zkHost: String, zkChroot: String, collectionName: String) {
  lazy val SOLR_SCHEMA: Schema = convertSolrSchemaToKafkaSchema(getSchema(zkHost, zkChroot, collectionName))

  private def getSchema(zkHost: String, chroot: String, collection: String): List[SolrFieldInfo] = {
    val solrClient = SolrClient(zkHost, chroot, collection)

    val req = new SchemaRequest.Fields()
    val res = req.process(solrClient.client)

    val fields = res.getFields.asScala

    val parsedFields = fields.map { f =>
      val name = f.get(NAME).toString
      val datatype = f.get(TYPE).toString
      val isMultivalued = Try(f.get(MULTIVALUED).toString.toBoolean).getOrElse(false)
      val isIndexed = Try(f.get(INDEXED).toString.toBoolean).getOrElse(false)
      val isStored = Try(f.get(STORED).toString.toBoolean).getOrElse(false)

      SolrFieldInfo(name, datatype, isMultivalued, isIndexed, isStored)
    }

    parsedFields.toList
  }


  private def convertSolrSchemaToKafkaSchema(fields: List[SolrFieldInfo]): Schema = {
    val schema = SchemaBuilder.struct().name("data").version(1)

    fields.foreach { f =>
      val datatype = f.datatype.toUpperCase match {
        case STRING => Schema.STRING_SCHEMA
        case PINT => Schema.INT32_SCHEMA
        case PLONG => Schema.INT64_SCHEMA
        case _ => Schema.STRING_SCHEMA
      }

      schema.field(f.name, datatype)
    }
    schema.build()
  }

  def convertSolrDocToKafkaMsg(doc: SolrDocument): Struct = {

    val struct = new Struct(SOLR_SCHEMA)
    val fields = SOLR_SCHEMA.fields().asScala.toList

    fields.map { f =>
      val fieldName = f.name

      if (doc.getFieldValue(fieldName) != null) {
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

object SchemaManager {
  def apply(zkHost: String, zkChroot: String, collectionName: String): SchemaManager =
    new SchemaManager(zkHost, zkChroot, collectionName)
}
