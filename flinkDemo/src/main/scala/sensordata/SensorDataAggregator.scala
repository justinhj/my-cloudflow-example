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
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow

class SensorDataAggregator extends FlinkStreamlet {

  val logger = LoggerFactory.getLogger("SensorDataAggregator")

  @transient val in    = AvroInlet[SensorData]("in")
  @transient val shape = StreamletShape.withInlets(in)

  class SumAggregate extends AggregateFunction[SensorData, Map[Int, Int], Map[Int, Int]] {
    override def createAccumulator() = Map.empty

    override def add(sensor: SensorData, acc: Map[Int, Int]) = {
      val stateCount = acc.getOrElse(sensor.measurements.state, 0)
      val newCount   = stateCount + 1
      logger.warn(s"add: ${sensor.deviceId} state ${sensor.measurements.state} count $newCount")

      acc.updated(sensor.measurements.state, newCount)
    }

    override def getResult(accumulator: Map[Int, Int]) = {
      logger.warn(s"getResult: $accumulator")
      accumulator
    }

    override def merge(a: Map[Int, Int], b: Map[Int, Int]) = {
      logger.warn(s"merge")
      b.foldLeft(a) {
        case (acc, (k, v)) =>
          val av = a.getOrElse(k, 0)
          acc updated (k,(av + v))
      }
    }
  }

  override protected def createLogic() = new FlinkStreamletLogic {

    override def buildExecutionGraph = {

      val ds: WindowedStream[SensorData, UUID, GlobalWindow] =
        readStream(in).
          keyBy(sd => sd.deviceId).
          countWindow(10,5)

      ds.aggregate(new SumAggregate).map{
        stateMap =>
          // Check for missing states indicating invalid state transition
          val minState = stateMap.keys.min
          val maxState = stateMap.keys.max
          val allStates = stateMap.keys.toSet
          val valid = (minState to maxState).forall {
            state =>
              allStates.contains(state)
          }
          if(!valid) {
            logger.error(s"Missing state! $allStates")
          }
          valid
      }

      // Note this a more convenient way to fold over data but it is deprecated and should be done using windows of data
      // as above...
      // ds.fold(Map.empty[Int, Int]) {
      //     case (acc, sensor) => {
      //       val stateCount = acc.getOrElse(sensor.measurements.state, 0)
      //       val newCount   = stateCount + 1
      //       logger.warn(s"State aggregate for ${sensor.deviceId} state ${sensor.measurements.state} count $newCount")

      //       acc.updated(sensor.measurements.state, newCount)
      //     }
      //   }

    }
  }

}
