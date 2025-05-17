package com.recipeapi.models

case class RecipeRequest(
  title: String,
  making_time: String,
  serves: String,
  ingredients: String,
  cost: Int
)
