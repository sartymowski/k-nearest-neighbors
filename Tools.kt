import kotlin.math.pow
import kotlin.math.sqrt

object IntervalTool {

    private fun multivariateNormalDistribution(collLeft: List<Double>, collRight: List<Double>, det: Double, v: Double, size: Int): Double
    {
        var mnd: Double = 0.0
        collLeft.zip(collRight).forEach { pair -> mnd += Math.E.pow(-0.5 * (1.0 / v) * DoubleData.euclideanMetric(pair.first, pair.second)) / (2.0 * Math.PI).pow(size.toDouble() / 2.0) * det.pow(0.5) }
        return mnd
    }

    fun sphericalGaussKernel(hammingDistance: Int, collLeft: List<Double>, collRight: List<Double>): Double
    {
        var v: Double = 1.0 / (2.0 * Math.PI * hammingDistance.toDouble().pow(2.0 / collLeft.size.toDouble()))
        var sum = 0.0
        var det = v.pow(collLeft.size.toDouble())

        var ff = (hammingDistance - multivariateNormalDistribution(collLeft, collRight, det, v, collLeft.size)).pow(2.0)

        collLeft.zip(collRight).forEach{ pair -> sum += (DoubleData.euclideanMetric(pair.first, pair.second) + ff)}

        return sqrt(sum)
    }
}