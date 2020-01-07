/*
 * Copyright 2017-2020 John A. De Goes and the ZIO Contributors
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

package zio

import zio.clock.Clock
import zio.console.Console
import zio.system.System
import zio.random.Random
import zio.scheduler.Scheduler

private[zio] trait PlatformSpecific {
  type ZEnv = Clock with Console with System with Random

  private[zio] val defaultEnvironment: Managed[Nothing, ZEnv] =
    ((Scheduler.live >>> Clock.live) ++ Console.live ++ System.live ++ Random.live).build

  type Tagged[A] = scala.reflect.ClassTag[A]

  private[zio] def taggedIsSubtype[A, B](left: Tagged[A], right: Tagged[B]): Boolean =
    right.runtimeClass.isAssignableFrom(left.runtimeClass)
}