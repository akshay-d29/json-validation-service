package validator

import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import scala.concurrent.ExecutionContext.global

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    serverResource.use(_ => IO.never).as(ExitCode.Success)

  private def serverResource: Resource[IO, Server] = {
    val jsonRequestHandler = new JsonRequestHandler()
    val httpService = new HttpService(jsonRequestHandler)

    BlazeServerBuilder[IO]
      .withExecutionContext(global)
      .bindHttp(port = 8080, host = "localhost")
      .withHttpApp(httpService.routes)
      .resource
  }
}
