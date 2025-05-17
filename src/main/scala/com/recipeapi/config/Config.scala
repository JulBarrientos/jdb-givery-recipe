package com.recipeapi.config

import com.typesafe.config.ConfigFactory
import slick.jdbc.{JdbcProfile, PostgresProfile, H2Profile}

object Config {
  val config = ConfigFactory.load()
  private val env = sys.env.getOrElse("APP_ENV", "local")

  println(s"Ambiente: $env")

  val profile: JdbcProfile = env match {
    case "local" => H2Profile
    case _ => PostgresProfile
  }

  println(s"Usando perfil: ${profile.getClass.getName}")

  val dbUrl = "jdbc:" + sys.env.get("DATABASE_URL") 

  val dbUser = sys.env.getOrElse("PGUSER", "postgres")
  val dbPassword = sys.env.getOrElse("PGPASSWORD", "postgres")
  
  val serverHost = sys.env.getOrElse("HOST", "8080")
  val serverPort = sys.env.getOrElse("PORT", "8080").toInt
  
  // Debug info
  println(s"DB URL: $dbUrl")
  println(s"DB User: $dbUser")
  println(s"Server running on: $serverHost:$serverPort")
}