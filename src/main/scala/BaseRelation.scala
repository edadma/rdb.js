package xyz.hyperreal.rdb

import collection.mutable.{HashMap, ArrayBuffer, ListBuffer, TreeMap}


class BaseRelation( name: String, definition: Seq[BaseRelationColumn] ) extends AbstractRelation {

	private val rows = new ArrayBuffer[Array[AnyRef]]

	val metadata = new Metadata( definition toIndexedSeq )

	private val indexes = new ArrayBuffer[TreeMap[AnyRef, Int]]
//	private val pkindex =
//		metadata primaryKey match {
//			case None => sys.error( s"attempt to create base relation '$name' with no primary key" )
//			case Some( Column( _, col, typ, _ ) ) =>
//				val index = new TreeMap[AnyRef, Int]()( typ )
//
//				indexes(col) = index
//				index
//		}

	def iterator( context: List[Tuple] ) = rows.iterator map (_ toVector)

	override def size = rows.length

	def delete( conn: Connection, cond: LogicalResult ) = {
		var count = 0

		for (i <- rows.length - 1 to 0 by -1)
			if (conn.evalCondition( List(rows(i)), cond ) == TRUE) {
				rows.remove( i )
				count += 1
			}

		count
	}

	def update( conn: Connection, cond: LogicalResult, updates: List[(Int, ValueResult)] ) = {
		var count = 0

		for (i <- rows.length - 1 to 0 by -1)
			if (conn.evalCondition( List(rows(i)), cond ) == TRUE) {
				for ((f, v) <- updates)
					rows(i)(f) = conn.evalValue( List(rows(i)), v )
				count += 1
			}

		count
	}

	def insertRow( row: Tuple ): Option[Map[String, AnyRef]] =
		row zip definition find {case (v, d) => v.isInstanceOf[Mark] && (d.unmarkable || d.constraint.contains( PrimaryKey ))} match {
			case None =>
				rows += row.toArray
				Some( Map.empty )
			case Some( (_, BaseRelationColumn(table, column, _, _, _, _)) ) => sys.error( s"column '$column' of table '$table' is unmarkable" )
		}

	def insertRelation( rel: Relation ) = {
		val mapping = new ArrayBuffer[Option[Int]]

		for (c <- metadata.header)
			mapping += rel.metadata.columnMap get c.column

		val res = new ListBuffer[Map[String, AnyRef]]
		var count = 0

		for (row <- rel) {
			val r =
				(for ((m, idx) <- mapping zipWithIndex)
					yield {
						m match {
							case Some( n ) => row( n )
							case None =>
								if (metadata.baseRelationHeader(idx).auto)
									metadata.baseRelationHeader(idx).typ.asInstanceOf[Auto].next()
								I
						}
					}) toVector

			insertRow( r ) match {
				case None =>
				case Some( m ) =>
					res += m
					count += 1
			}
		}

		(res toList, count)
	}

	def insertTupleseq( data: Tupleseq ) = {
		val res = new ListBuffer[Map[String, AnyRef]]
		var count = 0

		for (r <- data) {
			insertRow( r ) match {
				case None =>
				case Some( m ) =>
					res += m
					count += 1
			}
		}

		(res toList, count)
	}

	override def toString = s"baseRelation( $name, $definition )"
}
