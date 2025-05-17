package com.recipeapi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer

import com.recipeapi.repositories.RecipeRepository
import com.recipeapi.routes.RecipeRoutes
import com.recipeapi.config.Config

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.Failure
import scala.util.Success

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("recipe-api")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  
  // Inicializar repositorio
  val repository = new RecipeRepository()
  //repository.init()
  
  // Inicializar rutas
  val routes = new RecipeRoutes(repository).routes
  
  // Iniciar servidor
  val bindingFuture = Http().newServerAt(Config.serverHost, Config.serverPort).bind(routes)
  
  println(s"Server online at http://${Config.serverHost}:${Config.serverPort}/\nPress RETURN to stop...")

  StdIn.readLine()
  
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
