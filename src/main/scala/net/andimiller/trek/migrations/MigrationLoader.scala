package net.andimiller.trek.migrations

import net.andimiller.trek.models.LocalMigration
import net.andimiller.trek.effects.{ListFiles, ReadFile}
import fs2.io.file.Path
import cats.data.ValidatedNel
import cats.effect.kernel.Async
import cats.implicits.*

trait MigrationLoader[F[_]] {
  def load(p: Path): F[ValidatedNel[String, List[LocalMigration]]]
}

object MigrationLoader {
  def create[F[_]: Async: ListFiles: ReadFile] = new MigrationLoader[F] {
    override def load(p: Path): F[ValidatedNel[String, List[LocalMigration]]] =
      ListFiles[F].list(p).filter(_.toString.endsWith(".sql")).compile.toList.flatMap { paths =>
        paths.traverse(LocalMigration.fromPath).map(_.sequence)
      }
  }
}
