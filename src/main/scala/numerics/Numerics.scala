
package numerics

object Numerics {
  
import math._

  
  // * The standard digamma function.
   def digamma(xx: Double) = {
    var x = xx;
    var r = 0.0;

    while (x<=5) {
      r -= 1/x;
      x += 1;
    }

    val f = 1./(x * x);
    val t = f*(-1/12.0 +  f*(1/120.0 +  f*(-1/252.0 +  f*(1/240.0 +  f*(-1/132.0 +
            f*(691/32760.0 +  f*(-1/12.0 +  f*3617.0/8160.0)))))));
    r + log(x) - 0.5/x + t;
  }

  private val cof =  Array(76.18009172947146, -86.50532032941677,
    24.01409824083091,-1.231739572450155,
    0.1208650973866179e-2,-0.5395239384953e-5
  );


  // Computes the log of the gamma function.
  // return an approximation of the log of the Gamma function * of x.  Laczos Approximation
  
  def lgamma(x : Double) = {
    var y = x;
    var tmp = x + 5.5;
    tmp -= ((x + 0.5) * log(tmp));
    var ser = 1.000000000190015;
    var j = 0;
    while (j < 6) {
      y += 1;
      ser += (cof(j)/y);
      j +=1;
    }
    (-tmp + log(2.5066282746310005*ser / x));
  }

  private val SQRT_PI = sqrt(Pi);

  private val ERF_A = 8.0 / (3.0 * Pi) * (Pi - 3) / (4 - Pi);
  private val ERF_B = 4.0/ Pi;

  // Approximation to the inverse ERF. Based on
  def erfi(x:Double) = {
    val x2 = x*x;
    val lg1mx2 = log(1- x2);
    val c = 2 / Pi / ERF_A + lg1mx2/2;
    val result =  sqrt(-c + sqrt( c*c - 1/ ERF_A * lg1mx2))
    if (x < 0) -1 * result
    else result
  }

  // 1- erf(x)
  def erfc(x: Double) =  1 - erf(x);

 // approximation to the erf function, for gaussian integrals.
  def erf(x: Double) = {
    val mag = 1 - gamma(.5,x*x)/sqrt(Pi);
    if (x < 0) -1.0 * mag // ERF is odd.
    else mag
  }

  //  Incomplete lgamma function.
  def lgamma(a: Double, z:Double) = {
     var res = 0.0;
     var m = 21
     while( m > 1) {
        res = ((1.0-m)*(m-1.0-a)) / (2*m -1+z -a + res);
        m -= 1
    }

    a * log(z) - z - log(1+z-a+res);
  }

  
  //  Incomplete gamma function, the exp of lgamma(a,z)
    def gamma(a: Double, z:Double) = exp(lgamma(a,z));

  // Sums together things in log space.
  //  return log(exp(a) + exp(b))
   def logSum(a: Double, b: Double) = {
      if (a.isNegInfinity) b
      else if (b.isNegInfinity) a
      else if (a < b) b + log1p(exp(a - b))
      else a + log1p(exp(b - a))
  }

  /**
  * Sums together things in log space.
  * @return log(\sum exp(a_i))
  */
  def logSum(a: Double, b: Double, c: Double*): Double = {
    if (c.length == 0)
      logSum(a, b)
    else
      logSum(logSum(a, b) +: c)
  }

  /**
  * Sums together things in log space.
  * @return log(\sum exp(a_i))
  */
  def logSum(iter: Iterator[Double], max: Double): Double = {
    require(iter.hasNext)
    if (max.isInfinite) {
      max
    } else {
      val aux = (0.0 /: iter) {
        (acc, x) => if (x.isNegInfinity) acc else acc + math.exp(x-max)
      }
      if (aux != 0)
        max + log(aux)
      else
        max
    }
  }

  /**
  * Sums together things in log space.
  * @return log(\sum exp(a_i))
  */
  def logSum(a: Seq[Double]): Double = {
    a.length match {
      case 0 => Double.NegativeInfinity
      case 1 => a(0)
      case 2 => logSum(a(0), a(1))
      case _ => logSum(a.iterator, a reduceLeft (_ max _))
    }
  }

  /**
   * Sums together the first length elements in log space.
   * The length parameter is used.
   *
   * This method needs to be fast. Don't scala-ify it.
   * @return log(\sum^length exp(a_i))
   */
  def logSum(a: Array[Double], length: Int):Double = {
    length match {
      case 0 => Double.NegativeInfinity;
      case 1 => a(0)
      case 2 => logSum(a(0),a(1));
      case _ =>
        val m = max(a, length);
        if(m.isInfinite) m
        else {
          var i = 0;
          var accum = 0.0;
          while(i < length) {
            accum += exp(a(i) - m);
            i += 1;
          }
          m + log(accum);
        }
    }
  }

  // fast versions of max. Useful for the fast logsum.
  private def max(a: Array[Double], length: Int) = {
    var i = 1;
    var max =  a(0);
    while(i < length) {
      if(a(i) > max) max = a(i);
      i += 1;
    }
    max;

  }

  /**
   * The sigmoid function: 1/(1 + exp(-x));
   */
  def sigmoid(x: Double) = 1/(1+exp(-x));


  /**
   * Takes the difference of two doubles in log space. Requires a &gt b.
   * Note that this only works if a and b are close in value. For a &gt;&gt; b,
   * this will almost certainly do nothing. (exp(30) - exp(1) \approx exp(30))
   *
   * @return log(exp(a) - exp(b))
   */
  def logDiff(a: Double, b: Double): Double = {
    require(a >= b)
    if (a > b) a + math.log(1.0 - math.exp(b-a))
    else Double.NegativeInfinity
  }

  /**
   * Computes the polynomial P(x) with coefficients given in the passed in array.
   * coefs(i) is the coef for the x^i term.
   */
  def poly(coefs: Array[Double], x: Double) = {
    var i = coefs.length-1;
    var p = coefs(i);
    while (i>0) {
      i -= 1;
      p = p*x + coefs(i);
    }
    p
  }

}


