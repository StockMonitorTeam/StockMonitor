name := "StockCostChangeNotifier"
version := "0.1"
scalaVersion := "2.12.5"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "com.typesafe.akka" %% "akka-http" % "10.1.0",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.0" % Test,
  "info.mukel" %% "telegrambot4s" % "3.0.14",
  "org.slf4j" % "slf4j-jdk14" % "1.7.5",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test)

//"sbt assembly" to make jar
mainClass in assembly := Some("stockmonitoringbot.Main")
assemblyJarName in assembly := "StockMonitor.jar"
scriptClasspath := Seq((assemblyJarName in assembly).value)