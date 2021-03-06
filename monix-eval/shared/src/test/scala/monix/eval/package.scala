/*
 * Copyright (c) 2014-2016 by its authors. Some rights reserved.
 * See the project homepage at: https://monix.io
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

package monix

package object eval {
  /** Syntax for equivalence in tests. */
  implicit final class IsEqArrow[A](val lhs: A) extends AnyVal {
    def ===(rhs: A): IsEquiv[A] = IsEquiv(lhs, rhs)
  }

  /** Syntax for negating equivalence in tests. */
  implicit final class IsNotEqArrow[A](val lhs: A) extends AnyVal {
    def !==(rhs: A): IsNotEquiv[A] = IsNotEquiv(lhs, rhs)
  }
}
