package ru.itclover.tsp.core

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{ Foldable, Functor, Monad, Order }
import ru.itclover.tsp.core.PQueue._
import ru.itclover.tsp.core.Pattern.{ Idx, IdxExtractor, QI }

import scala.annotation.tailrec
import scala.collection.{ mutable => m }
import scala.language.higherKinds

/** Couple Pattern */
case class CouplePattern[Event: IdxExtractor, State1 <: PState[T1, State1], State2 <: PState[T2, State2], T1, T2, T3](
  left: Pattern[Event, State1, T1],
  right: Pattern[Event, State2, T2]
)(
  val func: (Result[T1], Result[T2]) => Result[T3]
)(
  implicit idxOrd: Order[Idx]
) extends Pattern[Event, CouplePState[State1, State2, T1, T2, T3], T3] {
  override def apply[F[_]: Monad, Cont[_]: Foldable: Functor](
    oldState: CouplePState[State1, State2, T1, T2, T3],
    events: Cont[Event]
  ): F[CouplePState[State1, State2, T1, T2, T3]] = {
    val leftF  = left.apply(oldState.left, events)
    val rightF = right.apply(oldState.right, events)
    for (newLeftState  <- leftF;
         newRightState <- rightF) yield {
      // Build a new queue from the left and right ones
      val (updatedLeftQueue, updatedRightQueue, newFinalQueue) =
        processQueues(newLeftState.queue, newRightState.queue, oldState.queue)

      CouplePState(
        newLeftState.copyWith(updatedLeftQueue),
        newRightState.copyWith(updatedRightQueue),
        newFinalQueue
      )
    }
  }

  private def processQueues(firstQ: QI[T1], secondQ: QI[T2], totalQ: QI[T3]): (QI[T1], QI[T2], QI[T3]) = {

    @tailrec
    def inner(first: QI[T1], second: QI[T2], total: QI[T3]): (QI[T1], QI[T2], QI[T3]) = {

      def default: (QI[T1], QI[T2], QI[T3]) = (first, second, total)

      (first.headOption, second.headOption) match {
        // if any of parts is empty -> do nothing
        case (_, None)                                                            => default
        case (None, _)                                                            => default
        case (Some(iv1 @ IdxValue(idx1, val1)), Some(iv2 @ IdxValue(idx2, val2))) =>
          // we emit result only if results on left and right sides come at the same time
          if (idxOrd.eqv(idx1, idx2)) {
            val result: Result[T3] = func(val1, val2)
            inner(first.behead(), second.behead(), total.enqueue(IdxValue.union(iv1, iv2)((_, _) => result)))
            // otherwise skip results from one of sides
          } else if (idxOrd.lt(idx1, idx2)) {
            inner(first.behead(), second, total)
          } else {
            inner(first, second.behead(), total)
          }
      }
    }

    inner(firstQ, secondQ, totalQ)
  }

  override def initialState(): CouplePState[State1, State2, T1, T2, T3] =
    CouplePState(left.initialState(), right.initialState(), MutablePQueue(m.Queue.empty))
}

case class CouplePState[State1 <: PState[T1, State1], State2 <: PState[T2, State2], T1, T2, T3](
  left: State1,
  right: State2,
  override val queue: QI[T3]
) extends PState[T3, CouplePState[State1, State2, T1, T2, T3]] {
  override def copyWith(queue: QI[T3]): CouplePState[State1, State2, T1, T2, T3] = this.copy(queue = queue)
}

case object CouplePState {}
