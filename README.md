# Offline Satellite Map Downloader for MapBox

This project demonstrates how to download satellite map tiles using the Mapbox API and the "Maps SDK for Android." It allows you to download satellite map backgrounds from a minimum zoom level of 1 up to a maximum zoom level of 22, and it supports downloading regions defined by polygons.

## Overview
Based on the [LegacyOfflineActivity](https://github.com/mapbox/mapbox-maps-android/blob/main/app/src/main/java/com/mapbox/maps/testapp/examples/LegacyOfflineActivity.kt) project from Mapbox, this application shows how to work with offline regions and manage downloads of satellite map tiles.

The project leverages the Maps SDK for Android. For more details on the SDK, please refer to the [official documentation](https://docs.mapbox.com/android/maps/guides/).

## Features

- Download satellite map tiles offline.
- Define offline regions using polygons.
- Supports zoom levels 1 to 22.
- Easy integration with Mapbox services.

## Prerequisites

- Java Version: 21
- Mapbox Access Token:
```
Create a resource file at app/res/values/mapbox_access_token.xml with the following content:
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <string name="mapbox_access_token" translatable="false" tools:ignore="UnusedResources">MAPBOX TOKEN</string>
</resources>
```

## Installation

1. Clone this repository
2. Open the project in your preferred IDE (Android Studio is recommended).
3. Configure your Mapbox access token by editing app/res/values/mapbox_access_token.xml.
4. Build and run the application on an Android device or emulator.

## Usage

Upon launching the application, the offline download process will begin. Once the download is complete, a button will appear to load the map corresponding to the defined offline region. The region is defined using a polygon, and the offline tile download spans from zoom level 1 to 22.

## Credits

**Author**: Mathias Nocet

**Based on**: [Mapbox Maps Android LegacyOfflineActivity](https://github.com/mapbox/mapbox-maps-android/blob/main/app/src/main/java/com/mapbox/maps/testapp/examples/LegacyOfflineActivity.kt)
