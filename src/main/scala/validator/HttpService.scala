package validator

import cats.effect.IO
import io.circe.Json
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import scala.language.postfixOps
import io.circe.syntax.EncoderOps

class HttpService(jsonRequestHandler: JsonRequestHandler) {

  private val errorHandler: Throwable => Json = (ex: Throwable) => Json.fromString(ex.getMessage)

  def routes: HttpApp[IO] = {
    val dsl = Http4sDsl[IO]
    import dsl._
    val schema: String = "schema"

    HttpRoutes
      .of[IO] {
        case request @ POST -> Root / `schema` / id =>
          val result = (for {
            requestJson <- request.as[Json]
            res         <- jsonRequestHandler.uploadSchema(requestJson, id)
          } yield res.asJson).handleError(errorHandler)

          Ok(result)

        case GET -> Root / `schema` / id => jsonRequestHandler.getSchema(id).flatMap(res => Ok(res.asJson))

        case request @ POST -> Root / "validate" / id =>
          val result = (for {
            requestJson <- request.as[Json]
            res         <- jsonRequestHandler.validate(requestJson, id)
          } yield res.asJson).handleError(errorHandler)

          Ok(result)

      } orNotFound
  }
}
