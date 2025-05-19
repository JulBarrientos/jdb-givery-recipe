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
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.Http

class RecipeRoutes(repository: RecipeRepository)(implicit ec: ExecutionContext) extends JsonFormats {
  
  val routes: Route = {
    logRequest("Request") { // Log todas las solicitudes entrantes
      pathPrefix("recipes") {
        println(s"Received request to path: /recipes")
        pathEnd {
          post {
            println("POST request to /recipes endpoint received")
            entity(as[RecipeRequest]) { recipeRequest =>
              println(s"Recipe request data: ${recipeRequest.toJson}")
              validateRecipeRequest(recipeRequest) match {
                case Some(errorMsg) => {
                  println(s"Recipe creation error: $errorMsg")
                  complete(
                    HttpResponse(
                      status =StatusCodes.BadRequest,
                      entity = HttpEntity(
                        ContentTypes.`application/json`, JsObject (
                          "message" -> JsString("Recipe creation failed!"),
                          "required" -> JsString(errorMsg)
                        ).prettyPrint
                      )
                    )
                  )
                }
                case None =>
                  onComplete(repository.create(recipeRequest)) {
                    case Success(recipe) =>{
                      println(s"Recipe successfully created with ID: ${recipe.id}")
                      complete(
                        HttpResponse(
                          status = StatusCodes.OK,
                          entity = HttpEntity(
                            ContentTypes.`application/json`, JsObject (
                              "message" -> JsString("Recipe successfully created!"),
                              "recipe" -> JsArray(Vector(recipe.toJson))
                            ).prettyPrint
                          )
                        )
                      )
                    }
                    case Failure(ex) =>{
                      println(s"Recipe creation failed with exception: ${ex.getMessage}")
                      complete(
                        HttpResponse(
                          status = StatusCodes.InternalServerError,
                          entity = HttpEntity(
                            ContentTypes.`application/json`, JsObject (
                              "message" -> JsString("Recipe creation failed!")
                            ).prettyPrint
                          )
                        )
                      )
                    }
                  }
              }
            } ~{
              println(s"Recipe creation failed! RecipeRequest not complete")
              complete(
                HttpResponse(
                  status = StatusCodes.OK,
                  entity = HttpEntity(
                    ContentTypes.`application/json`, JsObject(
                      "message" -> JsString("Recipe creation failed!")
                    ).prettyPrint
                  )
                )
              )
            }
          } ~
          get {
            println("GET request to /recipes endpoint received")
            onComplete(repository.getAll()) {
              case Success(recipes) =>{
                println(s"Retrieved ${recipes.size} recipes")
                complete(
                  HttpResponse(
                    status = StatusCodes.OK, 
                    entity = HttpEntity( 
                      ContentTypes.`application/json`, JsObject(
                        "recipes" -> recipes.toJson
                      ).prettyPrint
                    )
                  )
                )
              }
              case Failure(ex) =>{
                println(s"Error retrieving recipes: ${ex.getMessage}")
                complete(
                  HttpResponse(
                    status = StatusCodes.InternalServerError,
                    entity = HttpEntity( 
                      ContentTypes.`application/json`, JsObject( 
                        "message" -> JsString("Error retrieving recipes")
                      ).prettyPrint
                    )
                  )
                )
              }
            }
          }
        } ~
        path(IntNumber) { id =>
          println(s"Request for recipe with ID: $id")
          get {
            println(s"GET request for recipe ID: $id")
            onComplete(repository.getById(id)) {
              case Success(Some(recipe)) =>
                println(s"Recipe found with ID: $id")
                complete(
                  HttpResponse(
                    status = StatusCodes.OK,
                    entity = HttpEntity(
                      ContentTypes.`application/json`, JsObject(
                        "message" -> JsString("Recipe details by id"),
                        "recipe" -> JsArray(Vector(recipe.toJson))
                      ).prettyPrint
                    )
                  )
                )
              case Success(None) =>
                println(s"No recipe found with ID: $id")
                complete(
                  HttpResponse(
                    status = StatusCodes.NotFound,
                    entity = HttpEntity( 
                      ContentTypes.`application/json`, JsObject(
                        "message" -> JsString("No recipe found")
                      ).prettyPrint
                    )
                  )
                )
              case Failure(ex) =>
                println(s"Error retrieving recipe ID $id: ${ex.getMessage}")
                complete(
                  HttpResponse(
                    status = StatusCodes.InternalServerError, 
                    entity = HttpEntity(
                      ContentTypes.`application/json`, JsObject(
                        "message" -> JsString("Error retrieving recipe")
                      ).prettyPrint
                    )
                  )
                )
            }
          } ~
          patch {
            println(s"PATCH request for recipe ID: $id")
            entity(as[RecipeRequest]) { recipeRequest =>
              println(s"Update data for recipe ID $id: ${recipeRequest.toJson}")
              onComplete(repository.update(id, recipeRequest)) {
                case Success(Some(recipe)) =>
                  println(s"Recipe ID $id successfully updated")
                  complete(
                    HttpResponse(
                        status = StatusCodes.OK,
                        entity = HttpEntity( 
                          ContentTypes.`application/json`, JsObject(
                            "message" -> JsString("Recipe successfully updated!"),
                            "recipe" -> JsArray(Vector(recipe.toJson))
                          ).prettyPrint
                        )
                    )
                  )
                case Success(None) => {
                  println(s"No recipe found with ID: $id for update")
                  complete(
                    HttpResponse(
                      status = StatusCodes.NotFound,
                      entity = HttpEntity( 
                        ContentTypes.`application/json`, JsObject(
                          "message" -> JsString("No recipe found")
                        ).prettyPrint
                      )
                    )
                  )
                }
                case Failure(ex) => {
                  println(s"Error updating recipe ID $id: ${ex.getMessage}")
                  complete(
                    HttpResponse(
                      status = StatusCodes.InternalServerError,
                      entity = HttpEntity(
                        ContentTypes.`application/json`, JsObject(
                          "message" -> JsString("Error updating recipe")
                        ).prettyPrint
                      )
                    )
                  )
                }
              }
            }
          } ~
          delete {
            println(s"DELETE request for recipe ID: $id")
            onComplete(repository.delete(id)) {
              case Success(true) =>
                println(s"Recipe ID $id successfully deleted")
                complete(
                  HttpResponse(
                    status = StatusCodes.OK,
                    entity = HttpEntity(
                      ContentTypes.`application/json`, JsObject(
                        "message" -> JsString("Recipe successfully removed!")
                      ).prettyPrint
                    )
                  )
                )
              case Success(false) =>
                println(s"No recipe found with ID: $id for deletion")
                complete(
                  HttpResponse(
                    status = StatusCodes.NotFound,
                    entity = HttpEntity(
                        ContentTypes.`application/json`, JsObject(
                          "message" -> JsString("No recipe found")
                        ).prettyPrint
                      )
                  )
                )
              case Failure(ex) =>
                println(s"Error deleting recipe ID $id: ${ex.getMessage}")
                complete(
                  HttpResponse( 
                    status = StatusCodes.InternalServerError, 
                    entity = HttpEntity(
                      ContentTypes.`application/json`, JsObject(
                        "message" -> JsString("Error deleting recipe")
                      ).prettyPrint
                    )
                  )
                )
            }
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
    else {
      println(s"Missing fields: $missingFields")  
      Some(missingFields.mkString(", "))
    }
  }
}
