package emerald.apps.fairychess.model

fun printBitboard(bitboard: ULong): String {
    return buildString {
        // Chess board is typically displayed with rank 8 at the top
        for (rank in 7 downTo 0) {
            // Add rank number
            append("${rank + 1} ")

            for (file in 0..7) {
                // Calculate bit position (0-63)
                val position = rank * 8 + file
                // Check if bit is set at this position
                val bit = (bitboard shr position) and 1uL

                append(if (bit == 1uL) "X " else "0 ")
            }
            appendLine()
        }
        // Add file letters at the bottom
        appendLine("  a b c d e f g h")
    }
}