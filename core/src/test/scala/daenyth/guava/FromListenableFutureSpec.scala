/*
 * Copyright 2023 Daenyth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package daenyth.guava
package kibbles.guava

import cats.effect.IO
import cats.effect.testkit.TestControl
import com.google.common.util.concurrent.{AbstractFuture, ListenableFuture, MoreExecutors}

import java.util.concurrent.{CancellationException, Executors}
import scala.concurrent.duration._
import scala.util.control.NoStackTrace

class GuavaSpec extends munit.CatsEffectSuite {

  private val lfService =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1))

  test("fromListenableFuture signals success") {

    def lf() = lfService.submit(() => true)

    fromListenableFuture(IO(lf())).assert
  }

  test("fromListenableFuture signals thrown failures") {
    case object TestException extends Exception with NoStackTrace
    def lf() = lfService.submit[Unit](() => throw TestException)
    fromListenableFuture(IO(lf())).intercept[TestException.type]
  }

  // Styled after https://github.com/typelevel/cats-effect/pull/2665/files#diff-d68e0c39ae977e5c8643174d66c7659ebfb6bc5f3f6e19a581b7aefbfc6024bd
  test("fromListenableFuture signals cancellation") {

    lazy val lf = lfService.submit[Unit](() => Thread.sleep(200))

    val test = for {

      fiber <- fromListenableFuture(IO(lf)).start
      _ <- smallDelay // time for the callback to be set up
      _ <- fiber.cancel
      _ <- IO(lf.get()).intercept[CancellationException]
    } yield true

    test.assert
  }

  test("fromListenableFuture propagates backpressure from cancellation") {

    def lf: ListenableFuture[Unit] = new AbstractFuture[Unit] {
      override def cancel(mayInterruptIfRunning: Boolean): Boolean = false
    }

    val io = for {
      fiber <- fromListenableFuture(IO(lf)).start
      _ <- smallDelay // time for the callback to be set-up
      _ <- fiber.cancel
    } yield ()

    TestControl.executeEmbed(io).intercept[TestControl.NonTerminationException]

  }

  private def smallDelay: IO[Unit] = IO.sleep(100.millis)

}
