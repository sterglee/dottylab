val scala3 = "3.1.0"
lazy val root = project
  .in(file("."))
  .settings(
    name := "ScalaLab",
    description := "ScalaLab",
    version := s"$scala3-final",
    scalaVersion := scala3,
    organization := " ",
    organizationName := "ProgrammingScala",
    organizationHomepage := Some(url("http://programming-scala.org")),
    homepage := Some(url("https://github.com/deanwampler/programming-scala-book-code-examples/")),
    licenses += "Apache2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    maxErrors := 10,
    // At the time of publication, Scala 3 builds of Akka were not yet available.
    // Notice how the Scala 2.13-built libraries are used. For more information:
    // https://www.scala-lang.org/blog/2021/04/08/scala-3-in-sbt.html
   // libraryDependencies ++= Seq(
   //   "com.typesafe.akka"      %% "akka-actor-typed" % "2.6.14",
   //   "com.typesafe.akka"      %% "akka-slf4j"       % "2.6.14",
  //  ).map(dep => dep.cross(CrossVersion.for3Use2_13)) ++ Seq(
      // Libraries that already fully support Scala 3:
    //  "org.typelevel"          %% "cats-core"        % "2.6.1",
    //   "org.scala-lang.modules" %% "scala-parser-combinators" % "2.0.0",

//    ),

    // For Scala 3
// The -rewrite and -migration options are best used while migrating
// from Scala 2 to Scala 3, then removed.
// The default value for -source is 3.0. I'm using future to force more
// deprecation warnings for obsolete concepts that are being transitioned
// out. Use the default value if you are migrating from Scala 2!!
scalacOptions := Seq(
  // "-classpath", "foo:bar:...",         // Add to the classpath.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
 // "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
 // "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
 // "-explain",                          // Explain errors in more detail.
  // "-explain-types",                    // Explain type errors in more detail.
  // "-indent",                           // Together with -rewrite, remove {...} syntax when possible due to significant indentation.
 // "-no-indent",                        // Require classical {...} syntax, indentation is not significant.
 // "-new-syntax",                       // Require `then` and `do` in control expressions.
 // "-old-syntax",                       // Require `(...)` around conditions.
 //"-language:Scala2",                  // Compile Scala 2 code, highlight what needs updating
  // "-language:strictEquality",          // Require +derives Eql+ for using == or != comparisons
  // "-rewrite",                          // Attempt to fix code automatically. Use with -indent and ...-migration.
  // "-scalajs",                          // Compile in Scala.js mode (requires scalajs-library.jar on the classpath).
 // "-source:future",                       // Choices: future and future-migration. I use this to force future deprecation warnings, etc.
 // "-Xfatal-warnings",                  // Fail on warnings, not just errors
  // "-Xmigration",                       // Warn about constructs whose behavior may have changed since version.
  // "-Ysafe-init",                       // Warn on field access before initialization
  // "-Yexplicit-nulls",                  // For explicit nulls behavior.
),
Compile / console / scalacOptions := scalacOptions.value,
fork := true,
javaOptions ++= Seq(
  "-Duser.language=en_US"
),
javacOptions ++= Seq(
  "-Xlint:unchecked", "-Xlint:deprecation") // Java 8: "-Xdiags:verbose"),
)

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) => "ScalaLab.jar"
}


unmanagedJars in Compile ++= (file("./lib") * "*.jar").classpath

unmanagedJars in Compile ++= (file("./libScala3") * "*.jar").classpath


unmanagedJars in Compile ++= (file("./extralib") * "*.jar").classpath



exportJars := true



val dependentJarDirectory = settingKey[File]("location of the unpacked jars")
dependentJarDirectory := target.value / "dependent-jars"

val createDependentJarDirectory = taskKey[File]("create the dependent-jars directory")

createDependentJarDirectory :=  {
  sbt.IO.createDirectory(dependentJarDirectory.value)
  dependentJarDirectory.value
}
  
val excludes = List(".git",  "META-INF/*.SF",  "META-INF/*.DSA", "META-INF/*.RSA")
  
def unpackFilter(target: File) = new NameFilter {
    def accept(name: String) = {
    !excludes.exists( x => name.toLowerCase().startsWith(x)) && 
    !file(target.getAbsolutePath + "/" + name).exists
        }
 }
 
def unpack(target: File, f: File, log: Logger) = {
 log.debug("unpacking "+ f.getName)
 if (f.isDirectory) 
    sbt.IO.copyDirectory(f, target)
 else
    sbt.IO.unzip(f, target, filter = unpackFilter(target))
    }
    
def isLocal(f: File, base: File) = sbt.IO.relativize(base, f).isDefined


 def isValid(f:File, base:File) = true
 //{
 //   if ((isLocal(f, base)) && (f.getName.contains("openblas")==true))
 //      false
 //   else true
 //   }

def unpackJarSeq(files: Seq[File], target: File, base: File, local: Boolean, log: Logger) = {
 files.filter(f=> (local==isValid(f, base))  ).map(f=>  unpack(target, f, log))
 }
 
 val unpackJars = taskKey[Seq[_]]("unpacks a dependent jars into target/dependent-jars")
 
unpackJars := {
  val dir = createDependentJarDirectory.value
  val log = streams.value.log
  val bd = (baseDirectory in ThisBuild).value
  val classpathJars = Attributed.data((dependencyClasspath in Runtime).value)
  unpackJarSeq(classpathJars, dir, bd, true, log)
  }
  
val createUberJar = taskKey[File]("create jar which will run")
 
createUberJar := {
  val bd = (baseDirectory in ThisBuild).value
  val log = streams.value.log
  val output = target.value / "ScalaLab.jar"
  val classpathJars = Attributed.data((dependencyClasspath in Runtime).value)
  sbt.IO.withTemporaryDirectory( td => {
    unpackJarSeq(classpathJars, td, bd, true, log)
    create (dependentJarDirectory.value, td, (baseDirectory.value / "src/main/uber"), output)
    })
    output
  }
   
def create(depDir: File, localDir: File, extractDir: File, buildJar: File) = {
  def files(dir: File) = {
    val fs = (dir ** "*").get.filter(d => d != dir) // && d.fullpathname.endsWith("jar"))  )
    fs.map( x => (x, x.relativeTo(dir).get.getPath))
    }
    
   sbt.IO.zip(files(localDir) ++ files(depDir) ++ files(extractDir), buildJar)
   }
   /*
trait UberJarRunner {
  def start(): Unit
  def stop(): Unit
}
class MyUberJarRunner(uberJar: File) extends UberJarRunner {
  var p: Option[Process] = None
  def start(): Unit = {
    p = Some(Fork.java.fork(ForkOptions(),
             Seq("-cp", uberJar.getAbsolutePath, "Global")))
  }
  def stop(): Unit = p foreach (_.destroy())
}
*/
val runUberJar = taskKey[Int]("run the uber jar")
runUberJar := {
  val uberJar = createUberJar.value
  val options = ForkOptions()
  val arguments = Seq("-jar", uberJar.getAbsolutePath)
  Fork.java(options, arguments)
  }
  
  
 
 
val scala3LibsJars = new File("./libScala3").listFiles.filter(_.isFile)
     .filter(_.getName.endsWith(".jar")).toSeq
 
val scalalabLibsJars  = new File("./lib").listFiles.filter(_.isFile)
    .filter(_.getName.endsWith(".jar")).toSeq
    
val extraLibJars = new File("./extralib").listFiles.filter(_.isFile)
    .filter(_.getName.endsWith(".jar")).toSeq

val classPath =    scala3LibsJars  ++ scalalabLibsJars  ++ extraLibJars
    
    

packageOptions += Package.ManifestAttributes(
  "Class-Path" -> classPath.mkString(" "),
  "Main-Class" -> "scalaExec.scalaLab.scalaLab"
)
   


  
//packageOptions += Package.ManifestAttributes(

  //"Class-Path" ->  (Compile / dependencyClasspath).value.files.mkString(" "),
  //"Main-Class" -> "scalaExec.scalaLab.scalaLab"
//)
   

