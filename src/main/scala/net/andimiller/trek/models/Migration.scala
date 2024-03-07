package net.andimiller.trek.models

import cats.{Show, Order}
import cats.implicits.*

case class Migration(version: Int, name: String, hash: String)

object Migration {
  given Show[Migration]     = m => show"""${m.version} / ${m.name} / ${m.hash}"""
  given Ordering[Migration] = Ordering.by(_.version)
}
