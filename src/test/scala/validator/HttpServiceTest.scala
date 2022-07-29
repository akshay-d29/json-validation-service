package validator

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.http4s.Method.{GET, POST}
import org.http4s.Status.Ok
import org.http4s.Uri.fromString
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.{EntityDecoder, Request, Response, Status}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, BeforeAndAfterAll}
import utils.Utils._

import java.nio.file.{Path, Paths}

class HttpServiceTest extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  private val schemaStorageDir: Path = Paths.get("src/test/resources/")
  private val schemaId               = "valid-schema"

  override def afterAll(): Unit = {
    deleteFiles(schemaStorageDir)
    ()
  }
  private val jsonRequestHandler = new JsonRequestHandler(schemaStorageDir)

  "HttpService" should {
    "return success response on uploading a valid schema" in {

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
      val expected = parse(s"""{"action":"UploadSchema","id":"$schemaId","status":"Success"}""".stripMargin).getUnsafe
      val response = new HttpService(jsonRequestHandler).routes
        .run(Request(method = POST, uri = fromString(s"/schema/$schemaId").getUnsafe).withEntity(parse(validSchema).getUnsafe))

      assertResponse[Json](response, Ok, Some(expected))
    }

    "return success response on get schema with a valid schema-id" in {
      val expected = parse(s"""{"action":"GetSchema","id":"$schemaId","status":"Success"}""".stripMargin).getUnsafe
      val response = new HttpService(jsonRequestHandler).routes
        .run(Request(method = GET, uri = fromString(s"/schema/$schemaId").toOption.get))

      assertResponse[Json](response, Ok, Some(expected))
    }

    "return error response on get schema with an invalid schema-id" in {
      val invalidId = "invalid-schema"
      val expected = parse(
        s"""{"action":"GetSchema","id":"$invalidId","status":"Error(File Not Found. Please check schema id.)", "message": "File Not Found. Please check schema id."}""".stripMargin
      ).getUnsafe
      val response = new HttpService(jsonRequestHandler).routes
        .run(Request(method = GET, uri = fromString(s"/schema/$invalidId").toOption.get))

      assertResponse[Json](response, Ok, Some(expected))
    }

    "return success response on validating a json against a valid schema" in {
      val expected = parse(s"""{"action":"ValidateDocument","id":"$schemaId","status":"Success"}""".stripMargin).getUnsafe
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
      val response = new HttpService(jsonRequestHandler).routes
        .run(Request(method = POST, uri = fromString(s"/validate/$schemaId").toOption.get).withEntity(parse(jsonStr).getUnsafe))

      assertResponse[Json](response, Ok, Some(expected))
    }

    "return error response on validating an invalid json against a valid schema " in {
      val invalidJsonStr: String =
        """
          |{
          |  "source": "/home/alice/image.iso",
          |  "timeout": null,
          |  "chunks": {
          |    "size": 1024,
          |    "number": null
          |  }
          |}""".stripMargin
      val expected = parse(
        s"""{"action":"ValidateDocument","id":"$schemaId","status":"Error(#: required key [destination] not found)", "message": "#: required key [destination] not found"}""".stripMargin
      ).getUnsafe
      val response = new HttpService(jsonRequestHandler).routes
        .run(Request(method = POST, uri = fromString(s"/validate/$schemaId").getUnsafe).withEntity(parse(invalidJsonStr).getUnsafe))

      assertResponse[Json](response, Ok, Some(expected))
    }
  }

  private def assertResponse[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])(implicit ev: EntityDecoder[IO, A]): Assertion = {
    val actualResp         = actual.unsafeRunSync()
    val statusCheck        = actualResp.status == expectedStatus
    val bodyCheck          = expectedBody.fold[Boolean](actualResp.body.compile.toVector.unsafeRunSync().isEmpty)(expected => actualResp.as[A].unsafeRunSync() == expected)
    val isExpectedResponse = statusCheck && bodyCheck

    isExpectedResponse should be(true)
  }
}
