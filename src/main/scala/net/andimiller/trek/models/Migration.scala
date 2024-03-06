package net.andimiller.trek.models

import cats.Show
import cats.implicits.*

case class Migration(version: Int, name: String, hash: String)

object Migration {
  given Show[Migration] = m => show"""${m.version} / ${m.name} / ${m.hash}"""
}
