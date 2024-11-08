// import smithy4s.codegen.Smithy4sCodegenPlugin

ThisBuild / scalaVersion := "3.5.2"

val Http4sVersion = "0.23.27"
val CirceVersion = "0.14.10"

lazy val m = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  // .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalacOptions += "-Xkind-projector",
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    libraryDependencies ++= Seq(
        "io.circe" %%% "circe-core" % CirceVersion,
        "io.circe" %%% "circe-generic" % CirceVersion,
        "io.circe" %%% "circe-parser" % CirceVersion,
        "com.lihaoyi" %%% "scalatags" % "0.13.1",
    ),
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
    )
  )
  .jvmSettings(
      // Compile / run / fork := true,
    version := "0.1.0-SNAPSHOT",
    // assembly / mainClass := Some("RadServer"),
    resolvers += "jitpack"  at "https://jitpack.io",
    libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-ember-server" % Http4sVersion,
        "org.http4s" %% "http4s-ember-client" % Http4sVersion,
        "org.http4s" %% "http4s-dsl" % Http4sVersion,
        "org.http4s" %% "http4s-circe" % Http4sVersion,
        "org.http4s" %% "http4s-scalatags" % "0.25.2",
        "com.lihaoyi" %% "os-lib" % "0.10.7",
        // "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
        // "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
    ),
  )
