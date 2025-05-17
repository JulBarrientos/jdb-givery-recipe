name := "recipe-api"
version := "1.0"
scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  // Akka HTTP
  "com.typesafe.akka" %% "akka-actor-typed" % "2.7.0",
  "com.typesafe.akka" %% "akka-stream" % "2.7.0",
  "com.typesafe.akka" %% "akka-http" % "10.5.0",
  
  // JSON serialization
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
  
  // Database access
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "org.postgresql" % "postgresql" % "42.3.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "com.github.tminglei" %% "slick-pg" % "0.20.3",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.20.3",
  "com.h2database" % "h2" % "2.1.212",

  // Configuration
  "com.typesafe" % "config" % "1.4.2",
  
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.4.5"
)