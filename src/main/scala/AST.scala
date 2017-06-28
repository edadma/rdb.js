package xyz.hyperreal.rdb

import scala.util.parsing.input.{Positional, Position}


trait ValueExpression extends Positional
case class NumberLit( n: String ) extends ValueExpression
case class IntegerLit( n: String ) extends ValueExpression
case class StringLit( s: String ) extends ValueExpression
case class MarkLit( m: Mark ) extends ValueExpression

trait RelationExpression
case class VariableRelationExpression( name: Ident ) extends RelationExpression
case class LiteralRelationExpression( columns: List[ColumnSpec], data: List[List[ValueExpression]] ) extends RelationExpression
case class ProjectionRelationExpression( relation: RelationExpression, columns: List[Ident] ) extends RelationExpression

case class Ident( pos: Position, name: String )

case class ColumnSpec( name: Ident, typepos: Position, typ: Option[String] )