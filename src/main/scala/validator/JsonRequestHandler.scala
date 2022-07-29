package validator

import cats.data.Validated
import cats.effect.{IO, Resource}
import io.circe.schema.Schema
import io.circe.{Json, JsonObject}

import java.io.{BufferedReader, FileReader}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.IteratorHasAsScala

class JsonRequestHandler() {
  private val schemaStorageDir: Path = Paths.get("src/main/resources/")

  def uploadSchema(json: Json, id: String): IO[Json] = {
    val schemaPath = schemaStorageDir.resolve(s"$id.json")

    IO(Files.write(schemaPath, json.spaces2.getBytes, StandardOpenOption.CREATE)).attempt.map {
      case Left(_)  => ValidationResponse(UploadSchema, id, Error()).response
      case Right(_) => ValidationResponse(UploadSchema, id, Success).response
    }
  }

  def getSchema(id: String): IO[String] = {
    val filePath = schemaStorageDir.resolve(s"$id.json")
    if (Files.exists(filePath)) {
      val bufferedResource = Resource.make(
        IO(new BufferedReader(new FileReader(schemaStorageDir.resolve(s"$id.json").toFile)))
      )(reader => IO(reader.close()))

      bufferedResource.use { reader =>
        IO(reader.lines().iterator().asScala.mkString).handleError(_.getMessage)
      }
    } else IO("File Not Found")
  }

  def validate(json: Json, id: String): IO[Json] = {
    for {
      schemaStr <- getSchema(id)
      schema    <- IO.fromTry[Schema](Schema.loadFromString(schemaStr))
      filteredJson = json.deepDropNullValues
      result <- IO(schema.validate(filteredJson))
    } yield result match {
      case Validated.Valid(_)   => ValidationResponse(ValidateDocument, id, Success).response
      case Validated.Invalid(e) => ValidationResponse(ValidateDocument, id, Error(e.toList.map(_.getMessage).mkString(","))).response
    }
  }
}

sealed trait DomainAction
case object UploadSchema     extends DomainAction
case object ValidateDocument extends DomainAction

sealed trait Outcome
case object Success                                extends Outcome
case class Error(message: String = "Invalid Json") extends Outcome

case class ValidationResponse(action: DomainAction, id: String, status: Outcome) {
  private val defaultResponse: JsonObject = JsonObject(
    "action" -> Json.fromString(action.toString),
    "id"     -> Json.fromString(id),
    "status" -> Json.fromString(status.toString)
  )
  def response: Json = status match {
    case Success => Json.fromJsonObject(defaultResponse)
    case Error(msg) =>
      Json.fromJsonObject(
        defaultResponse.add("message", Json.fromString(msg))
      )
  }
}
