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

class SensorDataAggregator extends FlinkStreamlet {

  val logger = LoggerFactory.getLogger("SensorDataAggregator")

  @transient val in    = AvroInlet[SensorData]("in")
  @transient val shape = StreamletShape.withInlets(in)

  override protected def createLogic() = new FlinkStreamletLogic {

    override def buildExecutionGraph =
      try {
        val ds: KeyedStream[SensorData, UUID] = readStream(in).keyBy(sd => sd.deviceId)

        ds.fold(Map.empty[Int, Int]) {
            case (acc, sensor) => {
              val stateCount = acc.getOrElse(sensor.measurements.state, 0)
              val newCount   = stateCount + 1
              logger.warn(s"State aggregate for ${sensor.deviceId} state ${sensor.measurements.state} count $newCount")

              acc.updated(sensor.measurements.state, newCount)
            }
          }
          .print()
      } catch {
        case t: Throwable =>
          logger.error(t.toString())

      }

  }

}
