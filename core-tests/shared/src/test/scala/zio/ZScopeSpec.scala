package zio

import zio.test.Assertion._
import zio.test._

object ZScopeSpec extends ZIOBaseSpec {

  def testScope[A](label: String, a: A)(f: (Ref[A], ZScope[Any, Unit]) => UIO[A]) =
    testM(label) {
      for {
        ref      <- Ref.make[A](a)
        open     <- ZScope.make[Any, Unit](false)
        expected <- f(ref, open.scope)
        value    <- open.close(())
        actual   <- ref.get
      } yield assert(value)(isTrue) && assert(actual)(equalTo(expected))
    }

  def spec = suite("ZScopeSpec")(
    testM("make returns an empty and open scope") {
      for {
        open  <- ZScope.make[Any, Unit](false)
        empty <- open.scope.empty
        value <- open.scope.closed
      } yield assert(empty)(isTrue) && assert(value)(isFalse)
    },
    testM("close makes the scope closed") {
      for {
        open  <- ZScope.make[Any, Unit](false)
        _     <- open.close(())
        value <- open.scope.closed
      } yield assert(value)(isTrue)
    },
    testM("close can be called multiple times") {
      for {
        open  <- ZScope.make[Any, Unit](false)
        _     <- open.close(()).repeat(Schedule.recurs(10))
        value <- open.scope.closed
      } yield assert(value)(isTrue)
    },
    testM("ensure makes the scope non-empty") {
      for {
        open  <- ZScope.make[Any, Unit](false)
        value <- open.scope.ensure(_ => IO.unit)
        empty <- open.scope.empty
      } yield assert(empty)(isFalse) && assert(value)(isTrue)
    },
    testM("ensure on closed scope returns false") {
      for {
        open  <- ZScope.make[Any, Unit](false)
        _     <- open.close(())
        value <- open.scope.ensure(_ => IO.unit)
        empty <- open.scope.empty
      } yield assert(empty)(isTrue) && assert(value)(isFalse)
    },
    testM("ensure cannot add a finalizer with the same key") {
      for {
        open   <- ZScope.make[Any, Unit](false)
        first  <- open.scope.ensure("foo", _ => IO.unit)
        second <- open.scope.ensure("foo", _ => IO.unit)
      } yield assert(first)(isTrue) && assert(second)(isFalse)
    },
    testScope("one finalizer", 0)((ref, scope) => scope.ensure("first", _ => ref.update(_ + 1)) as 1),
    testScope("two finalizers in order", List.empty[String]) { (ref, scope) =>
      scope.ensure(_ => ref.update(_ :+ "foo")) *>
        scope.ensure(_ => ref.update(_ :+ "bar")) as (List("foo", "bar"))
    },
    testScope("100 finalizers in order", List.empty[String]) { (ref, scope) =>
      val range = 0 to 100

      val expected = 
        range.foldLeft(List.empty[String]) { 
          case (acc, int) => int.toString :: acc
        }.reverse

      val effect = 
        range.foldLeft(IO.unit) {
          case (acc, int) => acc *> ref.update(_ :+ int.toString).unit
        }

      scope.ensure(_ => effect) as expected
    },
    testM("scope extension") {
      for {
        ref    <- Ref.make(0)
        parent <- ZScope.make[Any, Unit](false)
        child  <- ZScope.make[Any, Unit](false).tap(_.scope.ensure(_ => ref.update(_ + 1)))
        _      <- parent.scope.extend(child.scope)
        _      <- child.close(())
        before <- ref.get 
        _      <- parent.close(())
        after  <- ref.get
      } yield assert(before)(equalTo(0)) && assert(after)(equalTo(1))
    }
  )
}
