package validator

import cats.effect.IO
import io.circe.Json
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}

import scala.language.postfixOps
import io.circe.syntax.EncoderOps
import validator.Domain.{Error, Success, UploadSchema, ValidationResponse}

class HttpService(jsonRequestHandler: JsonRequestHandler) {

  private val errorHandler: Throwable => Json = (ex: Throwable) => Json.fromString(ex.getMessage)

  def routes: HttpApp[IO] = {
    val dsl = Http4sDsl[IO]
    import dsl._
    val schema: String = "schema"

    HttpRoutes
      .of[IO] {
        case request @ POST -> Root / `schema` / id =>
          (for {
            requestJson <- request.as[Json]
            response    <- jsonRequestHandler.uploadSchema(requestJson, id)
          } yield response match {
            case res @ ValidationResponse(_, _, status) if status == Success => Created(res.asJson)
            case res                                                         => Ok(res.asJson)
          }).flatten.handleErrorWith(ex => Ok(ValidationResponse(UploadSchema, id, Error(ex.getMessage)).asJson))

        case GET -> Root / `schema` / id =>
          jsonRequestHandler.getSchema(id).flatMap {
            case res @ ValidationResponse(_, _, status) if status == Success => Ok(res.asJson)
            case res                                                         => BadRequest(res.asJson)
          }

        case request @ POST -> Root / "validate" / id =>
          val result = (for {
            requestJson <- request.as[Json]
            res         <- jsonRequestHandler.validate(requestJson, id)
          } yield res.asJson).handleError(errorHandler)

          Ok(result)

      } orNotFound
  }
}
