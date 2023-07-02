// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.1" // your current series x.y

ThisBuild / organization := "io.github.daenyth"
ThisBuild / organizationName := "Daenyth"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("daenyth", "Gavin Bisesi")
)
// Not apache license, horrible file headers go away
ThisBuild / tlCiHeaderCheck := false

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

val Scala213 = "2.13.11"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.3.0", "2.12.18")
ThisBuild / scalaVersion := Scala213 // the default Scala

val catsEffectV = "3.5.1"

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "cats-effect-guava",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.9.0",
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "org.typelevel" %%% "cats-effect-testkit" % catsEffectV % Test,
      "com.google.guava" % "guava" % "32.0.1-jre",
      "org.scalameta" %%% "munit" % "0.7.29" % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % "1.0.7" % Test
    )
  )

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    description := "Cats-effect integration with guava ListenableFuture",
    laikaTheme := {
      import laika.helium.config._
      tlSiteHelium.value.site
        .mainNavigation(appendLinks =
          Seq(
            ThemeNavigationSection(
              "Related Projects",
              TextLink.external("https://typelevel.org/cats-effect/", "cats-effect"),
              TextLink.external("https://guava.dev/", "Google guava")
            )
          )
        )
        .build
    }
  )
