import Commons._
import Dependencies._
import Environment._
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import fusion.sbt.gen.BuildInfo
import sbtassembly.PathList

ThisBuild / buildEnv := {
  sys.props
    .get("build.env")
    .orElse(sys.env.get("BUILD_ENV"))
    .flatMap {
      case "prod"  => Some(BuildEnv.Production)
      case "stage" => Some(BuildEnv.Stage)
      case "test"  => Some(BuildEnv.Test)
      case "dev"   => Some(BuildEnv.Developement)
      case _       => None
    }
    .getOrElse(BuildEnv.Developement)
}

ThisBuild / scalaVersion := Dependencies.versionScala

ThisBuild / scalafmtOnCompile := true

ThisBuild / resolvers ++= Seq(Resolver.bintrayRepo("akka-fusion", "maven"), Resolver.jcenterRepo)

lazy val root = Project(id = "mass-data-root", base = file("."))
  .aggregate(
    example,
    massDocs,
    massFunctest,
    massRdi,
    massRdiCli,
    massRdiCore,
    massConnector,
    massGovernance,
    massJob,
    massApiService,
    massConsole,
    massAuth,
    massCoreExt,
    massCore,
    massCommon)
  .settings(Environment.settings: _*)

lazy val massDocs = _project("mass-docs")
  .dependsOn(
    massFunctest,
    massRdi,
    massRdiCli,
    massRdiCore,
    massConnector,
    massGovernance,
    massApiService,
    massConsole,
    massAuth,
    massCoreExt % "compile->compile;test->test",
    massCore % "compile->compile;test->test",
    massCommon)
  .enablePlugins(AkkaParadoxPlugin)
  .settings(
    name in (Compile, paradox) := "massData",
    paradoxProperties ++= Map(
        "github.base_url" -> s"https://github.com/yangbajing/mass-data/tree/${version.value}",
        "scala.version" -> scalaVersion.value,
        "scala.binary_version" -> scalaBinaryVersion.value,
        "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/${BuildInfo.versionAkka}",
        "akka.version" -> BuildInfo.versionAkka))

lazy val example = _project("example")
  .dependsOn(massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_fusionCluster))

lazy val massFunctest = _project("mass-functest")
  .dependsOn(massConsole, massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .settings(
    jvmOptions in MultiJvm := Seq("-Xmx1024M"),
    libraryDependencies ++= Seq(_akkaClusterMetrics, _akkaMultiNodeTestkit % Test))

// API Service
lazy val massApiService = _project("mass-api-service")
  .dependsOn(massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging)
  .settings(Packaging.settings: _*)
  .settings(mainClass in Compile := Some("mass.apiservice.boot.ApiServiceMain"), libraryDependencies ++= Seq())

// 数据治理
lazy val massGovernance = _project("mass-governance")
  .dependsOn(massConnector, massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging)
  .settings(Packaging.settings: _*)
  .settings(mainClass in Compile := Some("mass.governance.boot.GovernanceMain"), libraryDependencies ++= Seq())

// 监查、控制、管理
lazy val massConsole = _project("mass-console")
  .dependsOn(massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging)
  .settings(Packaging.settings: _*)
  .settings(mainClass in Compile := Some("mass.console.boot.ConsoleMain"), libraryDependencies ++= Seq())

// Reactive Data Integration Console
lazy val massRdiConsole = _project("mass-rdi-console")
  .dependsOn(massRdiCore, massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .settings(
    mainClass in Compile := Some("mass.rdi.console.boot.RdiConsoleMain"),
    libraryDependencies ++= Seq(_quartz) ++ _pois)

// Reactive Data Integration 反应式数据处理流工具
lazy val massRdi = _project("mass-rdi")
  .dependsOn(
    massCoreExt,
    massRdiCore,
    massCoreExt % "compile->compile;test->test",
    massCore % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging)
  .settings(Packaging.settings: _*)
  .settings(mainClass in Compile := Some("mass.rdi.boot.RdiMain"), libraryDependencies ++= Seq(_quartz) ++ _pois)

// Reactive Data Integration Cli
lazy val massRdiCli = _project("mass-rdi-cli")
  .dependsOn(massRdiCore, massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .settings(
    //    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    assemblyJarName in assembly := "rdi.jar",
    mainClass in Compile := Some("mass.rdi.cli.boot.RdiCliMain"),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "aop.xml") =>
        Packaging.aopMerge
      case s =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(s)
    },
    libraryDependencies ++= Seq(_scopt))

lazy val massRdiCore = _project("mass-rdi-core")
  .dependsOn(massConnector, massCore % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_commonsVfs, _quartz % Provided))

// mass调度任务程序.
lazy val massJob = _project("mass-job")
  .dependsOn(massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging, JavaAgent, MultiJvmPlugin)
  .configs(MultiJvm)
  .settings(Packaging.settings: _*)
  .settings(mainClass in Compile := Some("mass.job.boot.JobMain"), libraryDependencies ++= Seq())

// 统一用户，OAuth 2服务
lazy val massAuth = _project("mass-auth")
  .dependsOn(massCoreExt % "compile->compile;test->test", massCore % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging)
  .settings(Packaging.settings: _*)
  .settings(mainClass in Compile := Some("mass.auth.boot.AuthMain"), libraryDependencies ++= Seq())

// 数据组件（采集、存储）
lazy val massConnector = _project("mass-connector")
  .dependsOn(massCore % "compile->compile;test->test")
  .settings(
    mainClass in Compile := Some("mass.connector.boot.ConnectorMain"),
    libraryDependencies ++= Seq(_akkaStreamKafka, _mssql, _mysql, _postgresql) ++ _alpakkas ++ _alpakkaNoSQLs)

// 管理程序公共库
lazy val massCoreExt = _project("mass-core-ext")
  .settings(Publishing.publishing: _*)
  .dependsOn(massCore % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_quartz, _jsch, _fusionCluster, _akkaMultiNodeTestkit % Test) ++ _slicks)

lazy val massCore =
  _project("mass-core")
    .dependsOn(massCommon % "compile->compile;test->test")
    .enablePlugins(AkkaGrpcPlugin)
    .settings(Publishing.publishing: _*)
    .settings(libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
        _scalaXml,
        _h2,
        _fusionJdbc,
        _fusionProtobufV3,
        _postgresql % Test,
        _quartz % Provided) ++ _akkaHttps)

lazy val massCommon = _project("mass-common")
  .settings(Publishing.publishing: _*)
  .settings(libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "provided",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      _akkaSerializationJackson,
      _fusionCore))

def _project(name: String, _base: String = null) =
  Project(id = name, base = file(if (_base eq null) name else _base))
    .settings(basicSettings: _*)
    .settings(skip in publish := true, libraryDependencies ++= Seq(_fusionTestkit % Test))
