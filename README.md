## Overview
This project is to ingest data in `Json` format into optimal data format for reporting purpose. 
The considerated data format is `Apache Parquet`, which is a columnar storage format available to any project in the Hadoop ecosystem, 
regardless of the choice of data processing framework, 
data model or programming language. And the pipeline is implemented in `Apache Spark`.
## Why should use `Apache Parquet`
There are some key reasons that `Apache Parquet` is a good choice:
* Designed to bring efficiency compared to row-based files like CSV. 
When querying, you can skip over the non-relevant data very quickly.
* Support advanced nested data structures.
* Optimized for queries that process large volumes of data.
* Support `schema evolution`, 
which mean dynamically handle change in data schema.
* works best with interactive and serverless technologies like AWS Athena, 
Amazon Redshift Spectrum, Google BigQuery and Google Dataproc.
## Why should use `Apache Spark`
There are some key reasons that `Apache Spark` is a good choice:
* Easy to load almost all data format.
* Easy to scale vertically and horizontally.
* Consume data at high speed.
## Data architecture
Folder structure:
```
- curated 
        |
        + - isNullUserId=false
        |       |
        |       + - date=2018-10-01
        |       + - date=2018-10-02
        |       + - date=2018-10-03
        |       ...
        |       + - date=2018-11-30
        |
        + - isNullUserId=true
                |
                + - date=2018-10-01
                + - date=2018-10-02
                + - date=2018-10-03
                ...
                + - date=2018-11-30
```
This data is partitioned by 2 fields: '_**isNullUserId**_' and '_**date**_'.

With '_**isNullUserId**_', it splits the data into 2 parts: 
* **isNullUserId=false**: store all records that `userId` **is not null**.
* **isNullUserId=true**: store all records that `userId` **is null**.

With '_**date**_', it helps the query engine to scan only the necessary data, not the whole table.

This data architecture does not match the requirement completely, 
but it helps BI team to query the data faster and easier, 
by separating `nullUserId` by a field, not by path.

For example:
```scala
val df = spark.read.option("mergeSchema", "true").parquet("curated")
//query record that `userId` is not null
df.filter($"isNullUserId" === "false").show()
//query record that `userId` is null
df.filter($"isNullUserId" === "true").show()
//query the whole data
df.show()
```


## How to run
### Assumption
Your machine already installed `Apache Spark` (version 3.1.2), `Scala` (version 2.12.10), `sbt` and `sbt-assembly`.
### Step
1. Build jar file ```sbt assembly```.
2. Submit the job 
```shell
   path/to/spark/bin/spark-submit \
   --master "local[4]" \
   --driver-memory 2g \
   --conf "spark.serializer=org.apache.spark.serializer.KryoSerializer" \
   --conf "spark.memory.fraction=0.7" \
   --conf "spark.executor.extraJavaOptions=-XX:G1GC" \
   --conf "spark.driver.memoryOverhead=1024" \
   --conf "spark.memory.offHeap.enabled=true" \
   --conf "spark.memory.offHeap.size=1g" \
   --class org.data.Ingestion \
   path/to/jar_file/timo-pipeline.jar <path/to/data/landing> <start_date YYYY-mm-dd> [<end-date YYYY-mm-dd>]
```
This pipeline has 3 running mode:
1. **Inital load**: (load all data) just specific the data path
2. **Incremental load**: specific the data path and <start-date>
3. **Load by range**: specific the data path, <start-date> and <end-date>

## Anwser the question:
1. Please check the code and the **how to run** section.
2. The reasons to choose `Apache Spark` for pipeline and `Apache Parquet` for data format 
are mentioned in **_Why should use Apache Spark_** and **_Why should use Apache Parquet_** section.
In this project, the **_landing_** data (JSON format) occupies **_1.2GB_** but **_curated_** data (parquet format) only takes **_12MB_**.
And it only take 180 second for **Inital load** based on provided data.
3. Because `Apache Parquet` supports _**schema evolution**_ feature, we need to **do nothing** when the input data schema changed. 
Use the following line of code to read the data:
```scala
val schemaChangeHandlingDataframe = spark.read.option("mergeSchema", "true").parquet("curated")
```
4. If I can make a change at ingestion step, I just want to change the deployment mode of the pipeline from **local** mode to **cluster** mode **when data volume increase dramatically**.

 
