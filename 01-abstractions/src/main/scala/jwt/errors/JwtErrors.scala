package jwt.errors

import common.errors.BusinessError

sealed trait JwtError extends BusinessError

final case class TokenMissingClaimError(claimName: String, tokenType: String = "Access")
    extends JwtError:
  override val errorCode: String =
    "TOKEN_MISSING_CLAIM"
  override def message: String =
    s"$tokenType token is missing required claim: '$claimName'."

final case class TokenExpiredError(
  tokenType: String = "Access",
  expiredAt: Option[Long] = None,
  now: Option[Long] = None,
) extends JwtError:
  override val errorCode: String =
    "TOKEN_EXPIRED"
  override def message: String =
    val details =
      (expiredAt, now) match
        case (Some(exp), Some(n)) => s" (expired at: $exp, current time: $n)"
        case _ => ""
    s"$tokenType token has expired$details."

final case class InvalidTokenSubjectFormatError(
  subject: String,
  reason: String,
  tokenType: String = "Access",
) extends JwtError:
  override val errorCode: String =
    "INVALID_TOKEN_SUBJECT_FORMAT"
  override def message: String =
    s"Invalid subject format in $tokenType token. Subject: '$subject', Reason: $reason."

final case class TokenDecodingError(tokenType: String = "Access", details: Option[String] = None)
    extends JwtError:
  override val errorCode: String =
    "TOKEN_DECODING_ERROR"
  override def message: String =
    val detailMessage = details.map(d => s": $d").getOrElse("")
    s"$tokenType token is malformed or could not be decoded$detailMessage."
