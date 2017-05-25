package quanto.cosy

/* Complex numbers for use in Tensor
 *
 */

class Complex(

               val re: Double,
               val im: Double) {
  override def toString: String =
    toStringConcise

  //re + (if (im < 0) "-" + -im else "+" + im) + "i"

  def toStringConcise: String = {
    def simpleNum(x: Double): String = {
      if (x == 0) ""
      else if (x == x.floor) x.toInt.toString
      else x.toString
    }


    val realPart: String = simpleNum(re)


    val imPart: String = {
      simpleNum(im) match {
        case "" => ""
        case "1" => "i"
        case "-1" => "-i"
        case s => s + "i"
      }
    }

    if (realPart != "" && imPart != "") {
      realPart + (if (im >= 0) "+" else "") + imPart
    }
    else {
      if (realPart != "") realPart else if (imPart != "") imPart else "0"
    }
  }


  def this(re: Double) = this(re, 0)

  def /(that: Complex) = this * that.inverse

  def inverse(): Complex = (Complex(re, 0) - Complex(0, im)) * (1 / (re * re + im * im))

  def *(that: Complex) =
    new Complex(re * that.re - im * that.im, re * that.im + im * that.re)

  def -(that: Complex): Complex =
    this + that.negate

  def +(that: Complex): Complex =
    new Complex(re + that.re, im + that.im)

  def negate(): Complex = new Complex(-this.re, -this.im)

  def abs: Double = math.pow(re * re + im * im, 0.5)

  override def equals(other: Any): Boolean =
    other match {
      case that: Complex =>
        (that canEqual this) &&
          this.re == that.re &&
          this.im == that.im
      case that: Double =>
        this.re == that
      case _ => false
    }

  def canEqual(other: Any): Boolean =
    other.isInstanceOf[Complex]

  override def hashCode(): Int = ((41 * this.im) + this.re).toInt
}

object Complex {
  val one = new Complex(1, 0)
  val zero = new Complex(0, 0)
  val i = new Complex(0, 1)

  def apply(r: Double, i: Double) = new Complex(r, i)

  implicit def intToCOmplex(x: Int): Complex = new Complex(x)

  implicit def doubleToComplex(x: Double): Complex = new Complex(x)

  def apply(r: Int, i: Int) = new Complex(r, i)
}
