package com.tackmobile.scala.slider

import android.graphics.Bitmap
import android.util.Log

import java.util.Random

import scala.collection.mutable.Set
import scala.collection.mutable.ArrayBuffer

class TileServer( original:Bitmap, rows:Int, columns:Int, tileSize:Int ) {

  val unservedSlices:ArrayBuffer[Bitmap] = new ArrayBuffer[Bitmap]
  val slices:Set[Bitmap] = Set()
  val random:Random = new Random

  sliceOriginal()

  def reset() = slices.foreach( slice => unservedSlices += slice )

  private def sliceOriginal() {
    val fullWidth = tileSize * rows
		val fullHeight = tileSize * columns
		val scaledImage = Bitmap.createScaledBitmap(original, fullWidth, fullHeight, true)
		
    var bitmap:Bitmap = null
		for (rowI <- 0 to 3; colI <- 0 to 3) {
			slices += Bitmap.createBitmap(scaledImage, rowI * tileSize, colI * tileSize, tileSize, tileSize)
		}
    reset()
  }
  
	def serveRandomSlice():Bitmap = {
		if (unservedSlices.size > 0) {
			val randomIndex = random.nextInt(unservedSlices.size)
      val drawable = unservedSlices(randomIndex)
			unservedSlices -= drawable
			return drawable
		}
    null
	}

}
