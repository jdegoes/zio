package zio

object Example extends ZIOAppDefault {

  def jobForever(r: Ref[Long]): Task[Unit] = {
  val eff = for {
    f <- ZIO
           .foreachParDiscard(Seq(1, 2, 3)) { id => // replacing to foreachDiscard fix the issue
             ZIO.succeed(id)
           }
           .fork // or removing fork fix the issue
    _ <- f.join
    _ <- r.update(_ + 1)
  } yield ()

  eff.forever
}

def program(): Task[Unit] =
  for {
    ref <- Ref.make[Long](0)
    _ <- jobForever(ref).onError { exit =>
           ZIO.logErrorCause("exit with cause", exit)
         }.catchAllCause(_ => ZIO.unit)
    count <- ref.get
    _     <- ZIO.debug(s"survives $count loops")
  } yield ()

  val run =
    program()
}