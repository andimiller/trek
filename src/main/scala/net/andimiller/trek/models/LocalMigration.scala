package net.andimiller.trek.models

import cats.{Applicative, Eq, Show}
import cats.parse.{Numbers, Parser, Parser0}
import cats.implicits.*
import cats.data.ValidatedNel
import fs2.io.file.Path
import net.andimiller.trek.effects.ReadFile

case class LocalMigration(version: Int, name: String, file: Path, hash: String) {
  def toMigration: Migration = Migration(version, name, hash)
}

object LocalMigration {
  given Ordering[LocalMigration] = Ordering.by(_.version)
  given Show[LocalMigration]     = m => show"""LocalMigration(version = ${m.version}, name = ${m.name}, hash = ${m.hash})"""

  object Parse {
    val V            = Parser.char('V').rep0(0, 1)
    val paddingChars = "-_ ".toSet
    val padding      = Parser.charsWhile0(paddingChars)
    val version      = Numbers.digits.map(_.toInt)
    val name         = Parser.charsWhile(_ != '.')
    val extension    = Parser.string(".sql")

    val parser: Parser0[(Path, String) => LocalMigration] = for
      version <- V *> version <* padding
      name    <- name <* extension
    yield LocalMigration(version, name, _, _)

    def apply[F[_]: ReadFile: Applicative](p: Path): F[Either[String, LocalMigration]] =
      ReadFile[F].hashFile(p).map { hash =>
        parser.parseAll(p.fileName.toString).map(_.apply(p, hash)).leftMap(e => show"$p - $e")
      }
  }

  def fromPath[F[_]: ReadFile: Applicative](p: Path): F[ValidatedNel[String, LocalMigration]] =
    Parse(p).map(_.toValidatedNel)
}
