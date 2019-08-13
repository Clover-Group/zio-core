package ru.itclover.tsp.core.optimizations
import ru.itclover.tsp.core.Pattern.IdxExtractor
import ru.itclover.tsp.core.Pat
import ru.itclover.tsp.core._
import scala.language.existentials

import scala.language.reflectiveCalls
import ru.itclover.tsp.core.aggregators.GroupPattern
import ru.itclover.tsp.core.aggregators.PreviousValue
import ru.itclover.tsp.core.aggregators.TimerPattern
import ru.itclover.tsp.core.aggregators.WindowStatistic
import ru.itclover.tsp.core.io.TimeExtractor
import cats.kernel.Group
import scala.language.higherKinds
import Optimizer.S

class Optimizer[E: IdxExtractor: TimeExtractor]() extends Serializable {

  def optimizations[T] =
    Seq(optimizeInners[T], coupleOfTwoSimple[T], coupleOfTwoConst[T], mapOfConst[T], mapOfSimple[T])

  def optimizable[T](pat: Pat[E, T]): Boolean =
    optimizations[T].exists(_.isDefinedAt(pat))

  def applyOptimizations[T](pat: Pat[E, T]): (Pat[E, T], Boolean) =
    optimizations[T].foldLeft(pat -> false) {
      case ((p, _), rule) if rule.isDefinedAt(p) => rule.apply(p).asInstanceOf[Pat[E, T]] -> true
      case (x, _)                                => x
    }

  def optimize[T](pattern: Pat[E, T]): Pattern[E, S[T], T] = forceState(optimizePat(pattern))

  private def optimizePat[T](pattern: Pat[E, T]): Pat[E, T] = {

    val optimizedPatternIterator = Iterator.iterate(pattern -> true) { case (p, _) => applyOptimizations(p) }

    // try no more than 10 cycles of optimizations to avoid infinite recursive loops in case of wrong rule.
    optimizedPatternIterator.takeWhile(_._2).take(10).map(_._1).toSeq.last
  }

  type OptimizeRule[T] = PartialFunction[Pat[E, T], Pat[E, T]]

  private def coupleOfTwoConst[T]: OptimizeRule[T] = {
    case Pat(x @ CouplePattern(Pat(ConstPattern(a)), Pat(ConstPattern(b)))) =>
      ConstPattern[E, T](x.func.apply(a, b))
  }

  private def coupleOfTwoSimple[T]: OptimizeRule[T] = {
    // couple(simple, simple) => simple
    case Pat(
        x @ CouplePattern(Pat(SimplePattern(fleft: (E => Result[T]))), Pat(SimplePattern(fright: (E => Result[T]))))
        ) =>
      SimplePattern[E, T](event => x.func.apply(fleft.apply(event), fright.apply(event)))
    // couple(simple, const) => simple
    case Pat(x @ CouplePattern(Pat(SimplePattern(fleft: (E => Result[T]))), Pat(ConstPattern(r)))) =>
      SimplePattern[E, T](event => x.func.apply(fleft.apply(event), r))
    // couple(const, simple) => simple
    case Pat(x @ CouplePattern(Pat(ConstPattern(l)), Pat(SimplePattern(fright: (E => Result[T]))))) =>
      SimplePattern[E, T](event => x.func.apply(l, fright.apply(event)))
    // couple(some, const) => map(some)
    case Pat(x @ CouplePattern(left, Pat(ConstPattern(r)))) =>
      MapPattern(forceState(left))(t => x.func.apply(Result.succ(t), r))
    // couple(const, some) => map(some)
    case Pat(x @ CouplePattern(Pat(ConstPattern(l)), right)) =>
      MapPattern(forceState(right))(t => x.func.apply(l, Result.succ(t)))
  }

  private def mapOfConst[T]: OptimizeRule[T] = {
    case Pat(map @ MapPattern(Pat(ConstPattern(x)))) => ConstPattern[E, T](x.flatMap(map.func))
  }

  private def mapOfSimple[T]: OptimizeRule[T] = {
    case Pat(map @ MapPattern(Pat(simple: SimplePattern[E, _]))) =>
      SimplePattern[E, T](e => simple.f(e).flatMap(map.func))
  }

  //todo mapOfMap

  private def optimizeInners[T]: OptimizeRule[T] = {
    case AndThenPattern(first, second) if optimizable(first) || optimizable(second) =>
      AndThenPattern(
        forceState(optimizePat(first)),
        forceState(optimizePat(second))
      )
    case x @ MapPattern(inner) if optimizable(inner) =>
      MapPattern(forceState(optimizePat(inner)))(x.func)
    case x @ CouplePattern(left, right) if optimizable(left) || optimizable(right) =>
      CouplePattern(
        forceState(optimizePat(left)),
        forceState(optimizePat(right))
      )(x.func)
    case x: IdxMapPattern[E, _, _, _] if optimizable(x.inner) => new IdxMapPattern(forceState(x.inner))(x.func)
    case x: ReducePattern[E, _, _, _] if x.patterns.exists(optimizable) => {
      def cast[S <: PState[T, S], T](pats: Seq[Pat[E, T]]): Seq[Pattern[E, S, T]] forSome { type S <: PState[T, S] } =
        pats.asInstanceOf[Seq[Pattern[E, S, T]]]
      new ReducePattern(cast(x.patterns.map(t => optimizePat(t))))(x.func, x.transform, x.filterCond, x.initial)
    }
    case x @ GroupPattern(inner, window) if optimizable(inner) => {
      implicit val group: Group[T]      = x.group.asInstanceOf[Group[T]]
      val newInner: Pattern[E, S[T], T] = forceState(optimizePat(inner.asInstanceOf[Pat[E, T]]))
      GroupPattern[E, S[T], T](newInner, window).asInstanceOf[Pat[E, T]]
    }
    case PreviousValue(inner, window) if optimizable(inner)   => PreviousValue(forceState(optimizePat(inner)), window)
    case TimerPattern(inner, window) if optimizable(inner)    => TimerPattern(forceState(optimizePat(inner)), window)
    case WindowStatistic(inner, window) if optimizable(inner) => WindowStatistic(forceState(optimizePat(inner)), window)
  }

  // Need to cast Pat[E,T] to some Pattern type. Pattern has restriction on State
  // type parameters which is constant, so simple asInstanceOf complains on
  // unmet restrictions.
  private def forceState[E, T](pat: Pat[E, T]): Pattern[E, S[T], T] =
    pat.asInstanceOf[Pattern[E, S[T], T]]
}

object Optimizer {
  // Fake type to return from Optimizer. Needs to meet the
  // constraints of Pattern type parameters.
  type S[T] <: PState[T, S[T]]
}
