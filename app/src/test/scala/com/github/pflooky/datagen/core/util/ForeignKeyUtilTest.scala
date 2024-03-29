package com.github.pflooky.datagen.core.util

import com.github.pflooky.datagen.core.model.SinkOptions

import java.sql.Date
import java.time.LocalDate
import scala.io.Source

class ForeignKeyUtilTest extends SparkSuite {

  test("When no foreign keys defined, return back same dataframes") {
    val sinkOptions = SinkOptions(None, Map())
    val dfMap = Map("name" -> sparkSession.emptyDataFrame)

    val result = ForeignKeyUtil.getDataFramesWithForeignKeys(sinkOptions, dfMap)

    assert(dfMap == result)
  }

  test("Can link foreign keys between data sets") {
    val sinkOptions = SinkOptions(None, Map("postgres.account.account_id" -> List("postgres.transaction.account_id")))
    val accountsList = List(Account("acc1", "peter", Date.valueOf(LocalDate.now()), 10))
    val transactionList = List(
      Transaction("some_acc9", "id123", Date.valueOf(LocalDate.now()), 10.0),
      Transaction("some_acc9", "id124", Date.valueOf(LocalDate.now()), 23.9)
    )
    val dfMap = Map(
      "postgres.account" -> sparkSession.createDataFrame(accountsList),
      "postgres.transaction" -> sparkSession.createDataFrame(transactionList)
    )

    val result = ForeignKeyUtil.getDataFramesWithForeignKeys(sinkOptions, dfMap)

    val resTxnRows = result("postgres.transaction").collect()
    resTxnRows.foreach(r => r.getString(0) == "acc1")
  }


  ignore("Can create random json generator") {
    val sampleJsonFile = Source.fromURL(getClass.getResource("/sample/json/sample.json"))
    val sampleJson = sampleJsonFile.getLines().mkString
    import sparkSession.implicits._
    val json = sparkSession.createDataset(Seq(sampleJson))
    val dataType = sparkSession.read.option("inferTimestamp", "true").json(json)
    sampleJsonFile.close()
  }

  case class Account(account_id: String, name: String, open_date: Date, age: Int)

  case class Transaction(account_id: String, transaction_id: String, created_date: Date, amount: Double)
}
