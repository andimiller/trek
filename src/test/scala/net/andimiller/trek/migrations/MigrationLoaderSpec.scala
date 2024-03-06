package net.andimiller.trek.migrations

import cats.implicits.*
import cats.effect.IO
import cats.data.*
import munit.CatsEffectSuite
import fs2.io.file.{Files, Path}
import net.andimiller.trek.models.LocalMigration
import net.andimiller.trek.effects.*

class MigrationLoaderSpec extends CatsEffectSuite {

  extension (s: String) def dropCR: String = s.replace("\r", "")

  test("Should load a valid file") {
    given ListFiles[IO] = ListFiles.static[IO](
      Map(
        "migrations" -> List("01_one.sql", "V2_two.sql")
      )
    )
    given ReadFile[IO]  = ReadFile.static[IO](
      Map(
        "01_one.sql" -> "hello",
        "V2_two.sql" -> "world"
      )
    )
    MigrationLoader
      .create[IO]
      .load(Path("migrations"))
      .assertEquals(
        List(
          LocalMigration(1, "one", Path("01_one.sql"), "1c02b9f6"),
          LocalMigration(2, "two", Path("V2_two.sql"), "8ab54779")
        ).validNel
      )
  }

  test("Should flag bad files") {
    given ListFiles[IO] = ListFiles.static[IO](
      Map(
        "migrations" -> List("01_one.sql", "V2_two.sql", "three.sql", "four", "5five.sql", "six6.sql")
      )
    )

    given ReadFile[IO] = ReadFile.static[IO](
      Map(
        "01_one.sql" -> "",
        "V2_two.sql" -> "",
        "three.sql"  -> "",
        "four"       -> "",
        "5five.sql"  -> "",
        "six6.sql"   -> ""
      )
    )

    MigrationLoader
      .create[IO]
      .load(Path("migrations"))
      .assertEquals(
        NonEmptyList
          .of(
            """three.sql - three.sql
            |^
            |expectation:
            |* must be a char within the range of: ['0', '9']""".stripMargin.dropCR,
            """six6.sql - six6.sql
            |^
            |expectation:
            |* must be a char within the range of: ['0', '9']""".stripMargin.dropCR
          )
          .invalid
      )
  }

}
