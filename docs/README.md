## cats-effect-guava

### Install

This library is currently available for Scala binary versions 2.12, 2.13, and 3.3+.

To use the latest version, include the following in your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "io.github.daenyth" %% "cats-effect-guava" % "@VERSION@"
)
```

### Usage

```scala
import daenyth.guava._

def googleyMoogley(): ListenableFuture[String] = ???

def catsyMoogley: F[String] = Async[F].fromListenableFuture(Sync[F].delay(googleyMoogley()))
def ioMoogley: IO[String] = IO.fromListenableFuture(IO(googleyMoogley()))
```
