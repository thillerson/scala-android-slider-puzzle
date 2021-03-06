package com.tackmobile.scala.slider

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.FloatEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.RelativeLayout

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
  

class GameboardView( context:Context, attrSet:AttributeSet, defStyle:Int )
  extends RelativeLayout( context, attrSet, defStyle )
  with OnTouchListener
  with TypedView {

  import com.tackmobile.scala.slider.GameboardView._

  var tileSize:Size = null
	var gameboardRect:RectF = null
	var emptyTile:GameTile = null
  var movedTile:GameTile = null
	var lastDragPoint:PointF = null
	var tileServer:TileServer = null
	var currentMotionDescriptors:ArrayBuffer[GameTileMotionDescriptor] = null
  var boardCreated:Boolean = false
	var tiles:HashSet[GameTile] = new HashSet[GameTile]

  def this( context:Context, attrSet:AttributeSet ) {
    this( context, attrSet, 0 )
   	val img = getResources().getDrawable(R.drawable.globe);
		val original = img.asInstanceOf[BitmapDrawable].getBitmap();
		tileServer = new TileServer(original, 4, 4, 68);
  }

  override protected def onLayout(changed:Boolean,
                                  left:Int,
                                  top:Int,
                                  right:Int,
                                  bottom:Int) {
    super.onLayout(changed, left, top, right, bottom)

    if (!boardCreated) {
      determineGameboardSizes
      placeTiles
      boardCreated = true
    }
  }

  def placeTiles = tiles foreach placeTile

  def placeTile(tile:GameTile) {
		val tileRect = rectForCoordinate(tile.coordinate)
		val params = new RelativeLayout.LayoutParams(tileSize.width, tileSize.height)
		params.topMargin = tileRect.top
		params.leftMargin = tileRect.left
		addView(tile, params)
		tile.setImageBitmap(tileServer.serveRandomSlice())
  }

  def createTiles() {
		for (rowI <- 0 to 3; colI <- 0 to 3) {
			val tile:GameTile = createTileAtCoordinate( new Coordinate(rowI, colI) )
			if (rowI == 3 && colI == 3) {
				emptyTile = tile
				tile.setEmpty(true)
			}
		}
  }

  def createTileAtCoordinate( coordinate:Coordinate ):GameTile = {
		val tile = new GameTile(getContext(), coordinate)
		tile.setOnTouchListener(this)
		tiles += tile
		tile
  }

	override def onTouch( v:View, event:MotionEvent ):Boolean = {
		try {
			val touchedTile = v.asInstanceOf[GameTile]
			if ( touchedTile.isEmpty || !touchedTile.isInRowOrColumnOf(emptyTile) ) {
				return false
			} else {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					movedTile = touchedTile
					currentMotionDescriptors = getMotionDescriptorsBetweenEmptyTileAndTile(movedTile)
				} else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
					if (lastDragPoint != null) {
						moveDraggedTilesByMotionEventDelta(event)
					}
					lastDragPoint = new PointF(event.getRawX(), event.getRawY())
				} else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
					// reload the motion descriptors in case of position change.
					currentMotionDescriptors = getMotionDescriptorsBetweenEmptyTileAndTile(movedTile)
					// if last move was a dragging move and the move was over half way to the empty tile
					if (lastDragPoint != null && lastDragMovedAtLeastHalfWay()) {
						animateCurrentMovedTilesToEmptySpace()
					  // otherwise, if it wasn't a drag, do the move
					} else if (lastDragPoint == null) {
						animateCurrentMovedTilesToEmptySpace()
					  // Animate tiles back to origin
					} else {
						animateMovedTilesBackToOrigin()
					}
					currentMotionDescriptors = null
					lastDragPoint = null
					movedTile = null
				}
				return true
			}
		} catch {
      case e:ClassCastException => {
        return false
      }
		}
	}

	def lastDragMovedAtLeastHalfWay():Boolean = {
		if (currentMotionDescriptors != null && currentMotionDescriptors.size > 0) {
			var firstMotionDescriptor = currentMotionDescriptors(0);
			if (firstMotionDescriptor.axialDelta > tileSize.width/2) {
				return true;
			}
		}
		return false;
	}

	def moveDraggedTilesByMotionEventDelta( event:MotionEvent ) {
		var dxTile:Float = 0
    var dyTile:Float = 0
		var tile:GameTile = null
	  var tilesToCheck:HashSet[GameTile] = null
		var impossibleMove = true
		var dxEvent = event.getRawX() - lastDragPoint.x
		var dyEvent = event.getRawY() - lastDragPoint.y
		for ( gameTileMotionDescriptor:GameTileMotionDescriptor <- currentMotionDescriptors ) {
			tile = gameTileMotionDescriptor.tile
			dxTile = tile.getX() + dxEvent
			dyTile = tile.getY() + dyEvent
			
			var candidateRect = new RectF(dxTile, dyTile, dxTile + tile.getWidth(), dyTile + tile.getHeight())
			if (tile.coordinate.row == emptyTile.coordinate.row) {
				tilesToCheck = allTilesInRow(tile.coordinate.row)
			} else if (tile.coordinate.column == emptyTile.coordinate.column) {
				tilesToCheck = allTilesInColumn(tile.coordinate.column)
			}
			
			var candidateRectInGameboard = (gameboardRect.contains(candidateRect))
			var collides = candidateRectForTileCollidesWithAnyTileInSet(candidateRect, tile, tilesToCheck)
			
			impossibleMove = impossibleMove && (!candidateRectInGameboard || collides)
		}

		if (!impossibleMove) {
			for ( gameTileMotionDescriptor <- currentMotionDescriptors ) {
				tile = gameTileMotionDescriptor.tile
				dxTile = tile.getX() + dxEvent
				dyTile = tile.getY() + dyEvent
				if (tile.coordinate.row == emptyTile.coordinate.row) {
					tile.setX(dxTile)
				} else if (tile.coordinate.column == emptyTile.coordinate.column) {
					tile.setY(dyTile)
				}
			}
		}
	}

	def candidateRectForTileCollidesWithAnyTileInSet( candidateRect:RectF, tile:GameTile,  set:HashSet[GameTile] ):Boolean = {
		var otherTileRect:RectF = null
		for ( otherTile <- set ) {
			if ( !otherTile.isEmpty && otherTile != tile ) {
				otherTileRect = new RectF(otherTile.getX(), otherTile.getY(), otherTile.getX() + otherTile.getWidth(), otherTile.getY() + otherTile.getHeight())
				if (RectF.intersects(otherTileRect, candidateRect)) {
					return true
				}
			}
		}
		return false
	}
	
	def animateCurrentMovedTilesToEmptySpace() {
		emptyTile.setX(movedTile.getX())
		emptyTile.setY(movedTile.getY())
		emptyTile.coordinate = movedTile.coordinate
		var animator:ObjectAnimator = null
		for (val motionDescriptor <- currentMotionDescriptors) {
			animator = ObjectAnimator.ofObject(
				motionDescriptor.tile,
				motionDescriptor.property,
				new FloatEvaluator(),
				motionDescriptor.from:java.lang.Float,
				motionDescriptor.to:java.lang.Float)
			animator.setDuration(16)
			animator.addListener(new AnimatorListener() {
				
				def onAnimationStart( animation:Animator ) { }
				def onAnimationCancel( animation:Animator ) { }
				def onAnimationRepeat( animation:Animator ) { }
				
				def onAnimationEnd( animation:Animator ) {
					motionDescriptor.tile.coordinate = motionDescriptor.finalCoordinate
					motionDescriptor.tile.setX(motionDescriptor.finalRect.left)
					motionDescriptor.tile.setY(motionDescriptor.finalRect.top)
				}
			})
			animator.start()
		}
	}
	
	def animateMovedTilesBackToOrigin() {
		var animator:ObjectAnimator = null
    var originalRect:Rect = null
		if (currentMotionDescriptors != null) {
			for (motionDescriptor <- currentMotionDescriptors) {
        originalRect = rectForCoordinate(motionDescriptor.tile.coordinate)
				animator = ObjectAnimator.ofObject(
					motionDescriptor.tile,
					motionDescriptor.property,
					new FloatEvaluator(),
					motionDescriptor.currentPosition():java.lang.Double,
					motionDescriptor.originalPosition(originalRect):java.lang.Double);
				animator.setDuration(16);
				animator.start();
			}
		}
	}

	def getMotionDescriptorsBetweenEmptyTileAndTile( tile:GameTile ):ArrayBuffer[GameTileMotionDescriptor] = {
		var descriptors = new ArrayBuffer[GameTileMotionDescriptor]
		var coordinate:Coordinate = null
		var foundTile:GameTile = null
		if (tile.isToRightOf(emptyTile)) {
			for ( i <- tile.coordinate.column until emptyTile.coordinate.column by -1) {
				coordinate = new Coordinate(tile.coordinate.row, i)
				foundTile = if (tile.coordinate.matches(coordinate)) tile else getTileAtCoordinate(coordinate).get
        descriptors += createMotionDescriptorForMovedTile( foundTile, "x", i-1 )
			}
		} else if (tile.isToLeftOf(emptyTile)) {
			for ( i <- tile.coordinate.column until emptyTile.coordinate.column ) {
				coordinate = new Coordinate(tile.coordinate.row, i)
				foundTile = if (tile.coordinate.matches(coordinate)) tile else getTileAtCoordinate(coordinate).get
        descriptors += createMotionDescriptorForMovedTile( foundTile, "x", i+1 )
			}
		} else if (tile.isAbove(emptyTile)) {
			for ( i <- tile.coordinate.row until emptyTile.coordinate.row ) {
				coordinate = new Coordinate(i, tile.coordinate.column)
				foundTile = if (tile.coordinate.matches(coordinate)) tile else getTileAtCoordinate(coordinate).get
        descriptors += createMotionDescriptorForMovedTile( foundTile, "y", i+1 )
			}
		} else if (tile.isBelow(emptyTile)) {
			for ( i <- tile.coordinate.row until emptyTile.coordinate.row by -1 ) {
				coordinate = new Coordinate(i, tile.coordinate.column)
				foundTile = if (tile.coordinate.matches(coordinate)) tile else getTileAtCoordinate(coordinate).get
        descriptors += createMotionDescriptorForMovedTile( foundTile, "y", i-1 )
			}
		}
		descriptors
	}

  def createMotionDescriptorForMovedTile( tile:GameTile, axis:String, coordinateChange:Int ):GameTileMotionDescriptor = {
		val currentRect = rectForCoordinate(tile.coordinate)
    val finalCoordinate = axis match {
      case "x" => new Coordinate( tile.coordinate.row, coordinateChange )
      case "y" => new Coordinate( coordinateChange, tile.coordinate.column )
    }
		val finalRect = rectForCoordinate(finalCoordinate)
    val currentPositionOnAxis = axis match {
      case "x" => tile.getX()
      case "y" => tile.getY()
    }
    val finalPositionOnAxis = axis match {
      case "x" => finalRect.left
      case "y" => finalRect.top
    }
		val axialDelta = Math.abs( axis match {
      case "x" => tile.getX() - currentRect.left
      case "y" => tile.getY() - currentRect.top
    })
		val motionDescriptor = new GameTileMotionDescriptor(
			tile,
      axis,
      currentPositionOnAxis,
      finalPositionOnAxis,
      axialDelta,
      finalRect,
      finalCoordinate
		)
    return motionDescriptor
  }
	
	def getTileAtCoordinate( coordinate:Coordinate ) = tiles.find( tile => tile.coordinate.matches(coordinate) )
	def allTilesInRow( row:Int ) = tiles.filter( tile => tile.coordinate.row == row )
	def allTilesInColumn( column:Int ) = tiles.filter( tile => tile.coordinate.column == column )

	def determineGameboardSizes() {
		var viewWidth = getWidth()
		var viewHeight = getHeight()
    val tileDimen = getResources().getDimension(R.dimen.tile_size)
		tileSize = new Size(tileDimen.toInt, tileDimen.toInt)
		var gameboardWidth = tileSize.width * 4
		var gameboardHeight = tileSize.height * 4
		var gameboardTop = viewHeight/2 - gameboardHeight/2
		var gameboardLeft = viewWidth/2 - gameboardWidth/2
		gameboardRect = new RectF(gameboardLeft, gameboardTop, gameboardLeft + gameboardWidth, gameboardTop + gameboardHeight)
		createTiles()
	}

	def rectForCoordinate( coordinate:Coordinate ):Rect = {
		var gameboardY = Math.floor(gameboardRect.top)
		var gameboardX = Math.floor(gameboardRect.left)
		var top = (coordinate.row * tileSize.height) + gameboardY
		var left = (coordinate.column * tileSize.width) + gameboardX
		return new Rect(left.toInt, top.toInt, left.toInt + tileSize.width, top.toInt + tileSize.height)
	}

}

object GameboardView {

  import android.graphics.Rect

  class Size( var width:Int, var height:Int ) { }

  class Coordinate( var row:Int, var column:Int ) {

		def matches( coordinate:Coordinate ) = coordinate.row == row && coordinate.column == column
    def sharesAxisWith( other:Coordinate ) = other.row == row || other.column == column
    def isToRightOf( other:Coordinate ) = sharesAxisWith(other) && column > other.column
    def isToLeftOf( other:Coordinate ) = sharesAxisWith(other) && column < other.column
    def isAbove( other:Coordinate ) = sharesAxisWith(other) && row < other.row
    def isBelow( other:Coordinate ) = sharesAxisWith(other) && row > other.row

    override def toString = "Coordinate [row=" + row + ", column=" + column + "]"

  }

  class GameTileMotionDescriptor( var tile:GameTile, 
                                 var property:String,
                                 var from:Float,
                                 var to:Float,
                                 var axialDelta:Float,
                                 var finalRect:Rect,
                                 var finalCoordinate:Coordinate
                               ) {
    var coordinate:Coordinate = null

		def currentPosition():Double = {
			if (property.equals("x")) {
				return tile.getX()
			} else if (property.equals("y")) {
				return tile.getY()
			}
			return 0
		}

		def originalPosition( originalRect:Rect ):Double = {
			if (property.equals("x")) {
				return originalRect.left
			} else if (property.equals("y")) {
				return originalRect.top
			}
			return 0
		}
  }

  class GameTile( context:Context, var coordinate:Coordinate ) extends ImageView( context ) {
    private var _empty = false

    def isEmpty = _empty
    def setEmpty( isEmptyTile:Boolean ) {
      _empty = isEmptyTile
      if (isEmptyTile) {
			  setBackgroundDrawable(null);
			  setAlpha(0);
      }
    }

    def isInRowOrColumnOf( otherTile:GameTile ) = coordinate.sharesAxisWith( otherTile.coordinate )
    def isToRightOf( otherTile:GameTile ) = coordinate.isToRightOf( otherTile.coordinate )
    def isToLeftOf( otherTile:GameTile ) = coordinate.isToLeftOf( otherTile.coordinate )
    def isAbove( otherTile:GameTile ) = coordinate.isAbove( otherTile.coordinate )
    def isBelow( otherTile:GameTile ) = coordinate.isBelow( otherTile.coordinate )
    
    override def toString = "<GameTile at row: %d, col: %d, x: %f, y: %f".format(coordinate.row, coordinate.column, getX(), getY())

  }

}
