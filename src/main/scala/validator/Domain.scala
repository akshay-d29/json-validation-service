package validator

import io.circe.{Encoder, Json, JsonObject}

object Domain {

  sealed trait DomainAction
  case object UploadSchema     extends DomainAction
  case object ValidateDocument extends DomainAction
  case object GetSchema        extends DomainAction

  sealed trait Outcome
  case object Success               extends Outcome
  case class Error(message: String) extends Outcome

  case class ValidationResponse(action: DomainAction, id: String, status: Outcome) {
    private val defaultResponse: JsonObject = JsonObject(
      "action" -> Json.fromString(action.toString),
      "id"     -> Json.fromString(id),
      "status" -> Json.fromString(status.toString)
    )

    private def response: Json = status match {
      case Success => Json.fromJsonObject(defaultResponse)
      case Error(msg) =>
        Json.fromJsonObject(
          defaultResponse.add("message", Json.fromString(msg))
        )
    }
  }

  object ValidationResponse {
    implicit val encoder: Encoder[ValidationResponse] = Encoder.instance(_.response)
  }

}
