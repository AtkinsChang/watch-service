lazy val root = (project in file("."))
  .aggregate(`macosx-watch-service`)
  .settings(
    name := "watch-service-parent",
    Settings.rootSettings,
    publish := {},
    publishLocal := {}
  )

lazy val `macosx-watch-service` = project
  .enablePlugins(Javah)
  .settings(
    Settings.projectSettings,
    Settings.compileNativeSettings,
    crossPaths := false,
    autoScalaLibrary := false
  )
