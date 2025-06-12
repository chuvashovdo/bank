package common.errors

trait BusinessError extends Throwable:
  def errorCode: String
  def message: String

final case class CorruptedDataInDBError(
  entityId: String,
  fieldName: String,
  fieldValue: String,
  validationErrorMessage: String,
) extends BusinessError:
  override val errorCode: String =
    "CORRUPTED_DATA_IN_DB"
  override def message: String =
    s"Corrupted data in DB for entity $entityId, field '$fieldName' (value: '$fieldValue'). Validation failed: $validationErrorMessage"
