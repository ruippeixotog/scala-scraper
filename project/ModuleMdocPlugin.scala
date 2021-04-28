import mdoc.MdocPlugin
import mdoc.MdocPlugin.autoImport._
import sbt.Keys._
import sbt._

/** A plugin that generates a synthetic SBT project for documentation for each module it is enabled on. The generated
  * SBT projects depend on the original project and also have a hardcoded dependency on "generic" in order to provide
  * support for auto-derivation in documentation.
  *
  * This is needed because unlike tut there is no separate classpath configuration for documentation. See
  * https://github.com/scalameta/mdoc/issues/155 for details.
  */
object ModuleMdocPlugin extends AutoPlugin {

  override def projectSettings: Seq[Setting[_]] = Seq(
    // format: off
    mdocIn := baseDirectory.value / "docs",
    mdocOut := baseDirectory.value,
    // format: on
  )

  override def derivedProjects(proj: ProjectDefinition[_]): Seq[Project] = {
    val moduleProj = LocalProject(proj.id)
    val docProjId = s"${proj.id}-docs"
    val docProjRoot = proj.base / "target" / "docs-project"

    val docProj =
      Project(docProjId, docProjRoot)
        .enablePlugins(MdocPlugin)
        .dependsOn(moduleProj)
        .settings(
          // format: off
          name := docProjId,

          mdocIn := (moduleProj / mdocIn).value,
          mdocOut := (moduleProj / mdocOut).value,
          mdocExtraArguments += "--no-link-hygiene",
          mdocVariables := Map("VERSION" -> latestRelease),

          scalacOptions += "-Ypartial-unification",

          publish / skip := true
          // format: on
        )

    List(docProj)
  }

  val changelogVersionRegex = "^### ([^\\s]+)".r

  lazy val latestRelease: String =
    IO.readLines(file("CHANGELOG.md")).flatMap(changelogVersionRegex.findFirstMatchIn).head.group(1)
}
