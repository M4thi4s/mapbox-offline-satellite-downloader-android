package com.mathoucorp.offlineandroidnative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.mapbox.bindgen.Value
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapDebugOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style

class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private var point = Point.fromLngLat(-1.519202, 47.283447)
    private var margin = 0.005
    // Create a polygon zone around the point (a rectangle in this case)
    private var polygon = Polygon.fromLngLats(
        listOf(
            listOf(
                Point.fromLngLat(-1.519202 + margin, 47.283447 + margin),
                Point.fromLngLat(-1.519202 - margin, 47.283447 + margin),
                Point.fromLngLat(-1.519202 - margin, 47.283447 - margin),
                Point.fromLngLat(-1.519202 + margin, 47.283447 - margin),
                Point.fromLngLat(-1.519202 + margin, 47.283447 + margin) // Closing the polygon
            )
        )
    )
    private var style = "mapbox://styles/mapbox/standard-satellite"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val debugOptions: MutableList<MapDebugOptions> = mutableListOf(
            MapDebugOptions.TILE_BORDERS,
            MapDebugOptions.PARSE_STATUS,
        )

        val frameLayout = FrameLayout(this)

        mapView = MapView(
            this,
            MapInitOptions(
                context = this,
                styleUri = style
            )
        )

        // Set the camera position to the given point
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(point)
                .pitch(0.0)
                .zoom(13.0)
                .bearing(0.0)
                .build()
        )

        mapView.mapboxMap.setDebug(debugOptions, true)

        // Add the map view to the layout
        frameLayout.addView(mapView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // When the style is loaded, add a polygon fill to display the zone.
        mapView.mapboxMap.loadStyle(
            style(style = style) {
                +geoJsonSource("GEO-SOURCE") {
                    data(polygon.toJson())
                }
                +layerAtPosition(
                    fillLayer("LAYER-ID", "GEO-SOURCE") {
                        fillColor(Color.parseColor("#0080ff")).fillOpacity(0.2)
                    }
                )
                +lineLayer(
                    "line-layer", "GEO-SOURCE"
                ) {
                    lineColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                    lineWidth(3.0)
                }
            }
        )

        // Create a container for bottom buttons
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val containerLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        buttonContainer.layoutParams = containerLayoutParams

        // "Download Map" button
        val downloadButton = Button(this).apply {
            text = "Download Map"
            setOnClickListener { downloadMap() }
        }
        buttonContainer.addView(downloadButton)

        // "Reset Cache and Tiles" button
        val resetButton = Button(this).apply {
            text = "Reset Cache and Tiles"
            setOnClickListener { resetCacheAndTiles() }
        }
        buttonContainer.addView(resetButton)

        // Add the button container to the layout
        frameLayout.addView(buttonContainer)

        Log.i("DISPLAY METRICS", this.resources.displayMetrics.toString())

        setContentView(frameLayout)
    }

    fun downloadMap() {

        val stylePackLoadOptions = StylePackLoadOptions.Builder()
            .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
            .metadata(Value("TEST-METADATA"))
            .build()

        val offlineManager: OfflineManager = OfflineManager()

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(style)
                .minZoom(18)
                .maxZoom(Double.POSITIVE_INFINITY.toInt().toByte())
                .pixelRatio(2f)
                .extraOptions(Value.fromJson("""{keep-legacy-style-pack": true}""")?.value)
                .build()
        )
        val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
            .geometry(polygon)
            .descriptors(listOf(tilesetDescriptor))
            .metadata(Value("TEST-METADATA"))
            //.acceptExpired(false)
            .networkRestriction(NetworkRestriction.NONE)
            .build()

        offlineManager.loadStylePack(
            style,
            stylePackLoadOptions,
            { progress ->
                Log.i("StylePackLoadProgress", "Progress: $progress")
            },
            { expected ->
                if (expected.isValue) {
                    expected.value?.let {
                        Log.i("StylePackLoadSuccess", "Style pack loaded successfully")
                        Log.i("STARTING DOWNLOAD TILES", "Downloading tiles...")
                        val tileStore = TileStore.create()
                        tileStore.loadTileRegion(
                            "test",
                            tileRegionLoadOptions,
                            { progress ->
                                Log.i("TileRegionLoadProgress", "Progress: $progress")
                            }
                        ) { expected ->
                            if (expected.isValue) {
                                expected.value?.let {
                                    Log.i("TileRegionLoadSuccess", "Tile region loaded successfully")
                                }
                            }
                            expected.error?.let {
                                Log.e("TileRegionLoadError", "Error: ${it.message}")
                            }
                        }

                    }
                }
                expected.error?.let {
                    Log.e("StylePackLoadError", "Error: ${it.message}")
                }
            }
        )

    }

    fun resetCacheAndTiles() {
        MapboxMap.Companion.clearData(
            { Log.i("ClearDataSuccess", "Data cleared successfully") }
        )
        val offlineManager: OfflineManager = OfflineManager()
        val tileStore = TileStore.create()
        tileStore.removeTileRegion("test")
    }

    /**
     * (Optional) Creates a custom marker bitmap.
     * This helper method is still available if you need to create point markers.
     */
    private fun createMarkerBitmap(size: Int, color: Int, isCircle: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        if (isCircle) {
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        } else {
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        }
        return bitmap
    }
}
