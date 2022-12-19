scalaVersion := "2.13.10"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ScalaLab",
    description := "ScalaLab",
  maxErrors := 10)

    scalacOptions := Seq(
  // "-classpath", "foo:bar:...",         // Add to the classpath.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-deprecation"                      // Emit warning and location for usages of deprecated APIs.
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
   

