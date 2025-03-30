package com.mathoucorp.offlineandroidnative

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mathoucorp.offlineandroidnative.databinding.ActivityLegacyOfflineBinding

/**
 * Sample application that downloads an offline region and,
 * once completed, displays a button to load the map corresponding to the defined region.
 */
class MainActivity : ComponentActivity() {

    private lateinit var offlineManager: OfflineRegionManager
    private lateinit var offlineRegion: OfflineRegion
    private var mapView: MapView? = null
    private lateinit var binding: ActivityLegacyOfflineBinding

    private val offlineObserver = object : OfflineRegionObserver {
        override fun errorOccurred(error: OfflineRegionError) {
            if (error.isFatal) {
                Log.e(TAG, "Fatal error: ${error.type}, ${error.message}")
            } else {
                Log.w(TAG, "Error during download: ${error.type}, ${error.message}")
            }
            offlineRegion.setOfflineRegionDownloadState(OfflineRegionDownloadState.INACTIVE)
        }

        override fun statusChanged(status: OfflineRegionStatus) {
            Log.d(TAG, "${status.completedResourceCount}/${status.requiredResourceCount} resources; ${status.completedResourceSize} bytes downloaded.")
            if (status.downloadState == OfflineRegionDownloadState.INACTIVE) {
                onDownloadComplete()
            }
        }
    }

    private val offlineRegionCallback = OfflineRegionCreateCallback { expected ->
        expected.value?.let { region ->
            Log.i(TAG, "Offline region created: $region")
            offlineRegion = region
            region.setOfflineRegionObserver(offlineObserver)
            region.setOfflineRegionDownloadState(OfflineRegionDownloadState.ACTIVE)
        } ?: run {
            Log.e(TAG, "Error creating offline region: ${expected.error}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "onCreate")
        binding = ActivityLegacyOfflineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        offlineManager = OfflineRegionManager()

        // Create the offline region definition with simplified parameters
        val offlineDefinition = OfflineRegionGeometryDefinition.Builder()
            .geometry(polygonGeometry)
            .pixelRatio(2f)
            .minZoom(1.0)
            .maxZoom(DEFAULT_ZOOM + 20)
            .styleURL(MAP_STYLE)
            .glyphsRasterizationMode(GlyphsRasterizationMode.NO_GLYPHS_RASTERIZED_LOCALLY)
            .build()

        Log.i(TAG, "Offline definition: $offlineDefinition")
        offlineManager.createOfflineRegion(offlineDefinition, offlineRegionCallback)
    }

    private fun onDownloadComplete() {
        Log.i(TAG, "Download complete")
        binding.downloadProgress.visibility = View.GONE
        binding.showMapButton.visibility = View.VISIBLE

        binding.showMapButton.setOnClickListener { button ->
            button.visibility = View.GONE
            initializeMapView()
        }
    }

    private fun initializeMapView() {
        mapView = MapView(
            this,
            MapInitOptions(context = this, styleUri = MAP_STYLE)
        ).also { mv ->
            mv.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .zoom(DEFAULT_ZOOM)
                    .center(MAP_CENTER)
                    .build()
            )
            mv.mapboxMap.setDebug(
                listOf(MapDebugOptions.TILE_BORDERS, MapDebugOptions.PARSE_STATUS), true
            )
            mv.onStart()

            mv.mapboxMap.loadStyle(
                style(MAP_STYLE) {
                    +geoJsonSource("geojson-source") {
                        data(polygonGeometry.toJson())
                    }
                    +layerAtPosition(
                        fillLayer("fill-layer", "geojson-source") {
                            fillColor(Color.parseColor("#0080ff"))
                            fillOpacity(0.2)
                        }
                    )
                    +lineLayer("line-layer", "geojson-source") {
                        lineColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                        lineWidth(3.0)
                    }
                }
            )
            setContentView(mv)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        offlineRegion.invalidate { }
        mapView?.onDestroy()
    }

    companion object {
        private const val TAG = "Offline"
        private const val DEFAULT_ZOOM = 16.0
        private val MAP_CENTER = Point.fromLngLat(-1.519202, 47.283447)
        private const val COORDINATE_MARGIN = 0.0005

        // Create a polygon that follows the counterclockwise (right-hand rule) order for the outer ring
        private val polygonGeometry: Polygon = Polygon.fromOuterInner(
            LineString.fromLngLats(
                listOf(
                    Point.fromLngLat(-1.519202 + COORDINATE_MARGIN, 47.283447 + COORDINATE_MARGIN),
                    Point.fromLngLat(-1.519202 - COORDINATE_MARGIN, 47.283447 + COORDINATE_MARGIN),
                    Point.fromLngLat(-1.519202 - COORDINATE_MARGIN, 47.283447 - COORDINATE_MARGIN),
                    Point.fromLngLat(-1.519202 + COORDINATE_MARGIN, 47.283447 - COORDINATE_MARGIN),
                    // Close the ring by repeating the first point
                    Point.fromLngLat(-1.519202 + COORDINATE_MARGIN, 47.283447 + COORDINATE_MARGIN)
                )
            ),
            BoundingBox.fromLngLats( // WARNING: BoundingBox is required for offline download !!!
                -1.519202 + COORDINATE_MARGIN, 47.283447 + COORDINATE_MARGIN,
                -1.519202 - COORDINATE_MARGIN, 47.283447 - COORDINATE_MARGIN
            )
        )

        private const val MAP_STYLE = Style.SATELLITE
    }
}
