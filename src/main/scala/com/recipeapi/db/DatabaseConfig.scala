package com.recipeapi.db

import slick.jdbc.H2Profile.api._
import com.recipeapi.config.Config

object DatabaseConfig {
  /*val db = Database.forURL(
    url = Config.dbUrl,
    user = Config.dbUser,
    password = Config.dbPassword,
    driver = "org.h2.Driver"
  )*/
  val db = Config.profile.api.Database.forConfig("", Config.dbConfig)
}