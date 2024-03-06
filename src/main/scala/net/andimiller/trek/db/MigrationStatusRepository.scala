package net.andimiller.trek.db

import cats.effect.*
import cats.effect.kernel.{Clock, Resource}
import cats.implicits.*
import net.andimiller.trek.models.RemoteMigration
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*

import java.time.{ZoneId, ZoneOffset}

trait MigrationStatusRepository[F[_]] {
  def provisionMigrationStatusTable: F[Unit]
  def getHistory(family: String): F[List[RemoteMigration]]
  def updateStatus(family: String, version: Int, versionName: String, versionHash: String): F[Unit]
}

object MigrationStatusRepository {
  object sql {
    val provisionMigrationStatusTable: Command[Void] =
      sql"""
            CREATE TABLE IF NOT EXISTS trek_migration_status (
              family VARCHAR(255) NOT NULL PRIMARY KEY,
              time TIMESTAMPTZ NOT NULL,
              version INT4 NOT NULL,
              version_name VARCHAR(255) NOT NULL,
              version_hash VARCHAR(8) NOT NULL
            );
         """.command

    val migrationStatus: Codec[RemoteMigration] =
      (varchar(255) *: timestamptz *: int4 *: varchar(255) *: varchar(8)).to[RemoteMigration]

    val getHistory: Query[String, RemoteMigration] =
      sql"""
           SELECT * FROM trek_migration_status WHERE family = ${varchar(255)} ORDER BY version ASC
         """.query(migrationStatus)

    val updateStatus: Command[RemoteMigration] =
      sql"""
           INSERT INTO trek_migration_status VALUES ($migrationStatus)
         """.command
  }

  def create[F[_]: Temporal: Clock](db: Resource[F, Session[F]]) = new MigrationStatusRepository[F] {

    override def provisionMigrationStatusTable: F[Unit] =
      db.use { session =>
        session.execute(sql.provisionMigrationStatusTable).void
      }

    override def getHistory(family: String): F[List[RemoteMigration]] =
      db.use { session =>
        session.prepare(sql.getHistory).flatMap(_.stream(family, 1024).compile.toList)
      }

    override def updateStatus(family: String, version: Int, versionName: String, versionHash: String): F[Unit] =
      db.use { session =>
        for
          now <- Clock[F].realTimeInstant.map(_.atZone(ZoneId.systemDefault()).toOffsetDateTime)
          q   <- session.prepare(sql.updateStatus)
          _   <- q.execute(RemoteMigration(family, now, version, versionName, versionHash))
        yield ()
      }
  }

}
