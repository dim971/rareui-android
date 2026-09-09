package io.github.dim971.rareui.showcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The showcase opens straight to one component when told which, which is what
        // makes verifying nineteen of them and the screenshots in docs/ reproducible:
        //
        //   adb shell am start -n io.github.dim971.rareui.showcase/.MainActivity \
        //       -e component "Gravity Letters"
        val opening = intent?.getStringExtra("component")

        setContent {
            ShowcaseApp(opening = opening)
        }
    }
}
