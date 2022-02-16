package io.github.edadma.rdb

import io.github.edadma.dal.BasicDAL
import io.github.edadma.dal.BasicDAL.relate

trait AggregateFunction {

  def name: String

  def typ(inputs: List[Type]): Type

  def instance: AggregateFunctionInstance

}

abstract class AbstractAggregateFunction(val name: String) extends AggregateFunction {

  override def toString = s"<aggregate function '$name'>"

}

trait AggregateFunctionInstance {

  def next(args: List[Any]): Unit

  def result: Any

}

abstract class AbstractAggregateFunctionInstance[T] extends AggregateFunctionInstance {

  protected var intermediate: T = _
  protected var count = 0

  def compute(next: Any): T = null.asInstanceOf[T]

  def next(args: List[Any]): Unit = {
    count += 1
    intermediate = compute(args.headOption.orNull)
  }

  def result = intermediate

}

object CountAggregateFunction extends AbstractAggregateFunction("count") {

  def typ(inputs: List[Type]) = IntegerType

  def instance =
    new AbstractAggregateFunctionInstance[Int] {
      override def result = count
    }

}

object SumAggregateFunction extends AbstractAggregateFunction("sum") {

  def typ(inputs: List[Type]) = inputs.head

  def instance: AbstractAggregateFunctionInstance[Number] =
    new AbstractAggregateFunctionInstance[Number] {
      override def compute(next: Any): Number =
        if (intermediate eq null)
          next.asInstanceOf[Number]
        else
          BasicDAL.compute(Symbol("+"), intermediate, next.asInstanceOf[Number])
    }

}

object AvgAggregateFunction extends AbstractAggregateFunction("avg") {

  def typ(inputs: List[Type]) = FloatType //todo: do this properly

  def instance =
    new AbstractAggregateFunctionInstance[Number] {
      override def compute(next: Any) =
        if (intermediate eq null)
          next.asInstanceOf[Number]
        else
          BasicDAL.compute(Symbol("+"), intermediate, next.asInstanceOf[Number])

      override def result = BasicDAL.compute(Symbol("/"), intermediate, count)
    }

}

object MinAggregateFunction extends AbstractAggregateFunction("min") {

  def typ(inputs: List[Type]) = inputs.head

  def instance =
    new AbstractAggregateFunctionInstance[Number] {
      override def compute(next: Any) = {
        if (intermediate eq null)
          next.asInstanceOf[Number]
        else if (relate(Symbol("<"), next.asInstanceOf[Number], intermediate))
          next.asInstanceOf[Number]
        else
          intermediate
      }
    }

}

object MaxAggregateFunction extends AbstractAggregateFunction("max") {

  def typ(inputs: List[Type]) = inputs.head

  def instance =
    new AbstractAggregateFunctionInstance[Number] {
      override def compute(next: Any) = {
        if (intermediate eq null)
          next.asInstanceOf[Number]
        else if (relate(Symbol("<"), intermediate, next.asInstanceOf[Number]))
          next.asInstanceOf[Number]
        else
          intermediate
      }
    }

}

object ListAggregateFunction extends AbstractAggregateFunction("list") {

  def typ(inputs: List[Type]) = TextType

  def instance =
    new AbstractAggregateFunctionInstance[String] {
      override def compute(next: Any) =
        if (intermediate eq null) next.toString else s"$intermediate, $next"
    }

}