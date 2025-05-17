package com.recipeapi.models

import java.time.LocalDateTime
import spray.json._

case class Recipe(
  id: Option[Int] = None,
  title: String,
  making_time: String,
  serves: String,
  ingredients: String,
  cost: Int,
  created_at: Option[LocalDateTime] = None,
  updated_at: Option[LocalDateTime] = None
)
