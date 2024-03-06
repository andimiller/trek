package net.andimiller.trek.migrations

import cats.implicits.*
import cats.Show
import net.andimiller.trek.models.*

object MigrationDiffer {

  case class DiffResult(localMissing: Set[Migration], remoteMissing: Set[Migration])

  object DiffResult {
    given Show[DiffResult] = { case DiffResult(localMissing, remoteMissing) =>
      List(
        Option.when(localMissing.nonEmpty)(
          show"""Local migration folder is missing these migrations found in the remote:
                |  ${localMissing.map(_.show).mkString("\n  ")}
                |""".stripMargin
        ),
        Option.when(remoteMissing.nonEmpty)(
          show"""Remote database is missing these migrations found locally:
                |  ${remoteMissing.map(_.show).mkString("\n  ")}
                |""".stripMargin
        ),
        Option.when(localMissing.isEmpty && remoteMissing.isEmpty)(
          show"""Database is in sync"""
        )
      ).flatten.mkString("\n")
    }
  }

  def diff(localMigrations: List[LocalMigration], remoteMigrations: List[RemoteMigration]) = {
    val remoteSet = remoteMigrations.map(_.toMigration).toSet
    val localSet  = localMigrations.map(_.toMigration).toSet

    val toBeApplied = localSet diff remoteSet
    val missing     = remoteSet diff localSet

    DiffResult(missing, toBeApplied)
  }

}
