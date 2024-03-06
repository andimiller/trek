package net.andimiller.trek.effects

import cats.implicits.*
import cats.Applicative
import cats.effect.Async
import fs2.io.file.{Files, Path}
import fs2.Stream
import scodec.bits.ByteVector

import java.security.MessageDigest
import scala.util.hashing.{Hashing, MurmurHash3}

/*
 * Represents a subset of the Files effect from fs2, for ease of use with tests
 */
@FunctionalInterface
trait ReadFile[F[_]: Applicative]:
  def readAll(p: Path): F[String]
  def hashFile(p: Path): F[String] = readAll(p).map { s =>
    ByteVector
      .fromInt(
        MurmurHash3.stringHash(s)
      )
      .toHex
  }

object ReadFile:
  def apply[F[_]](using f: ReadFile[F]): ReadFile[F] = f

  given [F[_]: Async](using Files[F]): ReadFile[F] =
    new ReadFile[F]:
      override def readAll(p: Path): F[String] =
        Files[F].readUtf8(p).compile.foldMonoid

  def static[F[_]: Applicative](db: Map[String, String]): ReadFile[F] =
    new ReadFile[F]:
      override def readAll(p: Path): F[String] =
        db(p.toString).pure[F]
