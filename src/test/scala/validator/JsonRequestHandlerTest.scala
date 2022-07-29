package validator

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import validator.Domain.{Error, GetSchema, Success, UploadSchema, ValidateDocument, ValidationResponse}
import java.nio.file.{Files, Path, Paths}

class JsonRequestHandlerTest extends AnyWordSpecLike with Matchers {

  private val jsonRequestHandler = new JsonRequestHandler()
  private val schemaStorageDir: Path = Paths.get("src/test/resources/")
  private val schemaDownloadDir: Path = Paths.get("src/test/downloads/")

  "JsonRequestHandler" should {
    val schemaId = "valid-schema"
    val validSchema: String =
      """
        |{
        |  "$schema": "http://json-schema.org/draft-04/schema#",
        |  "type": "object",
        |  "properties": {
        |    "source": {
        |      "type": "string"
        |    },
        |    "destination": {
        |      "type": "string"
        |    },
        |    "timeout": {
        |      "type": "integer",
        |      "minimum": 0,
        |      "maximum": 32767
        |    },
        |    "chunks": {
        |      "type": "object",
        |      "properties": {
        |        "size": {
        |          "type": "integer"
        |        },
        |        "number": {
        |          "type": "integer"
        |        }
        |      },
        |      "required": ["size"]
        |    }
        |  },
        |  "required": ["source", "destination"]
        |}
        |""".stripMargin

    "return success on uploading valid schema" in {
      IO(Files.deleteIfExists(schemaStorageDir.resolve(s"$schemaId.json"))).unsafeRunSync()
      val schemaJson: Json = parse(validSchema).getOrElse(Json.Null)
      val result = jsonRequestHandler.uploadSchema(schemaJson, schemaId, schemaStorageDir)
      val expected = ValidationResponse(UploadSchema, schemaId, Success)

      result.unsafeRunSync() shouldBe expected
    }

    "return error with message on uploading already uploaded schema" in {
      val schemaJson: Json = parse(validSchema).getOrElse(Json.Null)
      val result = jsonRequestHandler.uploadSchema(schemaJson, schemaId, schemaStorageDir)
      val expected = ValidationResponse(UploadSchema, schemaId, Error("File already exists!"))

      result.unsafeRunSync() shouldBe expected
    }

    "return success on validating json against schema" in {
      val jsonStr: String =
        """
          |{
          |  "source": "/home/alice/image.iso",
          |  "destination": "/mnt/storage",
          |  "timeout": null,
          |  "chunks": {
          |    "size": 1024,
          |    "number": null
          |  }
          |}""".stripMargin
      val json = parse(jsonStr).getOrElse(Json.Null)
      val result = jsonRequestHandler.validate(json, schemaId)
      val expected = ValidationResponse(ValidateDocument, schemaId, Success)

      result.unsafeRunSync() shouldBe expected
    }

    "return error on validating invalid json against schema" in {
      val jsonStr: String =
        """
          |{
          |  "source": "/home/alice/image.iso",
          |  "timeout": null,
          |  "chunks": {
          |    "size": 1024,
          |    "number": null
          |  }
          |}""".stripMargin
      val json = parse(jsonStr).getOrElse(Json.Null)
      val result = jsonRequestHandler.validate(json, schemaId)
      val expected = ValidationResponse(ValidateDocument, schemaId, Error("#: required key [destination] not found"))

      result.unsafeRunSync() shouldBe expected
    }

    "return success on calling get schema with valid schema-id and download file" in {
      val result = jsonRequestHandler.getSchema(schemaId, schemaDownloadDir)
      val expected = ValidationResponse(GetSchema, schemaId, Success)
      result.unsafeRunSync() shouldBe expected
    }

    "return error on calling get schema with invalid schema-id" in {
      val result = jsonRequestHandler.getSchema("invalid-schema", schemaDownloadDir)
      val expected = ValidationResponse(GetSchema, "invalid-schema", Error("File Not Found. Please check schema id."))
      result.unsafeRunSync() shouldBe expected
    }
  }

}
