import kotlin.math.min
fun levenshtein(strLeft: String, strRight: String): Int
{
    if (strLeft == strRight)
        return 0;
    if (strLeft.isEmpty()) return strRight.length
    if (strRight.isEmpty()) return strLeft.length

    val strLeftLength = strLeft.length + 1
    val strRightLength = strRight.length + 1

    var array = Array(strLeftLength) { IntArray(strRightLength) {0} }
    var cost = 0

    for(i in 1 until strLeftLength)
    {
        for(j in 1 until strRightLength)
        {
            cost = if (strLeft[i - 1] == strRight[j - 1])
                array[i - 1][j - 1]
            else
                array[i - 1][j - 1] + 1
            array[i][j] = min(min(array[i - 1][j] + 1, array[i][j - 1] + 1), cost)
        }
    }

    return array[strLeftLength - 1][strRightLength - 1]
}