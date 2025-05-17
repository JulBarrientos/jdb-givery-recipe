package com.recipeapi.db

import slick.jdbc.H2Profile.api._
import com.recipeapi.models.Recipe
import java.time.LocalDateTime
import slick.jdbc.{JdbcProfile, PostgresProfile}


class RecipeTable(tag: Tag) extends Table[Recipe](tag, "recipes") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def making_time = column[String]("making_time")
  def serves = column[String]("serves")
  def ingredients = column[String]("ingredients")
  def cost = column[Int]("cost")
  def created_at = column[LocalDateTime]("created_at")
  def updated_at = column[LocalDateTime]("updated_at")
  
  def * = (id.?, title, making_time, serves, ingredients, cost, created_at.?, updated_at.?) <> (Recipe.tupled, Recipe.unapply)
}