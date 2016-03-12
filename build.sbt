// Set the project name to the string 'My Project'
name := "SBTProject"

// The := method used in Name and Version is one of two fundamental methods.
// The other method is <<=
// All other initialization methods are implemented in terms of these.
version := "1.0"

scalaVersion := "2.11.7"

assemblyJarName in assembly := "edgarretriever.jar"

mainClass in assembly := Some("EdgarActorRunner")

// Add a single dependency
libraryDependencies += "junit" % "junit" % "4.8" % "test"
libraryDependencies += "com.netflix.rxjava" % "rxjava-scala" % "0.19.1"
libraryDependencies += "org.scala-lang" % "scala-swing" % "2.11+"
libraryDependencies += "commons-io" % "commons-io" % "2.4"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.5"
libraryDependencies += "commons-net" % "commons-net" % "3.4"
libraryDependencies += "org.scalamock" % "scalamock-core_2.11" % "3.2.2" % "test"
libraryDependencies += "org.scalamock" % "scalamock-scalatest-support_2.11" % "3.2.2" % "test"
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.3"
libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.3" % "test"
libraryDependencies += "org.mockito" % "mockito-core" % "1.9.5"
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
                            "org.slf4j" % "slf4j-simple" % "1.7.5",
                            "org.clapper" %% "grizzled-slf4j" % "1.0.2")
libraryDependencies += "me.lessis" %% "courier" % "0.1.3"
libraryDependencies += "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

