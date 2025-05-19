package com.recipeapi.config

import com.typesafe.config.ConfigFactory
import slick.jdbc.{JdbcProfile, PostgresProfile, H2Profile}

object Config {
  
  private val env = sys.env.getOrElse("APP_ENV", "local")
  private val configDb = ConfigFactory.load().getConfig(env)


  val profile: JdbcProfile = env match {
    case "local" => H2Profile
    case _ => PostgresProfile
  }
  val dbUrl = configDb.getString("url")
  val dbUser = sys.env.getOrElse("PGUSER", configDb.getString("user"))
  val dbPassword = sys.env.getOrElse("PGPASSWORD", configDb.getString("password"))
  val dbDriver = configDb.getString("driver")
  val serverConfig = ConfigFactory.load().getConfig("server")
  val serverHost = sys.env.getOrElse("HOST", serverConfig.getString("host"))
  val serverPort = sys.env.getOrElse("PORT", serverConfig.getString("port")).toInt
  
  // Debug info
  println(s"Ambiente: $env")
  println(s"DB URL: $dbUrl")
  println(s"DB User: $dbUser")
  println(s"Server running on: $serverHost:$serverPort")
}