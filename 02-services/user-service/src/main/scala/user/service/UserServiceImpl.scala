package user.service

import user.entity.UserEntity
import user.mapper.UserMapper
import user.mapper.PermissionMapper
import user.mapper.RoleMapper
import user.models.*
import user.repository.UserRepository
import user.repository.RoleRepository
import user.repository.PermissionRepository
import zio.*
import org.mindrot.jbcrypt.BCrypt
import user.errors.*
import java.util.UUID
import java.time.Instant

class UserServiceImpl(
  userRepository: UserRepository,
  roleRepository: RoleRepository,
  permissionRepository: PermissionRepository,
) extends UserService:
  override def findUserById(id: UserId): Task[User] =
    userRepository.findById(id.value).flatMap(loadUserWithRolesAndPermissions)

  override def findUserByEmail(email: Email): Task[User] =
    userRepository.findByEmail(email.value).flatMap(loadUserWithRolesAndPermissions)

  override def registerUser(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User] =
    userRepository
      .findByEmail(email.value)
      .foldZIO(
        failure = {
          case _: UserNotFoundError =>
            for
              passwordHash <- hashPassword(password.value)
              now = Instant.now()
              userEntity =
                UserEntity(
                  id = UUID.randomUUID(),
                  email = email.value,
                  passwordHash = passwordHash,
                  firstName = firstName.map(_.value),
                  lastName = lastName.map(_.value),
                  isActive = true,
                  createdAt = now,
                  updatedAt = now,
                )
              createdEntity <- userRepository.create(userEntity)
              userRole <-
                roleRepository
                  .findByName("USER")
                  .mapError(_ =>
                    new RuntimeException(
                      "Default 'USER' role not found. Please run migrations and initializers."
                    )
                  )
              _ <- userRepository.addRoleToUser(createdEntity.id, userRole.id)

              user <- loadUserWithRolesAndPermissions(createdEntity)
            yield user
          case otherError => ZIO.fail(otherError)
        },
        success = _ => ZIO.fail(UserAlreadyExistsError(email.value)),
      )

  override def validateCredentials(email: Email, password: Password): Task[User] =
    for
      userEntity <- userRepository.findByEmail(email.value)
      user <- loadUserWithRolesAndPermissions(userEntity)
      _ <- ZIO.fail(UserNotActiveError(user.id.value)).when(!user.isActive)
      isValid <- checkPassword(password.value, user.passwordHash)
      _ <- ZIO.fail(InvalidCredentialsError()).when(!isValid)
    yield user

  override def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User] =
    for
      existingEntity <- userRepository.findById(id.value)
      now = Instant.now()
      updatedEntity =
        existingEntity.copy(
          firstName = firstName.map(_.value).orElse(existingEntity.firstName),
          lastName = lastName.map(_.value).orElse(existingEntity.lastName),
          updatedAt = now,
        )
      resultEntity <- userRepository.update(updatedEntity)
      user <- loadUserWithRolesAndPermissions(resultEntity)
    yield user

  override def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): Task[Unit] =
    for
      userEntity <- userRepository.findById(id.value)
      user <- loadUserWithRolesAndPermissions(userEntity)
      _ <- ZIO.fail(UserNotActiveError(user.id.value)).when(!user.isActive)
      isValid <- checkPassword(oldPassword.value, user.passwordHash)
      _ <- ZIO.fail(InvalidOldPasswordError(id.value)).when(!isValid)
      newHash <- hashPassword(newPassword.value)
      _ <- userRepository.updatePassword(id.value, newHash)
    yield ()

  override def deactivateUser(id: UserId): Task[Unit] =
    for
      _ <- userRepository.findById(id.value)
      _ <- userRepository.deactivate(id.value)
    yield ()

  override def findAllUsers(): Task[List[User]] =
    for
      userEntities <- userRepository.findAll()
      users <- ZIO.foreach(userEntities)(loadUserWithRolesAndPermissions)
    yield users

  override def assignRoleToUser(userId: UserId, roleId: RoleId): Task[Unit] =
    userRepository.addRoleToUser(userId.value, roleId.value)

  override def revokeRoleFromUser(userId: UserId, roleId: RoleId): Task[Unit] =
    userRepository.removeRoleFromUser(userId.value, roleId.value)

  override def getUserRoles(userId: UserId): Task[Set[Role]] =
    for
      roleIds <- userRepository.findUserRoleIds(userId.value)
      roles <-
        ZIO
          .foreach(roleIds) { roleId =>
            for
              roleEntity <- roleRepository.findById(roleId)
              permissionIds <- roleRepository.findPermissionsByRoleId(roleId)
              permissions <-
                ZIO
                  .foreach(permissionIds) { permissionId =>
                    for
                      permissionEntity <- permissionRepository.findById(permissionId)
                      permission <- PermissionMapper.toModelFromEntity(permissionEntity)
                    yield permission
                  }
                  .map(_.toSet)
              role <- RoleMapper.toModelFromEntity(roleEntity, permissions)
            yield role
          }
          .map(_.toSet)
    yield roles

  override def updateUserRoles(userId: UserId, newRoleIds: Set[RoleId]): Task[Unit] =
    for
      currentRoles <- getUserRoles(userId)
      currentRoleIds = currentRoles.map(_.id)
      rolesToAdd = newRoleIds -- currentRoleIds
      rolesToRemove = currentRoleIds -- newRoleIds
      _ <- ZIO.foreachPar(rolesToAdd)(assignRoleToUser(userId, _))
      _ <- ZIO.foreachPar(rolesToRemove)(revokeRoleFromUser(userId, _))
    yield ()

  private def hashPassword(passwordRaw: String): Task[String] =
    ZIO.attempt(BCrypt.hashpw(passwordRaw, BCrypt.gensalt(12)))

  private def checkPassword(passwordRaw: String, hashedPassword: String): Task[Boolean] =
    ZIO.attempt(BCrypt.checkpw(passwordRaw, hashedPassword))

  private def loadUserWithRolesAndPermissions(userEntity: UserEntity): Task[User] =
    for
      roleIds <- userRepository.findUserRoleIds(userEntity.id)

      roleEntities <- ZIO.foreachPar(roleIds)(roleRepository.findById)

      allPermissionIds <-
        ZIO
          .foreachPar(roleIds)(roleRepository.findPermissionsByRoleId)
          .map(_.flatten.distinct)

      permissionEntities <- ZIO.foreachPar(allPermissionIds)(permissionRepository.findById)

      permissions <-
        ZIO
          .foreachPar(permissionEntities)(PermissionMapper.toModelFromEntity)
          .map(_.groupBy(_.id))

      roles <-
        ZIO
          .foreachPar(roleEntities) { roleEntity =>
            for

              rolePermIds <- roleRepository.findPermissionsByRoleId(roleEntity.id)

              rolePermissions =
                rolePermIds
                  .flatMap(permId => permissions.get(PermissionId(permId)).map(_.head))
                  .toSet

              role <- RoleMapper.toModelFromEntity(roleEntity, rolePermissions)
            yield role
          }
          .map(_.toSet)

      user <- UserMapper.toModelFromEntity(userEntity, roles)
    yield user

object UserServiceImpl:
  val layer: URLayer[UserRepository & RoleRepository & PermissionRepository, UserService] =
    ZLayer:
      for
        userRepository <- ZIO.service[UserRepository]
        roleRepository <- ZIO.service[RoleRepository]
        permissionRepository <- ZIO.service[PermissionRepository]
      yield UserServiceImpl(userRepository, roleRepository, permissionRepository)
