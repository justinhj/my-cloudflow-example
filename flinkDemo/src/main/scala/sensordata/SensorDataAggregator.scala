package sensordata

// Flink streamlet to aggregate sensor data states. We want to keep counts of
// how many reports a sensor made in each state and an error if the states
// increase by more than one, or decrease

import org.apache.flink.streaming.api.scala._
import java.util.UUID
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro._
import cloudflow.flink._
import org.slf4j.LoggerFactory
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink
import org.apache.flink.api.common.serialization.SimpleStringEncoder
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy
import java.util.concurrent.TimeUnit

class SensorDataAggregator extends FlinkStreamlet {

  val logger = LoggerFactory.getLogger("SensorDataAggregator")

  @transient val in    = AvroInlet[SensorData]("in")
  @transient val shape = StreamletShape.withInlets(in)

  val outputPath = "file:///tmp/sample.txt"

  val sink: StreamingFileSink[String] = StreamingFileSink
    .forRowFormat(new Path(outputPath), new SimpleStringEncoder[String]("UTF-8"))
    .withRollingPolicy(
        DefaultRollingPolicy.builder()
            .withRolloverInterval(TimeUnit.MINUTES.toMillis(15))
            .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
            .withMaxPartSize(1024 * 1024 * 1024)
            .build())
    .build()

  override protected def createLogic() = new FlinkStreamletLogic {

    override def buildExecutionGraph = {

      val ds: KeyedStream[SensorData, UUID] = readStream(in).keyBy(sd => sd.deviceId)

      val ss = ds.map {
        x =>
          logger.warn(s"Output string ${x.toString()}")
          x.toString()
      }

      ss.addSink(sink)

      // ds.fold(Map.empty[Int, Int]) {
      //     case (acc, sensor) => {
      //       val stateCount = acc.getOrElse(sensor.measurements.state, 0)
      //       val newCount   = stateCount + 1
      //       logger.warn(s"State aggregate for ${sensor.deviceId} state ${sensor.measurements.state} count $newCount")

      //       acc.updated(sensor.measurements.state, newCount)
      //     }
      //   }
      //   .print()

    }
  }

}
