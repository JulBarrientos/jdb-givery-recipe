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
  repository.init()
  
  // Inicializar rutas
  val routes = new RecipeRoutes(repository).routes
  
  val port = sys.env.get("PORT").map(_.toInt).getOrElse(Config.serverPort)
  val host = sys.env.get("HOST").getOrElse(Config.serverHost)

  // Iniciar servidor
  val bindingFuture = Http().newServerAt(host, port).bind(routes)
  
  sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
