name := "slick-utils"

organization := "mrks"

version := "0.1"

scalaVersion := "2.12.6"

licenses += ("Apache-2.0", url("https://github.com/markussammallahti/slick-utils/blob/master/LICENSE"))

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.+" % Provided
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.h2database" % "h2" % "1.4.192" % Test,
  "com.typesafe.play" %% "play-jdbc" % "2.6.+" % Test,
  "org.slf4j" % "slf4j-nop" % "1.7.+" % Test
)
