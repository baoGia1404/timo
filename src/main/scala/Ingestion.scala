package org.data

import org.apache.spark.sql.functions.{col, from_unixtime, to_date, when}
import org.apache.spark.sql.{SaveMode, SparkSession}

import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Ingestion {
  def parseCommandLineArguments(args: Array[String]): (String, Option[(LocalDate, LocalDate)]) = {
    def parseDate(str: String): LocalDate = LocalDate.parse(str, DateTimeFormatter.ISO_DATE) // YYYY-MM-DD
    val argLen = args.length
    val prefix = args(0)
    if (argLen < 1) throw new Exception("Accept at least 1 argument.")
    else if (argLen == 1) (prefix, None)
    else if (argLen == 2) {
      val prefix = args(0)
      val loadDate = parseDate(args(1))
      (prefix, Option(loadDate, loadDate))
    }
    else if (argLen == 3) {
      val startDate = parseDate(args(1))
      val endDate = parseDate(args(2))
      if (startDate.isAfter(endDate))
        throw new Exception("startDate must be before endDate.")
      (prefix, Option(startDate, endDate))
    }
    else throw new Exception("Accept at most 3 argument.")
  }
  def generatePath(args: Array[String]): List[String] = {
    val (prefix, startEndDate) = parseCommandLineArguments(args)
    if (startEndDate.nonEmpty)
    // create date range
      startEndDate.get._1.toEpochDay.to(startEndDate.get._2.toEpochDay)
      .map(LocalDate.ofEpochDay)
      // filter existing paths
      .filter(day => Files
        .exists(Paths
          .get(s"$prefix/${day.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))}")))
      // generate wildcard paths
      .map(day => s"$prefix/${day.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))}/*.json")
      .toList
    else List(s"$prefix/*/*/*/*.json")
  }
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .appName("timo-ingest")
      .getOrCreate()
    import spark.implicits._
    spark.read
      .option("multiline", "true")
      .option("mode", "DROPMALFORMED")
      .option("inferSchema", "true")
      .json(spark.read.textFile(generatePath(args):_*))
      .select(
            col("*"),
            to_date(from_unixtime($"ts" / 1000)).as("date"),
            when($"userId" === "", true).otherwise(false).as("isNullUserId")
          )
      // partition data by (isNullUserId, DATE(`ts`))
      .repartition($"isNullUserId", $"date")
      .write
      .mode(SaveMode.Append)
      //save data with partition path
      .partitionBy("isNullUserId", "date")
      .parquet("curated")
  }
}
