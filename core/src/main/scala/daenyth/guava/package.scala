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

package daenyth

import cats.effect.{Async, IO, MonadCancelThrow, Cont}
import cats.effect.syntax.all._
import cats.syntax.all._
import com.google.common.util.concurrent.ListenableFuture

import scala.concurrent.ExecutionException
import scala.util.control.NonFatal

package object guava {

  private[guava] def fromListenableFuture[F[_], A](
      lfF: F[ListenableFuture[A]]
  )(implicit F: Async[F]): F[A] = F.cont {
    new Cont[F, A, A] {
      def apply[G[_]](implicit G: MonadCancelThrow[G]) = { (resume, get, lift) =>
        G.uncancelable { poll =>
          G.flatMap(poll(lift(lfF))) { lf =>
            val go = F.executor.flatMap { executor =>
              F.delay {
                lf.addListener(
                  () =>
                    resume(
                      try Right(lf.get)
                      catch {
                        case ee: ExecutionException if ee.getCause != null =>
                          Left(ee.getCause)
                        case e if NonFatal(e) => Left(e)
                      }
                    ),
                  executor
                )
              }
            }

            val await = poll(get).onCancel(
              // if cannot cancel, fallback to get
              lift(F.delay(lf.cancel(false))).ifM(G.unit, get.void)
            )

            lift(go) *> await
          }
        }
      }
    }
  }

  implicit class AsyncOps[F[_]](val F: Async[F]) extends AnyVal {
    def fromListenableFuture[A](lfF: F[ListenableFuture[A]]): F[A] =
      guava.fromListenableFuture[F, A](lfF)(F)
  }

  implicit class IOOps(val io: IO.type) extends AnyVal {
    def fromListenableFuture[A](lfF: IO[ListenableFuture[A]]): IO[A] =
      guava.fromListenableFuture[IO, A](lfF)
  }
}
