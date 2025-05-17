package com.recipeapi.json

import com.recipeapi.models.{Recipe, RecipeRequest}
import spray.json._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

trait JsonFormats extends DefaultJsonProtocol {
  
  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    def write(dateTime: LocalDateTime): JsValue = JsString(dateTime.format(formatter))
    
    def read(json: JsValue): LocalDateTime = json match {
      case JsString(str) => LocalDateTime.parse(str, formatter)
      case _ => throw DeserializationException("LocalDateTime expected")
    }
  }
  
  implicit val recipeFormat: RootJsonFormat[Recipe] = jsonFormat8(Recipe)
  implicit val recipeRequestFormat: RootJsonFormat[RecipeRequest] = jsonFormat5(RecipeRequest)
}
