package com.example.rippleci.data

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class OpenStreetMapTileProvider(
    cacheDirectory: File,
) : TileProvider {
    private val client =
        OkHttpClient.Builder()
            .cache(Cache(File(cacheDirectory, "osm_tiles"), 20L * 1024L * 1024L))
            .build()

    override fun getTile(
        x: Int,
        y: Int,
        zoom: Int,
    ): Tile {
        val request =
            Request.Builder()
                .url("https://tile.openstreetmap.org/$zoom/$x/$y.png")
                .header("User-Agent", "RippleCI/1.0 (Campus map)")
                .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return TileProvider.NO_TILE
                val body = response.body?.bytes() ?: return TileProvider.NO_TILE
                Tile(TILE_SIZE, TILE_SIZE, body)
            }
        }.getOrDefault(TileProvider.NO_TILE)
    }

    private companion object {
        const val TILE_SIZE = 256
    }
}
