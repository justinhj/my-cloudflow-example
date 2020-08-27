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

class SensorDataFileIngress extends AkkaServerStreamlet {
  val out   = AvroOutlet[SensorData]("out").withPartitioner(RoundRobinPartitioner)
  def shape = StreamletShape.withOutlets(out)

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
    val str       = scala.io.Source.fromInputStream(inFares).mkString
    val faresJson = s"[$str]".replaceAll("\n", ",\n").parseJson
    val fares     = faresJson.convertTo[List[SensorData]]
    println(s"Read ${fares.size} fares")
    fares
  }
}
