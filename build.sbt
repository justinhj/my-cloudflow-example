import sbt._
import sbt.Keys._

  val Akka          = "2.6.6"
  val AkkaHttp      = "10.1.12"
  val AkkaMgmt      = "1.0.8"
  val AlpakkaKafka  = "2.0.3"
  val Scala         = "2.12.11"
  val Spark         = "2.4.5"
  val Flink         = "1.10.0"
  val EmbeddedKafka = "2.5.0"
  // skuber depends on 2.5.29
  val AkkaOperator  = "2.5.29"

lazy val flinkDemo = (project in file ("flinkDemo"))
    .enablePlugins(CloudflowApplicationPlugin, CloudflowFlinkPlugin)
    .dependsOn(sensorData)


//tag::local-conf[]
lazy val sensorData =  (project in file("."))
    .enablePlugins(CloudflowApplicationPlugin, CloudflowAkkaPlugin, ScalafmtPlugin)
    //.dependsOn(flinkDemo)
    .settings(
      scalaVersion := "2.12.11",
      runLocalConfigFile := Some("src/main/resources/local.conf"),
      scalafmtOnCompile := true,
      name := "sensor-data-scala",
//end::local-conf[]

      libraryDependencies ++= Seq(
        //"com.lightbend.akka"     %% "akka-stream-alpakka-file"  % AlpakkaKafka, // "1.1.2",
        "com.typesafe.akka"      %% "akka-http-spray-json"      % AkkaHttp, // "10.1.12",
        "ch.qos.logback"         %  "logback-classic"           % "1.2.3",
        "com.typesafe.akka"      %% "akka-http-testkit"         % "10.1.12" % "test",
        "org.scalatest"          %% "scalatest"                 % "3.0.8"  % "test",
        "com.typesafe.akka"      %% "akka-protobuf"             % Akka,
        "com.typesafe.akka"      %% "akka-actor"                % Akka,
        "com.typesafe.akka"      %% "akka-stream"               % Akka,
        "com.typesafe.akka"      %% "akka-slf4j"                % Akka
      ),
      organization := "com.lightbend.cloudflow",
      headerLicense := Some(HeaderLicense.ALv2("(C) 2016-2020", "Lightbend Inc. <https://www.lightbend.com>")),

      crossScalaVersions := Vector(scalaVersion.value),
      scalacOptions ++= Seq(
        "-encoding", "UTF-8",
        "-target:jvm-1.8",
        "-Xlog-reflective-calls",
        "-Xlint",
        "-Ywarn-unused",
        "-Ywarn-unused-import",
        "-deprecation",
        "-feature",
        "-language:_",
        "-unchecked"
      ),


      scalacOptions in (Compile, console) --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
      scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value

    )
