package com.guxins.videocourseplayer.util

/**
 * Compares file names so that numeric runs are ordered by value: "第2课" < "第10课".
 */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val charA = a[i]
            val charB = b[j]
            if (charA.isAsciiDigit() && charB.isAsciiDigit()) {
                val startA = i
                val startB = j
                while (i < a.length && a[i].isAsciiDigit()) i++
                while (j < b.length && b[j].isAsciiDigit()) j++

                val numberAStart = startA + a.substring(startA, i).indexOfFirst { it != '0' }.let {
                    if (it == -1) i - startA else it
                }
                val numberBStart = startB + b.substring(startB, j).indexOfFirst { it != '0' }.let {
                    if (it == -1) j - startB else it
                }
                val lengthA = i - numberAStart
                val lengthB = j - numberBStart
                if (lengthA != lengthB) return lengthA.compareTo(lengthB)
                for (offset in 0 until lengthA) {
                    val diff = a[numberAStart + offset].compareTo(b[numberBStart + offset])
                    if (diff != 0) return diff
                }
            } else {
                val foldedA = charA.lowercaseChar()
                val foldedB = charB.lowercaseChar()
                if (foldedA != foldedB) return foldedA.compareTo(foldedB)
                i++
                j++
            }
        }
        if (i < a.length) return 1
        if (j < b.length) return -1
        val ignoreCase = a.compareTo(b, ignoreCase = true)
        return if (ignoreCase != 0) ignoreCase else a.compareTo(b)
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
