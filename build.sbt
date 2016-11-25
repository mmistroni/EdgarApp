// Set the project name to the string 'My Project'
name := "EdgarRetriever"

version := "1.0"

scalaVersion := "2.11.5"
scalacOptions += "-target:jvm-1.7"


assembleArtifact in packageScala := true
assembleArtifact in packageDependency := true
assemblyJarName in assembly := "edgarretriever.jar"

mainClass in assembly :=   Some("EdgarActorRunner")

assemblyMergeStrategy in assembly := {
  case PathList("javax", "mail", xs @ _*)         => MergeStrategy.first
  case PathList("com", "sun", xs @ _*)         => MergeStrategy.first
  case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Add a single dependency
libraryDependencies += "junit" % "junit" % "4.8" % "test"
libraryDependencies += "org.scala-lang" % "scala-swing" % "2.11+"
libraryDependencies += "commons-io" % "commons-io" % "2.4"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.5"
libraryDependencies += "commons-net" % "commons-net" % "3.4"
libraryDependencies += "org.scalamock" % "scalamock-core_2.11" % "3.2.2" % "test"
libraryDependencies += "org.scalamock" % "scalamock-scalatest-support_2.11" % "3.2.2" % "test"
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.3"
libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.3" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.3"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"
libraryDependencies += "org.mockito" % "mockito-core" % "1.9.5" % "test"
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
                            "org.slf4j" % "slf4j-simple" % "1.7.5",
                            "org.clapper" %% "grizzled-slf4j" % "1.0.2")
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
libraryDependencies += "org.apache.commons" % "commons-email" % "1.4"
libraryDependencies += "org.powermock" % "powermock-mockito-release-full" % "1.5.4" % "test"
libraryDependencies += "org.apache.spark" %% "spark-core"   % "1.6.1" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-streaming"   % "1.6.1" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-mllib"   % "1.6.1"  % "provided"
libraryDependencies += "com.typesafe" % "config" % "1.2.1"
libraryDependencies += "com.mm" %% "sparkutilities" % "1.0"
resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

