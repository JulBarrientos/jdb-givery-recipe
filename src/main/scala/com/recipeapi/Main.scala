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
  
  // Inicializar rutas
  val routes = new RecipeRoutes(repository).routes

  // Iniciar servidor
  println(s"Intentando iniciar servidor en ${Config.serverHost}:${Config.serverPort}")
  val bindingFuture = Http().newServerAt(Config.serverHost, Config.serverPort).bind(routes)

  bindingFuture.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      println(s"Servidor iniciado en http://${address.getHostString}:${address.getPort}/")
    case Failure(ex) =>
      println(s"Error al iniciar el servidor: ${ex.getMessage}")
      system.terminate()
  }

  scala.sys.addShutdownHook {
    println("Cerrando servidor...")
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  try {
    scala.concurrent.Await.result(system.whenTerminated, scala.concurrent.duration.Duration.Inf)
  } catch {
    case ex: Exception =>
      println(s"Error durante la ejecución: ${ex.getMessage}")
  }
    repository.ensureTablesExist().onComplete {
    case Success(_) => println("Tablas verificadas correctamente.")
    case Failure(ex) => println(s"Error al verificar tablas: ${ex.getMessage}")
  }
}
