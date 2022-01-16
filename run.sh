~/spark-3.1.2-bin-hadoop3.2/bin/spark-submit \
--master "local[4]" \
--driver-memory 2g \
--conf "spark.serializer=org.apache.spark.serializer.KryoSerializer" \
--conf "spark.memory.fraction=0.7" \
--conf "spark.executor.extraJavaOptions=-XX:G1GC" \
--conf "spark.driver.memoryOverhead=1024" \
--conf "spark.memory.offHeap.enabled=true" \
--conf "spark.memory.offHeap.size=1g" \
--class org.data.Ingestion \
target/scala-2.12/timo-pipeline.jar ~/Downloads/landing 2018-10-01 2018-11-30

#--conf "spark.sql.shuffle.partitions=500" \