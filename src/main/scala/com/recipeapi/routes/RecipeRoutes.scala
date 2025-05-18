package com.recipeapi.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{`Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.model.HttpMethods._
import com.recipeapi.models.{Recipe, RecipeRequest}
import com.recipeapi.repositories.RecipeRepository
import com.recipeapi.json.JsonFormats
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RecipeRoutes(repository: RecipeRepository)(implicit ec: ExecutionContext) extends JsonFormats {
  
  // Define CORS headers
  private val corsHeaders = List(
    `Access-Control-Allow-Origin`.`*`,
    `Access-Control-Allow-Methods`(GET, POST, PUT, DELETE, OPTIONS, PATCH),
    `Access-Control-Allow-Headers`("Content-Type", "Authorization")
  )
  
  // Función para añadir CORS headers a cualquier respuesta
  private def addCorsHeaders(response: HttpResponse): HttpResponse = {
    response.withHeaders(corsHeaders)
  }
  
  // Handler for OPTIONS requests
  private val corsHandler: Route = options {
    complete(addCorsHeaders(HttpResponse(StatusCodes.OK)))
  }
  
  // Rutas completamente separadas por método HTTP
  val routes: Route = corsHandler ~ {
    // Separamos completamente las rutas por método HTTP para evitar confusiones
    // ÚNICA RUTA POST
    post{ path("/recipes") {
      extractRequest { request =>
        println(s"Received POST request to /recipes")
        println(s"Headers: ${request.headers.mkString(", ")}")
        
        entity(as[String]) { requestBody =>
          println(s"POST body raw: $requestBody")
          
          try {
            val recipeRequest = requestBody.parseJson.convertTo[RecipeRequest]
            println(s"Parsed recipe request: $recipeRequest")
            
            validateRecipeRequest(recipeRequest) match {
              case Some(errorMsg) => {
                println(s"Recipe creation validation error: $errorMsg")
                complete(addCorsHeaders(HttpResponse(
                  status = StatusCodes.BadRequest,
                  entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                    "message" -> JsString("Recipe creation failed!"),
                    "required" -> JsString(errorMsg)
                  ).prettyPrint)
                )))
              }
              case None =>
                onComplete(repository.create(recipeRequest)) {
                  case Success(recipe) => {
                    println(s"Recipe successfully created with ID: ${recipe.id}")
                    complete(addCorsHeaders(HttpResponse(
                      status = StatusCodes.OK,
                      entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                        "message" -> JsString("Recipe successfully created!"),
                        "recipe" -> JsArray(Vector(recipe.toJson))
                      ).prettyPrint)
                    )))
                  }
                  case Failure(ex) => {
                    println(s"Recipe creation failed with exception: ${ex.getMessage}")
                    complete(addCorsHeaders(HttpResponse(
                      status = StatusCodes.InternalServerError,
                      entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                        "message" -> JsString("Recipe creation failed!")
                      ).prettyPrint)
                    )))
                  }
                }
            }
          } catch {
            case ex: Exception =>
              println(s"Error parsing request JSON: ${ex.getMessage}")
              complete(addCorsHeaders(HttpResponse(
                status = StatusCodes.BadRequest,
                entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                  "message" -> JsString(s"Invalid JSON format: ${ex.getMessage}")
                ).prettyPrint)
              )))
          }
        }
      }
    }} ~
    // ÚNICA RUTA GET - TODOS LOS RECIPES
    (path("api" / "list") & get) {
      extractRequest { request =>
        println(s"Received GET request to /api/list")
        println(s"Headers: ${request.headers.mkString(", ")}")
        
        onComplete(repository.getAll()) {
          case Success(recipes) => {
            println(s"Retrieved ${recipes.size} recipes")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "recipes" -> recipes.toJson
              ).prettyPrint)
            )))
          }
          case Failure(ex) => {
            println(s"Error retrieving recipes: ${ex.getMessage}")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("Error retrieving recipes")
              ).prettyPrint)
            )))
          }
        }
      }
    } ~
    // GET POR ID
    (path("api" / "recipe" / IntNumber) & get) { id =>
      extractRequest { request =>
        println(s"Received GET request for ID $id to /api/recipe/$id")
        println(s"Headers: ${request.headers.mkString(", ")}")
        
        onComplete(repository.getById(id)) {
          case Success(Some(recipe)) => {
            println(s"Recipe found with ID: $id")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("Recipe details by id"),
                "recipe" -> JsArray(Vector(recipe.toJson))
              ).prettyPrint)
            )))
          }
          case Success(None) => {
            println(s"No recipe found with ID: $id")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.NotFound,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("No recipe found")
              ).prettyPrint)
            )))
          }
          case Failure(ex) => {
            println(s"Error retrieving recipe ID $id: ${ex.getMessage}")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("Error retrieving recipe")
              ).prettyPrint)
            )))
          }
        }
      }
    } ~
    // UPDATE
    (path("api" / "update" / IntNumber) & post) { id =>
      extractRequest { request =>
        println(s"Received UPDATE (POST) request for ID $id to /api/update/$id")
        println(s"Headers: ${request.headers.mkString(", ")}")
        
        entity(as[String]) { requestBody =>
          println(s"UPDATE body raw: $requestBody")
          
          try {
            val recipeRequest = requestBody.parseJson.convertTo[RecipeRequest]
            println(s"Parsed recipe update request: $recipeRequest")
            
            onComplete(repository.update(id, recipeRequest)) {
              case Success(Some(recipe)) => {
                println(s"Recipe ID $id successfully updated")
                complete(addCorsHeaders(HttpResponse(
                  status = StatusCodes.OK,
                  entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                    "message" -> JsString("Recipe successfully updated!"),
                    "recipe" -> JsArray(Vector(recipe.toJson))
                  ).prettyPrint)
                )))
              }
              case Success(None) => {
                println(s"No recipe found with ID: $id for update")
                complete(addCorsHeaders(HttpResponse(
                  status = StatusCodes.NotFound,
                  entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                    "message" -> JsString("No recipe found")
                  ).prettyPrint)
                )))
              }
              case Failure(ex) => {
                println(s"Error updating recipe ID $id: ${ex.getMessage}")
                complete(addCorsHeaders(HttpResponse(
                  status = StatusCodes.InternalServerError,
                  entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                    "message" -> JsString("Error updating recipe")
                  ).prettyPrint)
                )))
              }
            }
          } catch {
            case ex: Exception =>
              println(s"Error parsing update request JSON: ${ex.getMessage}")
              complete(addCorsHeaders(HttpResponse(
                status = StatusCodes.BadRequest,
                entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                  "message" -> JsString(s"Invalid JSON format: ${ex.getMessage}")
                ).prettyPrint)
              )))
          }
        }
      }
    } ~
    // DELETE
    (path("api" / "delete" / IntNumber) & post) { id =>
      extractRequest { request =>
        println(s"Received DELETE (POST) request for ID $id to /api/delete/$id")
        println(s"Headers: ${request.headers.mkString(", ")}")
        
        onComplete(repository.delete(id)) {
          case Success(true) => {
            println(s"Recipe ID $id successfully deleted")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("Recipe successfully removed!")
              ).prettyPrint)
            )))
          }
          case Success(false) => {
            println(s"No recipe found with ID: $id for deletion")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.NotFound,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("No recipe found")
              ).prettyPrint)
            )))
          }
          case Failure(ex) => {
            println(s"Error deleting recipe ID $id: ${ex.getMessage}")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("Error deleting recipe")
              ).prettyPrint)
            )))
          }
        }
      }
    } ~
    // Mantener compatible la ruta /recipes para GET
    (path("recipes") & get) {
      extractRequest { request =>
        println(s"Received compatibility GET request to /recipes")
        println(s"Headers: ${request.headers.mkString(", ")}")
        
        onComplete(repository.getAll()) {
          case Success(recipes) => {
            println(s"Retrieved ${recipes.size} recipes via compatibility route")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "recipes" -> recipes.toJson
              ).prettyPrint)
            )))
          }
          case Failure(ex) => {
            println(s"Error retrieving recipes via compatibility route: ${ex.getMessage}")
            complete(addCorsHeaders(HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(ContentTypes.`application/json`, JsObject(
                "message" -> JsString("Error retrieving recipes")
              ).prettyPrint)
            )))
          }
        }
      }
    } ~
    // Catch-all para rutas no definidas
    extractRequest { request =>
      println(s"Received request to undefined path: ${request.uri}")
      println(s"Method: ${request.method.name}")
      println(s"Headers: ${request.headers.mkString(", ")}")
      
      complete(addCorsHeaders(HttpResponse(
        status = StatusCodes.NotFound,
        entity = HttpEntity(ContentTypes.`application/json`, JsObject(
          "message" -> JsString(s"Path not found: ${request.uri}"),
          "method" -> JsString(request.method.name),
          "info" -> JsString("Available endpoints: /api/create (POST), /api/list (GET), /api/recipe/{id} (GET), /api/update/{id} (POST), /api/delete/{id} (POST)")
        ).prettyPrint)
      )))
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