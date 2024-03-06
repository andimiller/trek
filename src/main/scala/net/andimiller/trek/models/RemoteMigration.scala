package net.andimiller.trek.models

import cats.Show
import cats.implicits.showInterpolator

import java.time.OffsetDateTime

case class RemoteMigration(family: String, time: OffsetDateTime, version: Int, versionName: String, versionHash: String) {
  def toMigration = Migration(version, versionName, versionHash)
}
