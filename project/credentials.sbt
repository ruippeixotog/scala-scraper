credentials ++= {
  val sonatypeUser = System.getenv("SONATYPE_USERNAME")
  val sonatypePass = System.getenv("SONATYPE_PASSWORD")

  if (sonatypeUser == null || sonatypePass == null) Nil
  else Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", sonatypeUser, sonatypePass))
}
