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

import cats.effect.Async
import cats.syntax.all._
import com.google.common.util.concurrent.ListenableFuture

import java.util.concurrent.Executor
import scala.concurrent.ExecutionException
import scala.util.control.NonFatal

package object guava {

  def fromListenableFuture[F[_], A](
      lfF: F[ListenableFuture[A]]
  )(implicit F: Async[F]): F[A] =
    F.async { cb =>
      F.executionContext.flatMap { ec =>
        lfF.flatMap { lf =>
          val executor: Executor = (command: Runnable) => ec.execute(command)
          F.delay {
            lf.addListener(
              () =>
                cb(
                  try Right(lf.get)
                  catch {
                    case ee: ExecutionException if ee.getCause != null =>
                      Left(ee.getCause)
                    case NonFatal(e) => Left(e)
                  }
                ),
              executor
            )

            Some(F.delay(lf.cancel(false)).flatMap {
              case true  => F.unit
              case false =>
                // failed to cancel - block until completion
                F.async_[Unit] { cb =>
                  lf.addListener(() => cb(Right(())), executor)
                }
            })
          }
        }
      }

    }
}
