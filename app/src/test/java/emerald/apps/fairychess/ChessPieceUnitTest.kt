package emerald.apps.fairychess

import emerald.apps.fairychess.model.ChessPiece
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.apache.tools.ant.taskdefs.Move
import org.junit.Test


class ChessPieceUnitTest {

    @Test
    fun testGenerateLeaperMovements() {
        val chessPiece = ChessPiece("knight",3,3,0,"white","~1/2",0)
        var generatedMovements = chessPiece.generateMovements()
        //knight in middle => 8 target squares
        var sourceFile = 3; var sourceRank = 3; var movementNotation = ChessPiece.MovementNotation.parseMovementString("~1/2")[0]
        var expectedMovements = arrayOf(
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,4,5),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,2,5),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,4,1),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,2,1),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,5,4),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,1,4),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,5,2),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,1,2)
        )
        assertEquals(expectedMovements.size,generatedMovements.size)
        for(i in expectedMovements.indices){
            assertEquals(expectedMovements[i].targetFile,generatedMovements[i].targetFile)
            assertEquals(expectedMovements[i].targetRank,generatedMovements[i].targetRank)
        }
        //knight in corner => 2 target squares
        sourceFile = 0; sourceRank = 0
        chessPiece.positionRank = sourceRank; chessPiece.positionFile = sourceFile
        generatedMovements = chessPiece.generateMovements()
        expectedMovements = arrayOf(
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,1,2),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,2,1),
        )
        assertEquals(expectedMovements.size,generatedMovements.size)
        for(i in expectedMovements.indices){
            assertEquals(expectedMovements[i].targetFile,generatedMovements[i].targetFile)
            assertEquals(expectedMovements[i].targetRank,generatedMovements[i].targetRank)
        }
        //knight on edge => 4 target squares
        sourceFile = 0; sourceRank = 3
        chessPiece.positionRank = sourceRank; chessPiece.positionFile = sourceFile
        generatedMovements = chessPiece.generateMovements()
        expectedMovements = arrayOf(
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,1,5),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,1,1),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,2,4),
            ChessPiece.Movement(movementNotation,sourceFile,sourceRank,2,2)
        )
        assertEquals(expectedMovements.size,generatedMovements.size)
        for(i in expectedMovements.indices){
            assertEquals(expectedMovements[i].targetFile,generatedMovements[i].targetFile)
            assertEquals(expectedMovements[i].targetRank,generatedMovements[i].targetRank)
        }
    }

    @Test
    fun testGenerateOthogonalRiderMovements() {
        var chessPiece = ChessPiece("rook", 3, 3, 0, "white", "n+", 0)
        var generatedMovements = chessPiece.generateMovements()
        //rook in middle => 16 target squares
        var sourceFile = 3; var sourceRank = 3; var movementNotation = ChessPiece.MovementNotation.parseMovementString("n+")[0]
        var expectedMovements = mutableListOf<ChessPiece.Movement>()
        for(i in 0..7){
            if(i != sourceRank)expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile,i))
            if(i != sourceFile)expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,i,sourceRank))
        }
        assertEquals(expectedMovements.size,generatedMovements.size)
        for(i in generatedMovements.indices){
            expectedMovements.remove(generatedMovements[i])
        }
        assertEquals(0,expectedMovements.size) //expected movement equaled generatedMovements

        //rook in corner => still 16 target squares
        sourceFile = 0; sourceRank = 0; movementNotation = ChessPiece.MovementNotation.parseMovementString("n+")[0]
        chessPiece = ChessPiece("rook", sourceFile, sourceRank, 0, "white", "n+", 0)
        generatedMovements = chessPiece.generateMovements()
        expectedMovements = mutableListOf<ChessPiece.Movement>()
        for(i in 0..7){
            if(i != sourceRank)expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile,i))
            if(i != sourceFile)expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,i,sourceRank))
        }
        assertEquals(expectedMovements.size,generatedMovements.size)
        for(i in generatedMovements.indices){
            expectedMovements.remove(generatedMovements[i])
        }
        assertEquals(0,expectedMovements.size) //expected movement equaled generatedMovements

        //rook on edge => still 16 target squares
        sourceFile = 0; sourceRank = 3; movementNotation = ChessPiece.MovementNotation.parseMovementString("n+")[0]
        chessPiece = ChessPiece("rook", sourceFile, sourceRank, 0, "white", "n+", 0)
        generatedMovements = chessPiece.generateMovements()
        expectedMovements = mutableListOf<ChessPiece.Movement>()
        for(i in 0..7){
            if(i != sourceRank)expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile,i))
            if(i != sourceFile)expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,i,sourceRank))
        }
        assertEquals(expectedMovements.size,generatedMovements.size)
        for(i in generatedMovements.indices){
            expectedMovements.remove(generatedMovements[i])
        }
        assertEquals(0,expectedMovements.size) //expected movement equaled generatedMovements
    }


    @Test
    fun testGenerateDiagonalRiderMovements() {
        var chessPiece = ChessPiece("bishop", 3, 3, 0, "white", "nX", 0)
        var generatedMovements = chessPiece.generateMovements()
        //bishop in middle => 13 target squares
        var sourceFile = 3; var sourceRank = 3
        var movementNotation = ChessPiece.MovementNotation.parseMovementString("nX")[0]
        var expectedMovements = mutableListOf<ChessPiece.Movement>()
        for (i in 1..7) {
          if(sourceFile + i <= 7 && sourceRank + i <= 7){
              expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile+i,sourceRank+i))
          }
          if(sourceFile + i <= 7 && sourceRank - i >= 0){
              expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile+i,sourceRank-i))
          }
          if(sourceFile - i >= 0 && sourceRank + i <= 7){
              expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile-i,sourceRank+i))
          }
          if(sourceFile - i >= 0 && sourceRank - i >= 0){
              expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile-i,sourceRank-i))
          }
        }
        assertEquals(expectedMovements.size, generatedMovements.size)
        for (i in generatedMovements.indices) {
            expectedMovements.remove(generatedMovements[i])
        }
        assertEquals(0, expectedMovements.size) //expected movement equaled generatedMovements

        //bishop in corner => 7 target squares
        sourceFile = 0; sourceRank = 0
        chessPiece = ChessPiece("bishop", sourceFile, sourceRank, 0, "white", "nX", 0)
        generatedMovements = chessPiece.generateMovements()
        expectedMovements = mutableListOf<ChessPiece.Movement>()
        for (i in 1..7) {
            if(sourceFile + i <= 7 && sourceRank + i <= 7){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile+i,sourceRank+i))
            }
            if(sourceFile + i <= 7 && sourceRank - i >= 0){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile+i,sourceRank-i))
            }
            if(sourceFile - i >= 0 && sourceRank + i <= 7){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile-i,sourceRank+i))
            }
            if(sourceFile - i >= 0 && sourceRank - i >= 0){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile-i,sourceRank-i))
            }
        }
        assertEquals(expectedMovements.size, generatedMovements.size)
        for (i in generatedMovements.indices) {
            expectedMovements.remove(generatedMovements[i])
        }
        assertEquals(0, expectedMovements.size) //expected movement equaled generatedMovements

        //bishop on edge => 8 target squares
        sourceFile = 0; sourceRank = 4
        chessPiece = ChessPiece("bishop", sourceFile, sourceRank, 0, "white", "nX", 0)
        generatedMovements = chessPiece.generateMovements()
        expectedMovements = mutableListOf<ChessPiece.Movement>()
        for (i in 1..7) {
            if(sourceFile + i <= 7 && sourceRank + i <= 7){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile+i,sourceRank+i))
            }
            if(sourceFile + i <= 7 && sourceRank - i >= 0){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile+i,sourceRank-i))
            }
            if(sourceFile - i >= 0 && sourceRank + i <= 7){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile-i,sourceRank+i))
            }
            if(sourceFile - i >= 0 && sourceRank - i >= 0){
                expectedMovements.add(ChessPiece.Movement(movementNotation,sourceFile,sourceRank,sourceFile-i,sourceRank-i))
            }
        }
        assertEquals(expectedMovements.size, generatedMovements.size)
        for (i in generatedMovements.indices) {
            expectedMovements.remove(generatedMovements[i])
        }
        assertEquals(0, expectedMovements.size) //expected movement equaled generatedMovements
    }

    @Test
    fun testStringMethods() {
        val movementList = mutableListOf<ChessPiece.Movement>()
        for(i in 0..5){
            val a = (Math.random()*100).toInt()
            val b = (Math.random()*100).toInt()
            val c = (Math.random()*100).toInt()
            val d = (Math.random()*100).toInt()
            val movement = ChessPiece.Movement(sourceFile = a,sourceRank = b,targetFile = c,targetRank = d)
            assertEquals(
                movement
                ,ChessPiece.Movement.fromStringToMovement(a.toString()+"_"+b+"_"+c+"_"+d)
            )
            movementList.add(movement)
        }

        val movementListConvert = ChessPiece.Movement.fromStringToMovementList(
            ChessPiece.Movement.fromMovementListToString(movementList))

        assertEquals(movementListConvert.size,movementList.size)
        for(i in movementListConvert.indices){
            assertTrue(movementList[i].equals(movementListConvert[i]))

        }

    }
}
