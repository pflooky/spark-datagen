package com.github.pflooky.datagen.core.generator

import com.github.pflooky.datagen.core.model._
import com.github.pflooky.datagen.core.util.SparkSuite
import org.apache.spark.sql.types.{DoubleType, StringType}

class DataGeneratorFactoryTest extends SparkSuite {

  private val dataGeneratorFactory = new DataGeneratorFactory(None)
  private val schema = Schema("manual", Some(
    List(
      Field("id", "string", Generator("random", Map())),
      Field("amount", "double", Generator("random", Map())),
      Field("debit_credit", "string", Generator("oneOf", Map("oneOf" -> List("D", "C")))),
      Field("name", "string", Generator("regex", Map("regex" -> "[A-Z][a-z]{2,6} [A-Z][a-z]{2,8}"))),
    )
  ))
  private val simpleSchema = Schema("manual", Some(List(Field("id", "string", Generator("random", Map())))))

  test("Can generate data for basic step") {
    val step = Step("transaction", "parquet", Count(total = Some(10)), Map("path" -> "sample/output/parquet/transactions"), schema)

    val df = dataGeneratorFactory.generateDataForStep(step, "parquet")

    assert(df.count() == 10L)
    assert(df.columns sameElements Array("id", "amount", "debit_credit", "name"))
    assert(df.schema.fields.map(x => (x.name, x.dataType)) sameElements Array(
      ("id", StringType),
      ("amount", DoubleType),
      ("debit_credit", StringType),
      ("name", StringType),
    ))
    val sampleRow = df.head()
    assert(sampleRow.getString(0).nonEmpty && sampleRow.getString(0).length <= 20)
    assert(sampleRow.getDouble(1) >= 0.0 && sampleRow.getDouble(1) <= 1.0)
    assert(sampleRow.getString(2) == "D" || sampleRow.getString(2) == "C")
    assert(sampleRow.getString(3).matches("[A-Z][a-z]{2,6} [A-Z][a-z]{2,8}"))
  }

  test("Can generate data when number of rows per column is defined") {
    val step = Step("transaction", "parquet",
      Count(total = Some(10), perColumn = Some(PerColumnCount(List("id"), Some(2)))),
      Map("path" -> "sample/output/parquet/transactions"), simpleSchema)

    val df = dataGeneratorFactory.generateDataForStep(step, "parquet")

    assert(df.count() == 20L)
    val sampleId = df.head().getAs[String]("id")
    val sampleRows = df.filter(_.getAs[String]("id") == sampleId)
    assert(sampleRows.count() == 2L)
  }

  test("Can generate data with generated number of rows per column by a generator") {
    val step = Step("transaction", "parquet", Count(Some(10),
      perColumn = Some(PerColumnCount(List("id"), None, Some(Generator("random", Map("minValue" -> 1, "maxValue" -> 2))))), None),
      Map("path" -> "sample/output/parquet/transactions"), simpleSchema)

    val df = dataGeneratorFactory.generateDataForStep(step, "parquet")

    assert(df.count() >= 10L)
    assert(df.count() <= 20L)
    val sampleId = df.head().getAs[String]("id")
    val sampleRows = df.filter(_.getAs[String]("id") == sampleId)
    assert(sampleRows.count() >= 1L)
    assert(sampleRows.count() <= 2L)
  }

  test("Can generate data with generated number of rows generated by a data generator") {
    val step = Step("transaction", "parquet", Count(None,
      perColumn = None,
      generator = Some(Generator("random", Map("minValue" -> 10, "maxValue" -> 20)))),
      Map("path" -> "sample/output/parquet/transactions"), simpleSchema)

    val df = dataGeneratorFactory.generateDataForStep(step, "parquet")

    assert(df.count() >= 10L)
    assert(df.count() <= 20L)
    val sampleId = df.head().getAs[String]("id")
    val sampleRows = df.filter(_.getAs[String]("id") == sampleId)
    assert(sampleRows.count() == 1L)
  }

}
