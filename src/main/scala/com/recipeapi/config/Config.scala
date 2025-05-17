package com.recipeapi.config

import com.typesafe.config.ConfigFactory
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile
import slick.jdbc.H2Profile

object Config {
  val config = ConfigFactory.load()
  private val env = "railway"

  println(s"Ambiente: $env")
  val profile: JdbcProfile = env match {
    case "prod" | "railway" => PostgresProfile
    case _ => H2Profile
  }

  val dbConfig = env match {
    case "prod" => config.getConfig("docker")
    case "railway" => config.getConfig("railway")
    case _ => config.getConfig("local")
  }
  
  val dbUrl = sys.env.getOrElse("DATABASE_URL", dbConfig.getString("url"))
  val dbUser = sys.env.getOrElse("PGUSER", dbConfig.getString("user"))
  val dbPassword = sys.env.getOrElse("PGPASSWORD", dbConfig.getString("password"))
  
  val serverHost = config.getConfig("server").getString("host")
  val serverPort = config.getConfig("server").getInt("port")
}