package net.andimiller.trek

import cats.data.{OptionT, Validated}
import cats.effect.std.*
import cats.effect.*
import cats.implicits.*
import fs2.io.net.Network
import net.andimiller.trek.db.MigrationStatusRepository
import org.typelevel.otel4s.trace.Tracer
import skunk.Session
import Tracer.Implicits.noop
import fs2.io.file.Path
import net.andimiller.trek.CLI.CliCommand
import net.andimiller.trek.migrations.*
import epollcat.EpollApp

object Main extends EpollApp:
  override def run(args: List[String]): IO[ExitCode] = program[IO](args)

  def program[F[_]: Async: Env: Tracer: Network: Console](args: List[String]) =
    CLI.cli.parse(args, sys.env) match
      case Left(value) =>
        Console[F].println(value).as(ExitCode.Error)
      case Right(mode) =>
        mode match
          case CliCommand.Check(db, folder)   =>
            for
              _              <- Console[F].println(s"Validating $folder")
              session         = Session.single(
                                  host = db.host,
                                  port = db.port,
                                  database = db.database,
                                  user = db.user,
                                  password = db.password
                                )
              migrationStatus = MigrationStatusRepository.create(session)
              loader          = MigrationLoader.create[F]
              _              <- migrationStatus.provisionMigrationStatusTable
              local          <- loader.load(folder).flatMap {
                                  case Validated.Valid(a)   => a.pure[F]
                                  case Validated.Invalid(e) => Console[F].errorln(e.show) *> Async[F].raiseError(new Throwable("boom"))
                                }
              remote         <- migrationStatus.getHistory(folder.toString)
              diff            = MigrationDiffer.diff(local, remote)
              _              <- Console[F].println(diff.show)
            yield ExitCode.Success
          case CliCommand.Migrate(db, folder) =>
            for
              _                <- Console[F].println(s"Applying $folder")
              session           = Session.single(
                                    host = db.host,
                                    port = db.port,
                                    database = db.database,
                                    user = db.user,
                                    password = db.password
                                  )
              migrationStatus   = MigrationStatusRepository.create(session)
              loader            = MigrationLoader.create[F]
              _                <- migrationStatus.provisionMigrationStatusTable
              local            <- loader.load(folder).flatMap {
                                    case Validated.Valid(a)   => a.pure[F]
                                    case Validated.Invalid(e) => Console[F].errorln(e.show) *> Async[F].raiseError(new Throwable("boom"))
                                  }
              remote           <- migrationStatus.getHistory(folder.toString)
              diff              = MigrationDiffer.diff(local, remote)
              _                <- Console[F].println(diff.show)
              runner            = MigrationRunner.create[F](session, migrationStatus)
              idsToBeRun        = diff.remoteMissing.map(_.version).toSet
              migrationsToBeRun = local.filter(m => idsToBeRun.contains(m.version))
              code             <- if (diff.localMissing.isEmpty) {
                                    for
                                      _ <- Console[F].println(s"Proceeding with migrations")
                                      c <- runner.migrate(folder.toString, migrationsToBeRun).attempt.map {
                                             case Left(_)  => ExitCode.Error
                                             case Right(_) => ExitCode.Success
                                           }
                                    yield c
                                  } else {
                                    Console[F].errorln(s"Suspected bad state in your local migrations folder, exiting").as(ExitCode.Error)
                                  }
            yield code
