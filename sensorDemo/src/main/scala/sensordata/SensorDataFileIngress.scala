package sensordata

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import cloudflow.akkastream._
import cloudflow.akkastream.util.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import SensorDataJsonSupport._
import akka.stream.scaladsl._
import scala.concurrent.duration._
import spray.json._
import org.slf4j.LoggerFactory

class SensorDataFileIngress extends AkkaServerStreamlet {
  val out   = AvroOutlet[SensorData]("out").withPartitioner(RoundRobinPartitioner)
  def shape = StreamletShape.withOutlets(out)

  val logger = LoggerFactory.getLogger("SensorDataFileIngress")

  override def createLogic = new AkkaStreamletLogic() {
    val throttleElements = 2
    println(s"Throttling sensor data to $throttleElements/s")
    override def run() = {
      val fares = readSensorData()
      Source(fares)
        .throttle(throttleElements, 1.seconds)
        .to(plainSink(out))
        .run
    }
  }

  private def readSensorData() = {
    val inFares = this.getClass.getResourceAsStream("/sensorData.json")
    // 'fixing' JSON issues in input document
    val str  = scala.io.Source.fromInputStream(inFares).mkString.parseJson
    val data = str.convertTo[List[SensorData]]
    logger.warn(s"Read ${data.size} sensor data rows")
    data
  }
}
