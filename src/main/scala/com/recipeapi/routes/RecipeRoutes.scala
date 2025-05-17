package com.recipeapi.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.recipeapi.models.{Recipe, RecipeRequest}
import com.recipeapi.repositories.RecipeRepository
import com.recipeapi.json.JsonFormats
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RecipeRoutes(repository: RecipeRepository)(implicit ec: ExecutionContext) extends JsonFormats {
  
  val routes: Route = {
    pathPrefix("recipes") {
      pathEnd {
        post {
          entity(as[RecipeRequest]) { recipeRequest =>
            validateRecipeRequest(recipeRequest) match {
              case Some(errorMsg) => {
                println("Recipe creation error!")
                println(errorMsg)
                complete(StatusCodes.BadRequest -> JsObject(
                  "message" -> JsString("Recipe creation failed!"),
                  "required" -> JsString(errorMsg)
                ).prettyPrint)
              }
              case None =>
                onComplete(repository.create(recipeRequest)) {
                  case Success(recipe) =>{
                    println("Recipe successfully created!")
                    println(Vector(recipe.toJson))
                    complete(StatusCodes.OK -> JsObject(
                      "message" -> JsString("Recipe successfully created!"),
                      "recipe" -> JsArray(Vector(recipe.toJson))
                    ).prettyPrint)
                  }
                  case Failure(ex) =>{
                    println("Recipe creation failed!")
                    println(ex.toString())
                    complete(StatusCodes.InternalServerError -> JsObject(
                      "message" -> JsString("Recipe creation failed!")
                    ).prettyPrint)
                  }
                }
            }
          }
        } ~
        get {
          println(repository)
          onComplete(repository.getAll()) {
            case Success(recipes) =>{
              println(recipes.toJson)
              complete(StatusCodes.OK -> JsObject(
                "recipes" -> recipes.toJson
              ).prettyPrint)
            }
            case Failure(ex) =>{
              println("Error retrieving recipes")
              println(ex)
              complete(StatusCodes.InternalServerError -> "Error retrieving recipes")
            }
          }
        }
      } ~
      path(IntNumber) { id =>
        get {
          onComplete(repository.getById(id)) {
            case Success(Some(recipe)) =>
              complete(StatusCodes.OK -> JsObject(
                "message" -> JsString("Recipe details by id"),
                "recipe" -> JsArray(Vector(recipe.toJson))
              ).prettyPrint)
            case Success(None) =>
              complete(StatusCodes.NotFound -> JsObject(
                "message" -> JsString("No recipe found")
              ).prettyPrint)
            case Failure(ex) =>
              complete(StatusCodes.InternalServerError -> "Error retrieving recipe")
          }
        } ~
        patch {
          entity(as[RecipeRequest]) { recipeRequest =>
            onComplete(repository.update(id, recipeRequest)) {
              case Success(Some(recipe)) =>
                complete(StatusCodes.OK -> JsObject(
                  "message" -> JsString("Recipe successfully updated!"),
                  "recipe" -> JsArray(Vector(recipe.toJson))
                ).prettyPrint)
              case Success(None) =>
                complete(StatusCodes.NotFound -> JsObject(
                  "message" -> JsString("No recipe found")
                ).prettyPrint)
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> "Error updating recipe")
            }
          }
        } ~
        delete {
          onComplete(repository.delete(id)) {
            case Success(true) =>
              complete(StatusCodes.OK -> JsObject(
                "message" -> JsString("Recipe successfully removed!")
              ).prettyPrint)
            case Success(false) =>
              complete(StatusCodes.NotFound -> JsObject(
                "message" -> JsString("No recipe found")
              ).prettyPrint)
            case Failure(ex) =>
              complete(StatusCodes.InternalServerError -> "Error deleting recipe")
          }
        }
      }
    }
  }
  
  private def validateRecipeRequest(request: RecipeRequest): Option[String] = {
    val requiredFields = List(
      ("title", request.title),
      ("making_time", request.making_time),
      ("serves", request.serves),
      ("ingredients", request.ingredients),
      ("cost", request.cost.toString())
    )
    
    val missingFields = requiredFields
      .filter { case (_, value) => value.trim.isEmpty }
      .map { case (field, _) => field }
      
    if (missingFields.isEmpty) None
    else Some(missingFields.mkString(", "))
  }
}
