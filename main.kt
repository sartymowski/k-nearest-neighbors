import java.awt.Point
import java.io.File
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

abstract class Data<Type, RET_VAL>(val value: Type)
{
    abstract fun interval(_value: Data<Type, RET_VAL> ): RET_VAL
}

class PointData(value: Point) : Data<Point, Double>(value)
{
    //override fun interval(_value: PointData) = sqrt((value.getX() - _value.value.getX()) * (value.getX() - _value.value.getX()) + (value.getY() - _value.value.getY()) * (value.getY() - _value.value.getY()))
    override fun interval(_value: Data<Point, Double>) = sqrt((value.getX() - _value.value.getX()) * (value.getX() - _value.value.getX()) + (value.getY() - _value.value.getY()) * (value.getY() - _value.value.getY()))
}

class TextData(value: String): Data<String, Int>(value)
{
    //override fun interval(_value: String) = levenshtein(value, _value)
    //override fun interval(_value: TextData) = (value == _value.value).compareTo(true)
    override fun interval(_value: Data<String, Int>): Int {
        if (value.compareTo(_value.value) == 0)
            return 1
        return 0
    }
}

class IntegerData(value: Int): Data<Int, Int>(value)
{
    //override fun interval(_value: IntegerData) = throw RuntimeException("Not implemented")//euclideanMetric(value, _value).toInt()

    companion object {
        /*fun euclideanMetric(valueLeft: Int, valueRight: Int) = (valueLeft.toDouble() - valueRight.toDouble()).pow(2)
    */}

    override fun interval(_value: Data<Int, Int>) = throw RuntimeException("Not implemented")//euclideanMetric(value, _value).toInt()
}

class RecordData(private val records: MutableList<Data<*, *>>)
{
    fun interval(_records: RecordData): Double
    {
        var sumH = 0
        var amountOfElement = 0
        val collLeft = mutableListOf<Double>()
        val collRight = mutableListOf<Double>()
        for (index in records.indices)
        {
            when (val record = records[index]) {
                is TextData ->
                {
                    sumH += record.interval(_records.records[index] as TextData)
                    amountOfElement++
                }
                is DoubleData ->
                {
                    collLeft.add(record.value)
                    collRight.add((_records.records[index] as DoubleData).value)
                }
            }
        }

        return IntervalTool.sphericalGaussKernel((amountOfElement - sumH), collLeft, collRight)
    }
}

class DoubleData(value: Double): Data<Double, Double>(value)
{
    //override fun interval(_value: DoubleData) = euclideanMetric(value, _value.value)

    companion object {
        fun euclideanMetric(valueLeft: Double, valueRight: Double) = (valueLeft - valueRight).pow(2)
    }

    override fun interval(_value: Data<Double, Double>): Double {
        TODO("Not yet implemented")
    }
}

class FileReader(private val filename: String)
{
    val records: List<String>
    private val recordsNumber: Int

    init {
        records = read()
        recordsNumber = records.size
    }

    private fun read(): List<String>
    {
        val recordsLocal = File(filename).readLines()
        val countCategory = recordsLocal[0].toInt() + 1
        return recordsLocal.drop(countCategory)
    }

    fun getCategory(index: Int): Int
    {
        val record = records[index]
        return record.split(" ").last().toInt()
    }

    fun getRecord(index: Int): RecordData
    {
        val record = records[index]
        val recordsColl = mutableListOf<Data<*, *>>()
        val recordSplitted = record.split(" ").dropLast(1)
        for(rec in recordSplitted)
        {
            if (rec.toIntOrNull() != null)
                recordsColl.add(TextData(rec))
            else if (rec.toDoubleOrNull() != null)
                recordsColl.add(DoubleData(rec.toDouble()))

        }

        return RecordData(recordsColl)
    }
}

abstract class ACollection<T>
{
    abstract fun insert(value: T)
    abstract fun getCurrentValue() : T
    abstract fun size(): Int
    abstract fun moveToTheBeginning()
    abstract fun moveToNextValue()
    abstract fun isEnd(): Boolean
    abstract fun countCategory(compareFun: (T) -> Boolean) : Int
    abstract fun makeEmpty()
}

open class Collection<T>(): ACollection<T>()
{
    private var array = mutableListOf<T>()// MutableList<T>(size) { Unit as T }
    private var it = 0

    constructor(newArray: MutableList<T>): this()
    {
        array = newArray
    }

    override fun insert(value: T) {
        if (!array.contains(value))
            array.add(value)
    }

    override fun getCurrentValue(): T {
        if (size() == it)
            throw RuntimeException("Out of range exception")

        return array[it]
    }

    override fun size(): Int {
        return array.size
    }

    override fun moveToTheBeginning() {
        it = 0
    }

    override fun moveToNextValue() {
        it++
    }

    override fun isEnd(): Boolean
    {
        return it == size()
    }

    override fun countCategory(compareFun: (T) -> Boolean) : Int
    {
        return array.count { compareFun(it) }
    }

    override fun makeEmpty()
    {
        array.clear()
    }
    //private inline fun <reified T>CreateGenericArray() = MutableList<T?>(size) { null }
}

class MyPair<X, C>(private val pair: Pair<X, C>)
{
    val el: X by pair::first
    val cat: C by pair::second
}

class TrainingSet<X, C>: Collection<MyPair<X, C>>()
{
    private fun kNearest(el: X, nearestNeighborsCount: Int): ACollection<MyPair<X, C>>
    {
        val trainingSetWithInterval = mutableListOf<Pair<MyPair<X, C>, Double>>()
        this.moveToTheBeginning()
        while (!this.isEnd())
        {
            val currentValue = this.getCurrentValue()
            val elLoc = currentValue.el
            var intervalCurrVal = 0.0
            when(elLoc){
                is RecordData -> intervalCurrVal = elLoc.interval(el as RecordData)
            }
            trainingSetWithInterval.add(Pair(currentValue, intervalCurrVal))
            this.moveToNextValue()
        }

        val sortedTrainingSetWithInterval = trainingSetWithInterval.sortedWith { a, b -> a.second.compareTo(b.second) }
        return Collection(sortedTrainingSetWithInterval.map { it.first }.toMutableList().subList(0, nearestNeighborsCount))
    }

    private fun decision(kNearestCollection: ACollection<MyPair<X, C>>, cDefaultValueFactory: () -> C): C
    {
        var maxCountCategory = -1
        var decisionVal: C = cDefaultValueFactory()
        while(!kNearestCollection.isEnd())
        {
            val currentCategory = kNearestCollection.getCurrentValue()
            val countCurrentCategory = kNearestCollection.countCategory { i -> i.cat == currentCategory.cat }
            if (countCurrentCategory > maxCountCategory)
            {
                maxCountCategory = countCurrentCategory
                decisionVal = currentCategory.cat
            }

            kNearestCollection.moveToNextValue()
        }

        return decisionVal
    }

    fun classification(el: X, nearestNeighborsCount: Int, cDefaultValueFactory: () -> C): C
    {
        return decision(kNearest(el, nearestNeighborsCount), cDefaultValueFactory)
    }
}

fun main(args: Array<String>)
{
    if (args.isEmpty())
        throw RuntimeException("There is no argument!")
    val fileReader = FileReader(args[0])
    val records = fileReader.records
    val trainingSet = TrainingSet<RecordData, Int>()
    val pairOfRecords = mutableListOf<MyPair<RecordData, Int>>()

    for(index in records.indices)
    {
        pairOfRecords.add(MyPair(Pair(fileReader.getRecord(index), fileReader.getCategory(index))))
    }
    pairOfRecords.shuffle()

    val scanner = Scanner(System.`in`)
    print("Provide amount of nearest neighbors: ")
    val nearestNeighbors = scanner.nextInt()
    print("Provide amount for cross validation: ")
    val crossValidation = scanner.nextInt()

    val crossValidationCollSize = pairOfRecords.size / crossValidation
    val crossValidationBase = mutableListOf<MutableList<MyPair<RecordData, Int>>>()
    val chunked = pairOfRecords.chunked(crossValidationCollSize)
    for(index in chunked.indices)
    {
        crossValidationBase.add(mutableListOf())
        crossValidationBase[index] = chunked[index].toMutableList()
    }

    var generalError = 0.0
    for(row in 0 until crossValidation)
    {
        trainingSet.makeEmpty()
        for (i in 0 until crossValidation)
        {
            if (row != i)
                crossValidationBase[i].forEach { trainingSet.insert(it) }
        }
        var error = 0.0
        for (column in 0 until crossValidationBase[row].size)
        {
            val existedCat = crossValidationBase[row][column].cat
            val newCat = trainingSet.classification(crossValidationBase[row][column].el, nearestNeighbors) { 0 }
            if (existedCat != newCat)
                error++
        }
        error /= crossValidationBase[row].size.toDouble()
        generalError += error
        println("Error for assigning proper category: $error = ${error * 100}%")
    }
    generalError /= crossValidation
    println("General error: $generalError = ${generalError * 100}")
}
