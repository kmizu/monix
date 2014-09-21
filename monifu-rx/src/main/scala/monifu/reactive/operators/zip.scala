package monifu.reactive.operators

import monifu.concurrent.Scheduler
import monifu.concurrent.locks.SpinLock
import monifu.reactive.{Observable, Observer, Ack}
import monifu.reactive.Ack.{Cancel, Continue}
import monifu.reactive.internals._

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

object zip {
  /**
   * Implements [[Observable.zip]].
   */
  def apply[T, U](other: Observable[U])(implicit s: Scheduler) =
    (source: Observable[T]) => {

      Observable.create[(T, U)] { observerOfPairs =>
        // using mutability, receiving data from 2 producers, so must synchronize
        val lock = SpinLock()
        val queueA = mutable.Queue.empty[(Promise[U], Promise[Ack])]
        val queueB = mutable.Queue.empty[(U, Promise[Ack])]
        var isCompleted = false
        var ack = Continue : Future[Ack]

        def _onError(ex: Throwable) = lock.enter {
          if (!isCompleted) {
            isCompleted = true
            queueA.clear()
            queueB.clear()
            observerOfPairs.onError(ex)
          }
        }

        source.unsafeSubscribe(new Observer[T] {
          def onNext(a: T): Future[Ack] =
            lock.enter {
              if (isCompleted)
                Cancel
              else if (queueB.isEmpty) {
                val resp = Promise[Ack]()
                val promiseForB = Promise[U]()
                queueA.enqueue((promiseForB, resp))

                ack = promiseForB.future.flatMap(b => observerOfPairs.onNext((a, b)))
                resp.completeWith(ack)
                ack
              }
              else {
                val (b, bResponse) = queueB.dequeue()
                val f = observerOfPairs.onNext((a, b))
                bResponse.completeWith(f)
                f
              }
            }

          def onError(ex: Throwable) =
            _onError(ex)

          def onComplete() =
            ack.onContinue {
              lock.enter {
                if (!isCompleted && queueA.isEmpty) {
                  isCompleted = true
                  queueA.clear()
                  queueB.clear()
                  observerOfPairs.onComplete()
                }
              }
            }
        })

        other.unsafeSubscribe(new Observer[U] {
          def onNext(b: U): Future[Ack] =
            lock.enter {
              if (isCompleted)
                Cancel
              else if (queueA.nonEmpty) {
                val (bPromise, response) = queueA.dequeue()
                bPromise.success(b)
                response.future
              }
              else {
                val p = Promise[Ack]()
                queueB.enqueue((b, p))
                p.future
              }
            }

          def onError(ex: Throwable) = _onError(ex)

          def onComplete() =
            ack.onContinue {
              lock.enter {
                if (!isCompleted && queueB.isEmpty) {
                  isCompleted = true
                  queueA.clear()
                  queueB.clear()
                  observerOfPairs.onComplete()
                }
              }
            }
        })
      }
    }

}
