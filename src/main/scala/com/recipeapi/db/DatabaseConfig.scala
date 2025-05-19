package com.recipeapi.db

import slick.jdbc.JdbcProfile
import com.recipeapi.config.Config

object DatabaseConfig {
  // Set up profile from Config
  val profile: JdbcProfile = Config.profile
  
  // DB setUp
  import profile.api._
  
  val db = Database.forURL(
    url = Config.dbUrl,
    user = Config.dbUser,
    password = Config.dbPassword,
    driver = Config.dbDriver
  )
}
