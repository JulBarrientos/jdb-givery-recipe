package com.recipeapi.repositories

import com.recipeapi.db.{DatabaseConfig, RecipeTable}

import com.recipeapi.models.{Recipe, RecipeRequest}
import slick.jdbc.H2Profile.api._
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class RecipeRepository(implicit ec: ExecutionContext) {
  import DatabaseConfig.db
  
  val recipes = TableQuery[RecipeTable]
  
  // Inicialización de la tabla
  def init(): Future[Unit] = {
    db.run(recipes.schema.createIfNotExists)
  }
  
  // Crear una receta
  def create(recipeRequest: RecipeRequest): Future[Recipe] = {
    val now = LocalDateTime.now()
    val recipe = Recipe(
      id = None,
      title = recipeRequest.title,
      making_time = recipeRequest.making_time,
      serves = recipeRequest.serves,
      ingredients = recipeRequest.ingredients,
      cost = recipeRequest.cost,
      created_at = Some(now),
      updated_at = Some(now)
    )
    
    val insertQuery = (recipes returning recipes.map(_.id)
      into ((recipe, id) => recipe.copy(id = Some(id)))
    ) += recipe
    
    db.run(insertQuery)
  }
  
  // Obtener todas las recetas
  def getAll(): Future[Seq[Recipe]] = {
    db.run(recipes.result)
  }
  
  // Obtener una receta por ID
  def getById(id: Int): Future[Option[Recipe]] = {
    db.run(recipes.filter(_.id === id).result.headOption)
  }
  
  // Actualizar una receta
  def update(id: Int, recipeRequest: RecipeRequest): Future[Option[Recipe]] = {
    val now = LocalDateTime.now()
    val query = for {
      recipe <- recipes if recipe.id === id
    } yield (recipe.title, recipe.making_time, recipe.serves, recipe.ingredients, recipe.cost, recipe.updated_at)
    
    val updateAction = query.update(
      recipeRequest.title,
      recipeRequest.making_time,
      recipeRequest.serves,
      recipeRequest.ingredients,
      recipeRequest.cost,
      now
    )
    
    db.run(updateAction).flatMap { rowsAffected =>
      if (rowsAffected > 0) getById(id)
      else Future.successful(None)
    }
  }
  
  // Eliminar una receta
  def delete(id: Int): Future[Boolean] = {
    val query = recipes.filter(_.id === id)
    
    db.run(query.delete).map { rowsAffected =>
      rowsAffected > 0
    }
  }
}
