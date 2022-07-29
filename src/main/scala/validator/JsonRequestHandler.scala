package validator

import cats.data.Validated
import cats.effect.{IO, Resource}
import io.circe.Json
import io.circe.schema.Schema
import validator.Domain._

import java.io.{BufferedReader, FileReader}
import java.nio.file.{Files, Path, Paths, StandardCopyOption, StandardOpenOption}
import scala.jdk.CollectionConverters.IteratorHasAsScala

class JsonRequestHandler() {
  private val schemaStorageDir: Path = Paths.get("src/main/resources/")
  private val schemaDownloadPath: Path = Paths.get("downloads/")
  private val extension: String = ".json"

  def uploadSchema(json: Json, id: String, schemaDir: Path = schemaStorageDir): IO[ValidationResponse] = {
    val schemaPath = schemaDir.resolve(s"$id$extension")

    if(Files.exists(schemaPath))
      IO.pure(ValidationResponse(UploadSchema, id, Error("File already exists!")))
    else {
      IO(Files.write(schemaPath, json.spaces2.getBytes, StandardOpenOption.CREATE)).attempt.map {
        case Left(_) => ValidationResponse(UploadSchema, id, Error())
        case Right(_) => ValidationResponse(UploadSchema, id, Success)
      }
    }
  }

  def getSchema(id: String, downloadPath: Path = schemaDownloadPath): IO[ValidationResponse] = {
    val filePath = schemaStorageDir.resolve(s"$id$extension")
    if (Files.exists(filePath))
      IO(
        Files.copy(
          schemaStorageDir.resolve(s"$id$extension"),
          downloadPath.resolve(s"$id$extension"),
          StandardCopyOption.REPLACE_EXISTING
        )) *> IO(ValidationResponse(GetSchema, id, Success))
    else IO.pure(ValidationResponse(GetSchema, id, Error("File Not Found. Please check schema id.")))
  }

  def validate(json: Json, id: String): IO[ValidationResponse] = {
    for {
      schemaStr <- readSchemaJson(id)
      schema    <- IO.fromTry[Schema](Schema.loadFromString(schemaStr))
      filteredJson = json.deepDropNullValues
      result = schema.validate(filteredJson)
    } yield {
      result match {
        case Validated.Valid(_)   => ValidationResponse(ValidateDocument, id, Success)
        case Validated.Invalid(e) => ValidationResponse(ValidateDocument, id, Error(e.toList.map(_.getMessage).mkString(",")))
      }
    }
  }

  private def readSchemaJson(id: String): IO[String] = {
    val filePath = schemaStorageDir.resolve(s"$id$extension")
    if (Files.exists(filePath)) {
      val bufferedResource = Resource.make{
        IO(new BufferedReader(new FileReader(schemaStorageDir.resolve(s"$id$extension").toFile)))
      }(reader => IO(reader.close()))

      bufferedResource.use(reader => IO(reader.lines().iterator().asScala.mkString))
    } else IO.raiseError(new RuntimeException(s"File not found: $id$extension"))
  }
}
