package common.errors

sealed trait ValidationError extends Throwable:
  def input: Option[String]
  def developerFriendlyMessage: String
  override def getMessage: String =
    s"${this.getClass.getSimpleName}: $developerFriendlyMessage" + input.fold("")(i =>
      s" (Input: '$i')"
    )

case class InvalidDataFormatError(
  fieldName: String,
  ruleDescription: String,
  override val input: Option[String],
  details: String,
) extends ValidationError:
  override val developerFriendlyMessage: String =
    s"Поле '$fieldName' не соответствует правилу: '$ruleDescription'. Детали: $details"

case class ValueCannotBeEmptyError(
  fieldName: String,
  override val input: Option[String],
) extends ValidationError:
  override val developerFriendlyMessage: String =
    s"Поле '$fieldName' не может быть пустым."
