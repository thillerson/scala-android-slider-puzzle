package com.tackmobile.scala.slider

import android.app.Activity
import android.os.Bundle

class SliderPuzzleActivity extends Activity with TypedActivity {

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)
  }

}
