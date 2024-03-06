package net.andimiller.trek

import cats.data.ValidatedNel
import cats.implicits.*
import com.monovore.decline.{Argument, Command, Opts}
import fs2.io.file.Path

object CLI {

  case class DBConfig(
      host: String,
      port: Int,
      database: String,
      user: String,
      password: Option[String]
  )

  val dbConfig: Opts[DBConfig] =
    (
      Opts.env[String]("POSTGRES_HOST", "host to connect to postgres on").withDefault("localhost"),
      Opts.env[Int]("POSTGRES_PORT", "port to connect to postgres on").withDefault(5432),
      Opts.env[String]("POSTGRES_DATABASE", "database to provision").withDefault("postgres"),
      Opts.env[String]("POSTGRES_USER", "user to connect with").withDefault("postgres"),
      Opts.env[String]("POSTGRES_PASSWORD", "password to connect with").orNone
    ).mapN(DBConfig.apply)

  given Argument[Path] = new Argument[Path]:
    override def read(string: String): ValidatedNel[String, Path] = Path(string).validNel
    override def defaultMetavar: String                           = "folder/"

  val folder = Opts.argument[Path]("folder")

  enum CliCommand:
    case Check(dbConfig: DBConfig, folder: Path)

  val check: Opts[CliCommand.Check] = Opts.subcommand(Command("check", "check a database's migration state vs a folder of migrations") {
    (dbConfig, folder).mapN(CliCommand.Check.apply)
  })

  val cli: Command[CliCommand] = Command("trek", "postgres migration tool")(check)
}
