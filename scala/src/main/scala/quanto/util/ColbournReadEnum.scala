package quanto.util

/**
  * An iso-free graph enumeration procedure, based on the one described in Colbourn and Read (1979)
  *
  * This variation enumerates undirected, typed, bipartite (red/green) graphs with fixed boundary
  */


// store an undirected graph as a symmetric adjacency matrix
case class AdjMat(numRedTypes: Int,
                  numGreenTypes: Int,
                  numBoundaries: Int = 0,
                  red: Vector[Int] = Vector(),
                  green: Vector[Int] = Vector(),
                  mat: Vector[Vector[Boolean]] = Vector())
extends Ordered[AdjMat]
{
  lazy val size: Int = mat.length
  lazy val numRed = red.sum
  lazy val numGreen = green.sum

//  def copy(numRedTypes: Int = numRedTypes,
//           numGreenTypes: Int = numGreenTypes,
//           numBoundaries: Int = numBoundaries,
//           red: Vector[Int] = red,
//           green: Vector[Int] = green,
//           mat: Vector[Vector[Boolean]] = mat): AdjMat =
//    new AdjMat(numRedTypes,numGreenTypes,numBoundaries,red,green,mat)

  // advance to the next type of vertex added by the addVertex method. The order is boundaries,
  // then each red type, then each green type.
  def nextType: Option[AdjMat] = {
    if (red.length < numRedTypes) Some(copy(red = red :+ 0))
    else if (green.length < numGreenTypes) Some(copy(green = green :+ 0))
    else None
  }

  // add the given vector as the bottom row and rightmost column, with false in bottom-right.
  private def growMatrix(vec: Vector[Boolean]) = {
    mat.indices.toVector.map { r => mat(r) :+ vec(r) } :+ (vec :+ false)
  }

  // This method grows the adjacency matrix by adding a new boundary, red node, or green node, with the given
  // vector of edges.
  def addVertex(connection: Vector[Boolean]) = {
    if (red.isEmpty && green.isEmpty) { // new vertex is a boundary
      copy(numBoundaries = numBoundaries + 1, mat = growMatrix(connection))
    } else if (red.nonEmpty && green.isEmpty) { // new vertex is a red node
      copy(red = red.updated(red.length-1, red(red.length-1)+1), mat = growMatrix(connection))
    } else { // new vertex is a green node
      copy(green = green.updated(green.length-1, green(green.length-1)+1), mat = growMatrix(connection))
    }
  }

  // compare the upper triangular part of this matrix, lexicographically
  def compare(that: AdjMat): Int = {
    for (i <- 0 to size)
      for (j <- i to size)
        if (mat(i)(j) < that.mat(i)(j)) return -1
        else if (mat(i)(j) > that.mat(i)(j)) return 1
    0
  }

  // compare this matrix with itself, but with the rows and columns permuted according to "perm"
  def compareWithPerm(perm: Vector[Int]): Int = {
    for (i <- 0 to size)
      for (j <- i to size)
        if (mat(i)(j) < mat(perm(i))(perm(j))) return -1
        else if (mat(i)(j) > mat(perm(i))(perm(j))) return 1
    0
  }

  // return all the vertex-permutations which respect type and keep boundary fixed
  def validPerms: Vector[Vector[Int]] = {
    var idx = numBoundaries
    val bVec = (0 until idx).toVector
    var vecs = Vector[Vector[Int]]()
    for (i <- red) {
      val r = idx until idx + i
      idx += i
      vecs = vecs :+ r.toVector
    }

    AdjMat.productPerms(vecs).map { p => bVec ++ p }
  }

  // return a list of all the valid ways to connect a new node to the graph, which respect the
  // bipartite structure, and maintain boundaries with arity at most 1
  def validConnections: Vector[Vector[Boolean]] = {
    val notRed = red.isEmpty || green.nonEmpty
    val notGreen = green.isEmpty

    def validConnectionsFrom(i: Int): Vector[Vector[Boolean]] =
      if (i >= size) Vector()
      else {
        val rest = validConnectionsFrom(i + 1)
        if (
          (i < numBoundaries && !mat(i).contains(true)) ||
          (i >= numBoundaries && i < numBoundaries + numRed && notRed) ||
          (i >= numBoundaries + numRed && notGreen)
        )
          rest.map(false +: _) ++ rest.map(true +: _)
        else
          rest.map(false +: _)
      }

    validConnectionsFrom(0)
  }


  // a matrix is canonical if it is lexicographically smaller than any vertex permutation
  def isCanonical: Boolean = validPerms.forall { p => compareWithPerm(p) <= 0 }

  // returns true if all boundaries are connected to something
  def isComplete: Boolean = (0 until numBoundaries).forall(i => mat(i).contains(true))

  override def toString: String = {
    var pipes = List[Int](numBoundaries)
    for (r <- red) pipes = (r + pipes.head) :: pipes
    for (g <- green) pipes = (g + pipes.head) :: pipes
    val pipeSet = pipes.toSet

    mat.indices.foldRight("") { (i,str) =>
      mat(i).indices.foldRight("") { (j, rowStr) =>
        (if (pipeSet.contains(i)) " | " else "") + (if (mat(i)(j)) " 1 " else " 0 ")
      } + "\n" + str
    }
  }
}

object AdjMat {
  def perms(vec: Vector[Int]): Vector[Vector[Int]] =
    if (vec.isEmpty) Vector(Vector())
    else vec.indices.toVector.flatMap { i => perms(vec.take(i) ++ vec.drop(i+1)).map { vec(i) +: _ } }

  def productPerms(vecs: Vector[Vector[Int]]): Vector[Vector[Int]] =
    vecs match {
      case vec +: vecs1 =>
        val pVec = perms(vec)
        val pRest = productPerms(vecs1)
        pVec.flatMap { p1 => pRest.map { p2 => p1 ++ p2 } }
      case _ => Vector(Vector())
    }
}

object ColbournReadEnum {
  def enumerate(numRedTypes: Int, numGreenTypes: Int, maxBoundaries: Int, maxVertices: Int): Stream[AdjMat] = {
    def enum1(bnd: Int, verts: Int, amat: AdjMat): Stream[AdjMat] =
      if (amat.isCanonical) {
        (
          // put the current matrix on the stream, if complete
          if (amat.isComplete)
            Stream(amat)
          else Stream()
        ) ++ (
          // add boundaries in all possible ways
          if (bnd > 0) {
            amat.validConnections.foldRight(Stream[AdjMat]()){ (c, rest) =>
              enum1(bnd - 1, verts, amat.addVertex(c)) ++ rest
            }
          } else if (verts > 0) { // add current node type in all possible ways
            amat.validConnections.foldRight(Stream[AdjMat]()){ (c, rest) =>
              enum1(0, verts - 1, amat.addVertex(c)) ++ rest
            }
          } else Stream()
        ) ++ (
          // advancing to the next type of vertex
          amat.nextType match {
            case Some(amat1) => enum1(0, verts, amat1)
            case None => Stream()
          }
        )
      } else Stream()

    enum1(maxBoundaries, maxVertices, AdjMat(numRedTypes, numGreenTypes))
  }
}