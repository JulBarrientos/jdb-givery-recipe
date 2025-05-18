
package com.recipeapi.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{HttpOrigin, HttpOriginRange, Origin, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.model.HttpMethods._
import com.recipeapi.models.{Recipe, RecipeRequest}
import com.recipeapi.repositories.RecipeRepository
import com.recipeapi.json.JsonFormats
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RecipeRoutes(repository: RecipeRepository)(implicit ec: ExecutionContext) extends JsonFormats {
  
  // Definir los CORS headers explícitamente
  private def corsHeaders = List(
    `Access-Control-Allow-Origin`(HttpOriginRange.`*`),
    `Access-Control-Allow-Methods`(GET, POST, PUT, DELETE, OPTIONS, PATCH),
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Authorization")
  )
  
  // Manejar las solicitudes OPTIONS para CORS
  private def corsHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).withHeaders(corsHeaders))
  }
  
  // Envolver todas las rutas con corsHandler para manejar CORS correctamente
  val routes: Route = corsHandler ~ {
    pathPrefix("recipes") {
      extractRequest { request =>
        println(s"Received ${request.method.name} request to URI: ${request.uri}")
        println(s"Headers: ${request.headers.mkString(", ")}")
        
        pathEnd {
          // Manejar POST primero, antes que GET para asegurarnos que se evalúe correctamente
          post {
            entity(as[String]) { requestBody =>
              println(s"POST body: $requestBody")
              
              // Intentar parsear el JSON recibido
              try {
                val recipeRequest = requestBody.parseJson.convertTo[RecipeRequest]
                println(s"Parsed recipe request: $recipeRequest")
                
                validateRecipeRequest(recipeRequest) match {
                  case Some(errorMsg) => {
                    println(s"Recipe creation validation error: $errorMsg")
                    complete(StatusCodes.BadRequest -> JsObject(
                      "message" -> JsString("Recipe creation failed!"),
                      "required" -> JsString(errorMsg)
                    ).prettyPrint)
                  }
                  case None =>
                    onComplete(repository.create(recipeRequest)) {
                      case Success(recipe) => {
                        println(s"Recipe successfully created with ID: ${recipe.id}")
                        complete(StatusCodes.OK -> JsObject(
                          "message" -> JsString("Recipe successfully created!"),
                          "recipe" -> JsArray(Vector(recipe.toJson))
                        ).prettyPrint)
                      }
                      case Failure(ex) => {
                        println(s"Recipe creation failed with exception: ${ex.getMessage}")
                        complete(StatusCodes.InternalServerError -> JsObject(
                          "message" -> JsString("Recipe creation failed!")
                        ).prettyPrint)
                      }
                    }
                }
              } catch {
                case ex: Exception =>
                  println(s"Error parsing request JSON: ${ex.getMessage}")
                  complete(StatusCodes.BadRequest -> JsObject(
                    "message" -> JsString(s"Invalid JSON format: ${ex.getMessage}")
                  ).prettyPrint)
              }
            }
          } ~
          // GET después de POST
          get {
            println("Processing GET request to /recipes")
            onComplete(repository.getAll()) {
              case Success(recipes) => {
                println(s"Retrieved ${recipes.size} recipes")
                complete(StatusCodes.OK -> JsObject(
                  "recipes" -> recipes.toJson
                ).prettyPrint)
              }
              case Failure(ex) => {
                println(s"Error retrieving recipes: ${ex.getMessage}")
                complete(StatusCodes.InternalServerError -> "Error retrieving recipes")
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
                complete(StatusCodes.OK -> JsObject(
                  "message" -> JsString("Recipe details by id"),
                  "recipe" -> JsArray(Vector(recipe.toJson))
                ).prettyPrint)
              case Success(None) =>
                println(s"No recipe found with ID: $id")
                complete(StatusCodes.NotFound -> JsObject(
                  "message" -> JsString("No recipe found")
                ).prettyPrint)
              case Failure(ex) =>
                println(s"Error retrieving recipe ID $id: ${ex.getMessage}")
                complete(StatusCodes.InternalServerError -> "Error retrieving recipe")
            }
          } ~
          patch {
            println(s"PATCH request for recipe ID: $id")
            entity(as[RecipeRequest]) { recipeRequest =>
              println(s"Update data for recipe ID $id: ${recipeRequest.toJson}")
              onComplete(repository.update(id, recipeRequest)) {
                case Success(Some(recipe)) =>
                  println(s"Recipe ID $id successfully updated")
                  complete(StatusCodes.OK -> JsObject(
                    "message" -> JsString("Recipe successfully updated!"),
                    "recipe" -> JsArray(Vector(recipe.toJson))
                  ).prettyPrint)
                case Success(None) =>
                  println(s"No recipe found with ID: $id for update")
                  complete(StatusCodes.NotFound -> JsObject(
                    "message" -> JsString("No recipe found")
                  ).prettyPrint)
                case Failure(ex) =>
                  println(s"Error updating recipe ID $id: ${ex.getMessage}")
                  complete(StatusCodes.InternalServerError -> "Error updating recipe")
              }
            }
          } ~
          delete {
            println(s"DELETE request for recipe ID: $id")
            onComplete(repository.delete(id)) {
              case Success(true) =>
                println(s"Recipe ID $id successfully deleted")
                complete(StatusCodes.OK -> JsObject(
                  "message" -> JsString("Recipe successfully removed!")
                ).prettyPrint)
              case Success(false) =>
                println(s"No recipe found with ID: $id for deletion")
                complete(StatusCodes.NotFound -> JsObject(
                  "message" -> JsString("No recipe found")
                ).prettyPrint)
              case Failure(ex) =>
                println(s"Error deleting recipe ID $id: ${ex.getMessage}")
                complete(StatusCodes.InternalServerError -> "Error deleting recipe")
            }
          }
        }
      }
    } ~
    // Para capturar posibles rutas incorrectas
    path("recipe") {
      extractRequest { request =>
        println(s"Warning: Request to incorrect path 'recipe' (singular): ${request.method.name} ${request.uri}")
        complete(StatusCodes.NotFound -> JsObject(
          "message" -> JsString("Did you mean to use /recipes (plural) instead of /recipe (singular)?")
        ).prettyPrint)
      }
    } ~
    // Ruta alternativa para POST si hay problemas con la ruta principal
    path("create-recipe") {
      post {
        entity(as[RecipeRequest]) { recipeRequest =>
          println(s"POST request to alternative path /create-recipe")
          validateRecipeRequest(recipeRequest) match {
            case Some(errorMsg) => {
              println(s"Recipe creation validation error: $errorMsg")
              complete(StatusCodes.BadRequest -> JsObject(
                "message" -> JsString("Recipe creation failed!"),
                "required" -> JsString(errorMsg)
              ).prettyPrint)
            }
            case None =>
              onComplete(repository.create(recipeRequest)) {
                case Success(recipe) => {
                  println(s"Recipe successfully created with ID: ${recipe.id}")
                  complete(StatusCodes.OK -> JsObject(
                    "message" -> JsString("Recipe successfully created!"),
                    "recipe" -> JsArray(Vector(recipe.toJson))
                  ).prettyPrint)
                }
                case Failure(ex) => {
                  println(s"Recipe creation failed with exception: ${ex.getMessage}")
                  complete(StatusCodes.InternalServerError -> JsObject(
                    "message" -> JsString("Recipe creation failed!")
                  ).prettyPrint)
                }
              }
          }
        }
      }
    }
  }
  
  private def validateRecipeRequest(request: RecipeRequest): Option[String] = {
    println(s"Validating recipe request: $request")
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
      
    if (missingFields.isEmpty) {
      println("Recipe validation successful")
      None
    } else {
      println(s"Recipe validation failed. Missing fields: ${missingFields.mkString(", ")}")
      Some(missingFields.mkString(", "))
    }
  }
}