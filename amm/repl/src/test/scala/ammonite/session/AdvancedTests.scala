package ammonite.session

import ammonite.TestUtils._
import ammonite.DualTestRepl
import ammonite.util.{Res, Util}
import utest._


object AdvancedTests extends TestSuite{
  val tests = Tests{
    println("AdvancedTests")
    val check = new DualTestRepl()
    test("pprint"){
      check.session(s"""
        @ Seq.fill(10)(Seq.fill(3)("Foo"))
        res0: Seq[Seq[String]] = List(
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo"),
          List("Foo", "Foo", "Foo")
        )

        @ case class Foo(i: Int, s0: String, s1: Seq[String])
        defined class Foo

        @ Foo(1, "", Nil)
        res2: Foo = Foo(1, "", List())

        @ Foo(
        @   1234567,
        @   "I am a cow, hear me moo",
        @   Seq("I weigh twice as much as you", "and I look good on the barbecue")
        @ )
        res3: Foo = Foo(
          1234567,
          "I am a cow, hear me moo",
          List("I weigh twice as much as you", "and I look good on the barbecue")
        )
      """)
    }

    test("exit"){
      check.result("exit", Res.Exit())
    }
    test("skip"){
      check.result("", Res.Skip)
    }

    test("predef"){
      val check2 = new DualTestRepl{
        override def predef = (
          """
          import math.abs
          val x = 1
          val y = "2"
          """,
          None
        )
      }
      check2.session("""
        @ -x
        res0: Int = -1

        @ y
        res1: String = "2"

        @ x + y
        res2: String = "12"

        @ abs(-x)
        res3: Int = 1
      """)

    }
    test("predefSettings"){
      val check2 = new DualTestRepl{
        override def predef = (
          """
          interp.configureCompiler(_.settings.Xexperimental.value = true)
          """,
          None
        )
      }
      check2.session("""
        @ repl.compiler.settings.Xexperimental.value
        res0: Boolean = true
      """)

    }
    test("macros"){
      check.session("""
        @ import language.experimental.macros

        @ import reflect.macros.Context

        @ object Macro {
        @   def impl(c: Context): c.Expr[String] = {
        @    import c.universe._
        @    c.Expr[String](Literal(Constant("Hello!")))
        @   }
        @ }
        defined object Macro

        @ def m: String = macro Macro.impl
        defined function m

        @ m
        res4: String = "Hello!"
      """)
    }
    test("typeScope"){
      // Fancy type-printing isn't implemented at all in 2.10.x
      check.session("""
        @ collection.mutable.Buffer(1)
        res0: collection.mutable.Buffer[Int] = ArrayBuffer(1)

        @ import collection.mutable

        @ collection.mutable.Buffer(1)
        res2: mutable.Buffer[Int] = ArrayBuffer(1)

        @ mutable.Buffer(1)
        res3: mutable.Buffer[Int] = ArrayBuffer(1)

        @ import collection.mutable.Buffer

        @ mutable.Buffer(1)
        res5: Buffer[Int] = ArrayBuffer(1)
      """)
    }
    test("customTypePrinter"){
      check.session("""
        @ Array(1)
        res0: Array[Int] = Array(1)

        @ import pprint.TPrint

        @ implicit def ArrayTPrint[T: TPrint]: TPrint[Array[T]] = TPrint.lambda( c =>
        @   implicitly[TPrint[T]].render(c) +
        @   " " +
        @   c.typeColor("Array").render
        @ )

        @ Array(1)
        res3: Int Array = Array(1)
      """)
    }
    test("trappedType"){
      check.session("""
        @ val nope = ammonite.Nope(2); val n = 2
        n: Int = 2
      """)
    }
    test("unwrapping"){
      check.session("""
        @ {
        @   val x = 1
        @   val y = 2
        @   x + y
        @ }
        x: Int = 1
        y: Int = 2
        res0_2: Int = 3
      """)
    }
    test("forceWrapping"){
      check.session("""
        @ {{
        @   val x = 1
        @   val y = 2
        @   x + y
        @ }}
        res0: Int = 3
      """)
    }
    test("truncation"){
      // Need a way to capture stdout in tests to make these tests work
      if(false) check.session("""
        @ Seq.fill(20)(100)
        res0: Seq[Int] = List(
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
        ...

        @ show(Seq.fill(20)(100))
        res1: ammonite.pprint.Show[Seq[Int]] = List(
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100,
          100
        )

        @ show(Seq.fill(20)(100), height = 3)
        res2: ammonite.pprint.Show[Seq[Int]] = List(
          100,
          100,
        ...

        @ repl.pprinter() = repl.pprinter().copy(defaultHeight = 5)

        @ Seq.fill(20)(100)
        res4: Seq[Int] = List(
          100,
          100,
          100,
          100,
        ...
      """)
    }
    test("private"){
      test("vals") - check.session("""
        @ private val x = 1; val y = x + 1
        y: Int = 2

        @ y
        res1: Int = 2

        @ x
        error: not found: value x

        @ {
        @ private[this] val a = 3
        @ val b = a * 4
        @ }

        @ a
        error: not found: value a

        @ b
        
      """)

      test("dontPrint"){
        check.session(
          """
          @ private object Foo { def get = "a" }; val s = Foo.get
          s: String = "a"

          @ private class Foo { def get = "a" }; val s = (new Foo).get
          s: String = "a"

          @ private trait Foo { def get = "a" }; val s = (new Foo {}).get
          s: String = "a"

          @ private def foo(): String = "a"; val s = foo()
          s: String = "a"

          @ private lazy val foo: String = "a"; val s = foo
          s: String = "a"

          @ private val foo: String = "a"; val s = foo
          s: String = "a"

          @ private type T = String; private def foo(): T = "a"; val s: String = foo()
          s: String = "a"
        """)
      }
    }
    test("compilerPlugin") - retry(3){
      if (scala2_11) check.session("""
        @ // Compiler plugins imported without `.$plugin` are not loaded

        @ import $ivy.`org.spire-math::kind-projector:0.6.3`

        @ trait TC0[F[_]]
        defined trait TC0

        @ type TC0EitherStr = TC0[Either[String, ?]]
        error: not found: type ?

        @ // You need to use `import $plugin.$ivy`

        @ import $plugin.$ivy.`org.spire-math::kind-projector:0.6.3`

        @ trait TC[F[_]]
        defined trait TC

        @ type TCEitherStr = TC[Either[String, ?]]
        defined type TCEitherStr

        @ // Importing plugins doesn't affect the run-time classpath

        @ import $plugin.$ivy.`com.lihaoyi::scalatags:0.7.0`

        @ import scalatags.Text
        error: not found: value scalatags
      """)
    }
    test("replApiUniqueness"){
      // Make sure we can instantiate multiple copies of Interpreter, with each
      // one getting its own `ReplBridge`. This ensures that the various
      // Interpreters are properly encapsulated and don't interfere with each
      // other.
      val c1 = new DualTestRepl()
      val c2 = new DualTestRepl()
      c1.session("""
        @ repl.prompt() = "A"
      """)
      c2.session("""
        @ repl.prompt() = "B"
      """)
      c1.session("""
        @ assert(repl.prompt() == "A")
      """)
      c2.session("""
        @ assert(repl.prompt() == "B")
      """)
    }
    test("macroParadiseWorks"){
      // no more macroparadise in 2.13
      if (scala2_11 || scala2_12) {
        val scalaVersion: String = scala.util.Properties.versionNumberString
        val c1: DualTestRepl = new DualTestRepl()
        c1.session(s"""
          @ interp.load.plugin.ivy("org.scalamacros" % "paradise_${scalaVersion}" % "2.1.0")
        """)
        c1.session("""
          @ val x = 1
        """)
      }
    }
    test("desugar"){
      check.session("""
        @ desugar{1 + 2 max 3}
        res0: Desugared = scala.Predef.intWrapper(3).max(3)
      """)
    }
    test("loadingModulesInPredef"){

      val dir = os.pwd/'amm/'src/'test/'resources/'scripts/'predefWithLoad
      test("loadExec"){
        val c1 = new DualTestRepl() {
          override def predef = (
            os.read(dir/"PredefLoadExec.sc"),
            Some(dir/"PredefLoadExec.sc")
          )
        }
        c1.session("""
          @ val previouslyLoaded = predefDefinedValue
          previouslyLoaded: Int = 1337
        """)
      }
      test("loadModule"){
        val c2 = new DualTestRepl(){
          override def predef = (
            os.read(dir/"PredefLoadModule.sc"),
            Some(dir/"PredefLoadModule.sc")
          )
        }
        c2.session("""
          @ val previouslyLoaded = predefDefinedValue
          previouslyLoaded: Int = 1337
        """)
      }
      test("importIvy"){
        val c2 = new DualTestRepl(){
          override def predef = (
            os.read(dir/"PredefMagicImport.sc"),
            Some(dir/"PredefMagicImport.sc")
          )
        }
        c2.session("""
          @ val previouslyLoaded = predefDefinedValue
          previouslyLoaded: Int = 1337

          @ val loadedDirect = Loaded.loadedDefinedValue
          loadedDirect: Int = 1337
        """)
      }
    }
    test("bytecodeForReplClasses"){
      check.session("""
        @ case class Child(name: String)

        @ val cls = classOf[Child]

        @ val resName = cls.getName.replace('.', '/') + ".class"

        @ cls.getClassLoader.getResource(resName) != null
        res3: Boolean = true

        @ cls.getClassLoader.getResourceAsStream(resName) != null
        res4: Boolean = true
      """)
    }
    test("customBridge"){
      check.session("""
        @ val s = test.message
        s: String = "ba"
      """)
    }

    test("dontRefreshCompiler"){
      test{
        // Conditional check due to https://github.com/scala/bug/issues/11564
        if (scala.util.Properties.versionNumberString != "2.13.0") check.session("""
          @ val c1 = repl.compiler

          @ val n = 2
          n: Int = 2

          @ val c2 = repl.compiler

          @ import scala.collection.mutable.ListBuffer
          import scala.collection.mutable.ListBuffer

          @ val c3 = repl.compiler

          @ assert(c1 eq c2)

          @ assert(c1 eq c3)
        """)
      }

      test("preconfigured"){
        // Conditional check due to https://github.com/scala/bug/issues/11564
        if (scala.util.Properties.versionNumberString != "2.13.0") check.session("""
          @ val c0 = repl.compiler

          @ interp.preConfigureCompiler(_ => ())

          @ val c1 = repl.compiler

          @ val n = 2
          n: Int = 2

          @ val c2 = repl.compiler

          @ import scala.collection.mutable.ListBuffer
          import scala.collection.mutable.ListBuffer

          @ val c3 = repl.compiler

          @ assert(c0 ne c1)

          @ assert(c1 eq c2)

          @ assert(c1 eq c3)
        """)
      }
    }

    test("loadURL"){
      val sbv = {
        val sv = scala.util.Properties.versionNumberString
        if (sv.forall(c => c.isDigit || c == '.'))
          sv.split('.').take(2).mkString(".")
        else
          sv
      }
      val url = "https://repo1.maven.org/maven2/" +
        s"org/scalacheck/scalacheck_$sbv/1.14.0/scalacheck_$sbv-1.14.0.jar"
      check.session(s"""
        @ interp.load.cp(new java.net.URL("$url"))

        @ import org.scalacheck.Gen
        import org.scalacheck.Gen

        @ val check = Gen.choose(1, 5).sample.exists(_ <= 5)
        check: Boolean = true
      """)
    }

    test("accessPressy"){
      check.session("""
        @ def typeAt(code: String, pos: Int) = {
        @   import scala.tools.nsc.interactive.Response
        @   import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition}
        @   val c = repl.interactiveCompiler
        @   val f = new BatchSourceFile("<virtual>", code)
        @   val r = new Response[Unit]
        @   c.askReload(List(f), r)
        @   r.get.fold(x => x, e => throw e)
        @   val r0 = new Response[c.Tree]
        @   c.askTypeAt(new OffsetPosition(f, pos), r0)
        @   r0.get.fold(x => x, e => throw e)
        @ }
        defined function typeAt

        @ val code = "object A { val l = List }"
        code: String = "object A { val l = List }"

        @ val t = typeAt(code, code.length - 2).toString
        t: String = ?

        @ assert(t.endsWith(".List"))
      """)
    }
  }
}