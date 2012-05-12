package com.tackmobile.scala.slider

import java.util.ArrayList
import java.util.HashSet
import java.util.Random

import android.graphics.Bitmap

class TileServer( original:Bitmap, rows:Int, columns:Int, tileSize:Int ) {

  var scaledImage:Bitmap = null
  val width:Int = 0
  val unservedSlices:ArrayList[Bitmap] = new ArrayList[Bitmap]
  val slices:HashSet[Bitmap] = new HashSet[Bitmap]
  val random:Random = new Random

  sliceOriginal()

  def sliceOriginal() {
    val fullWidth = tileSize * rows
		val fullHeight = tileSize * columns
		scaledImage = Bitmap.createScaledBitmap(original, fullWidth, fullHeight, true)
		
    var x:Int = 0
    var y:Int = 0
		var bitmap:Bitmap = null
		for (rowI <- 0 to 3; colI <- 0 to 3) {
			x = rowI * tileSize
			y = colI * tileSize
			bitmap = Bitmap.createBitmap(scaledImage, x, y, tileSize, tileSize)
			slices.add(bitmap)
		}
		unservedSlices.addAll(slices)
  }
  
	def serveRandomSlice():Bitmap = {
		if (unservedSlices.size() > 0) {
			val randomIndex = random.nextInt(unservedSlices.size())
			val drawable = unservedSlices.remove(randomIndex)
			return drawable
		}
    null
	}
}
