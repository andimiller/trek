package net.andimiller.trek.migrations

import cats.implicits.*
import cats.effect.Async
import cats.effect.kernel.Resource
import cats.effect.std.Console
import fs2.io.file.Path
import net.andimiller.trek.db.MigrationStatusRepository
import net.andimiller.trek.effects.ReadFile
import net.andimiller.trek.models.LocalMigration
import skunk.util.Origin
import skunk.{Fragment, Session, Void}

trait MigrationRunner[F[_]] {
  def migrate(family: String, migrations: List[LocalMigration]): F[Unit]
}

object MigrationRunner {
  def create[F[_]: Async: Console: ReadFile](db: Resource[F, Session[F]], status: MigrationStatusRepository[F]) = new MigrationRunner[F]:
    override def migrate(family: String, migrations: List[LocalMigration]): F[Unit] =
      migrations.traverse { migration =>
        for
          _       <- Console[F].println(show"Performing migration ${migration.toMigration}")
          sql     <- ReadFile[F].readAll(migration.file)
          fragment = Fragment(List(Left(sql)), Void.codec, Origin.unknown)
          result  <- db.use { session =>
                       session.execute(fragment.command)
                     }.attempt
          _       <- Console[F].println(s"  result: $result")
          _       <- result match
                       case Left(value) =>
                         Console[F].errorln(value) *> Async[F].raiseError(new Throwable("Migration failed"))
                       case Right(_)    =>
                         status.updateStatus(family, migration.version, migration.name, migration.hash)
        yield ()
      }.void *> Console[F].println("Migrations performed successfully")
}
