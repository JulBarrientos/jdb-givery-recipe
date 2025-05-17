package com.recipeapi.db

import slick.jdbc.JdbcProfile
import com.recipeapi.config.Config

object DatabaseConfig {
  // Usar el perfil desde Config
  val profile: JdbcProfile = Config.profile
  
  // Configurar la base de datos
  import profile.api._
  
  val db = Database.forURL(
    url = Config.dbUrl,
    user = Config.dbUser,
    password = Config.dbPassword,
    driver = "org.postgresql.Driver"
  )
}
