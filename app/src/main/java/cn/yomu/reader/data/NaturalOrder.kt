package cn.yomu.reader.data

/** Sorts file names the way readers expect: 2.jpg comes before 10.jpg. */
object NaturalOrder : Comparator<String> {
    override fun compare(left: String, right: String): Int {
        var li = 0
        var ri = 0

        while (li < left.length && ri < right.length) {
            val lc = left[li]
            val rc = right[ri]
            if (lc.isDigit() && rc.isDigit()) {
                val leftStart = li
                val rightStart = ri
                while (li < left.length && left[li].isDigit()) li++
                while (ri < right.length && right[ri].isDigit()) ri++

                val leftNumber = left.substring(leftStart, li).trimStart('0').ifEmpty { "0" }
                val rightNumber = right.substring(rightStart, ri).trimStart('0').ifEmpty { "0" }
                val lengthResult = leftNumber.length.compareTo(rightNumber.length)
                if (lengthResult != 0) return lengthResult
                val numberResult = leftNumber.compareTo(rightNumber)
                if (numberResult != 0) return numberResult
            } else {
                val charResult = lc.lowercaseChar().compareTo(rc.lowercaseChar())
                if (charResult != 0) return charResult
                li++
                ri++
            }
        }
        return left.length.compareTo(right.length)
    }
}
