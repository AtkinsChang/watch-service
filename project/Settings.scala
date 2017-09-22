import sbt.Keys._
import sbt._
import sbt.internal.io.Source

import scala.sys.process.Process

object Settings {

  def rootSettings: Seq[Setting[_]] = inThisBuild(Seq(
    organization := "edu.nccu.plsm.watchservice",
    version := "0.0.1-SNAPSHOT",
    description := "Watch Service",
    resolvers ++= Resolver.mavenLocal +: DefaultOptions.resolvers(snapshot = false)
  ))

  def commonSettings: Seq[Setting[_]] = Seq[SettingsDefinition](
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq("2.12.3"),
    scalaModuleInfo ~= {
      _.map(_.withOverrideScalaVersion(true))
    },

    fork := true,
    cancelable in Global := true,
    javaOptions in run ++= Seq(
      "-ea",
      "-Xcheck:jni"
    ),

    testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
    publishArtifact in Test := false,

    updateOptions ~= {
      _.withCachedResolution(true)
    },
    logBuffered := false,

    publishMavenStyle := true
  ) flatMap (_.settings)

  private[this] def compilerSettings: Seq[Setting[_]] = Seq(
    scalacOptions in compile ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-opt:l:inline",
      "-opt-inline-from:**:!java.**:!javax.**:!jdk.**:!apple.**:!sun.**:!com.apple.**:!com.oracle.**:!com.sun.**",
      "-opt-warnings:_",
      "-target:jvm-1.8",
      "-unchecked"
    ),
    scalacOptions in compile ++= Seq(
      "-Xlint:_",
      "-Xverify"
    ),
    scalacOptions in compile ++= Seq(
      "-Ywarn-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-value-discard"
    ),
    javacOptions in compile ++= Seq(
      "-source", "1.8",
      "-target", "1.8",
      "-encoding", "UTF-8",
      "-Xlint:all",
      "-XDignore.symbol.file",
      "-g",
      "-deprecation"
    )
  )

  val compileNative: TaskKey[Seq[File]] = TaskKey[Seq[File]]("compile-native", "Compile native code")

  def compileNativeSettings: Seq[Def.Setting[_]] = inConfig(Compile)(Seq(
    compileNative := {
      val source = sourceDirectory.value / "native"
      val s = streams.value
      if (source.exists()) {
        val cache = s.cacheDirectory / "cmake"
        if (cache.exists()) {
          IO.delete(cache.listFiles())
        } else {
          IO.createDirectory(cache)
        }
        val target = crossTarget.value / "native" / Defaults.nameForSrc(configuration.value.name)
        val absTargetPath = target.absolutePath
        val command = Seq(
          "cmake",
          s"-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=$absTargetPath",
          s"-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=$absTargetPath",
          s"-DCMAKE_ARCHIVE_OUTPUT_DIRECTORY=$absTargetPath",
          "-DCMAKE_BUILD_TYPE=Release",
          source.absolutePath,
        )
        val code = Process(command, cache) ! s.log
        if (code == 0) {
          val buildCommand = Seq(
            "cmake",
            "--build", ".",
            "--clean-first",
            "--",
            s"-j${sys.runtime.availableProcessors}"
          )
          val code = Process(buildCommand, cache) ! s.log
          if (code == 0) {
            target.listFiles()
          } else {
            s.log.error(s"cmake build exit with code $code")
            Seq.empty
          }
        } else {
          s.log.error(s"cmake config exit with code $code")
          Seq.empty
        }
      } else {
        Seq.empty
      }
    },
    sources in compileNative := {
      (sourceDirectory.value / "native")
        .descendantsExcept(includeFilter.value, excludeFilter.value)
        .get
    },
    watchSources in Global += {
      new Source(sourceDirectory.value / "native", includeFilter.value, excludeFilter.value)
    },
    resourceGenerators += Def.task {
      val base = resourceManaged.value / "META-INF" / "native"
      compileNative.value.map { file =>
        val resource = base / file.getName
        IO.copyFile(file, resource)
        resource
      }
    }.taskValue
  ))

  def projectSettings: Seq[Setting[_]] = commonSettings ++ compilerSettings :+ (libraryDependencies ++= Dependencies.Testing.map(_ % Test))

}
