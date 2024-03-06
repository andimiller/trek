package net.andimiller.trek.effects

import cats.Applicative
import fs2.io.file.{Files, Path}
import fs2.Stream

/*
 * Represents a subset of the Files effect from fs2, for ease of use with tests
 */
@FunctionalInterface
trait ListFiles[F[_]]:
  def list(p: Path): Stream[F, Path]

object ListFiles:
  def apply[F[_]](using f: ListFiles[F]): ListFiles[F] = f

  given [F[_]](using Files[F]): ListFiles[F] =
    Files[F].list

  def static[F[_]: Applicative](db: Map[String, List[String]]): ListFiles[F] =
    p => Stream.emits(db.getOrElse(p.toString, List.empty)).covary[F].map(Path(_))
