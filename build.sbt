ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.0"

enablePlugins(ScalaNativePlugin)

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

// import to add Scala Native options
import scala.scalanative.build._

name := "trek"

// defaults set with common options shown
nativeConfig ~= { c =>
  c.withLTO(LTO.none)     // thin
    .withMode(Mode.debug) // releaseFast
    .withGC(GC.immix)     // commix
}

libraryDependencies ++= List(
  "com.armanbilge" %%% "epollcat" % "0.1.6", // some runtime parts
  "org.tpolecat"  %%% "skunk-core"        % "1.1.0-M3",
  "com.monovore"  %%% "decline"           % "2.4.1",
  "org.typelevel" %%% "cats-effect-cps"   % "0.4.0",
  "org.typelevel" %%% "cats-parse"        % "1.0.0",
  "org.typelevel" %%% "munit-cats-effect" % "2.0.0-M4" % "test"
)
