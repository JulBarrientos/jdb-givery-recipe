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

   val dbUrl = sys.env.get("DATABASE_URL") match {
    case Some(url) if url.startsWith("postgres://") => 
      // Convertir formato postgres:// a jdbc:postgresql://
      val jdbcUrl = s"jdbc:postgresql${url.substring(8)}"
      println(s"Converted URL: $jdbcUrl")
      jdbcUrl
    case Some(url) => url
    case None => 
      // Construir a partir de componentes
      val host = sys.env.getOrElse("PGHOST", "localhost")
      val port = sys.env.getOrElse("PGPORT", "5432")
      val db = sys.env.getOrElse("PGDATABASE", "postgres")
      s"jdbc:postgresql://$host:$port/$db"
  }

  val dbUser = sys.env.getOrElse("PGUSER", "postgres")
  val dbPassword = sys.env.getOrElse("PGPASSWORD", "postgres")
  
  val serverHost = sys.env.getOrElse("HOST", "0.0.0.0")
  val serverPort = sys.env.getOrElse("PORT", "8080").toInt
  
  // Debug info
  println(s"DB URL: $dbUrl")
  println(s"DB User: $dbUser")
  println(s"Server running on: $serverHost:$serverPort")
}