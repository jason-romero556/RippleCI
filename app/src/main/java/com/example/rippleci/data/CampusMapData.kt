package com.example.rippleci.data

import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CampusLocation(
    val id: String,
    val name: String,
    val coordinate: LatLng,
)

private val campusLocations =
    listOf(
        CampusLocation("broome_library", "Broome Library", LatLng(34.162585, -119.041070)),
        CampusLocation("bell_tower", "Bell Tower", LatLng(34.160908, -119.043118)),
        CampusLocation("student_union", "Student Union", LatLng(34.161316, -119.044425)),
        CampusLocation("del_norte", "Del Norte", LatLng(34.163157, -119.044055)),
        CampusLocation("sierra_hall", "Sierra Hall", LatLng(34.162234, -119.044409)),
        CampusLocation("aliso_hall", "Aliso Hall", LatLng(34.161097, -119.045298)),
        CampusLocation("placer_hall", "Placer Hall", LatLng(34.163266, -119.042973)),
        CampusLocation("anacapa_village", "Anacapa Village", LatLng(34.159529, -119.044269)),
        CampusLocation("santa_rosa_village", "Santa Rosa Village", LatLng(34.159014, -119.042951)),
        CampusLocation("islands_cafe", "Islands Cafe", LatLng(34.160639, -119.042190)),
        CampusLocation("topanga_hall", "Topanga Hall", LatLng(34.160195, -119.041997)),
        CampusLocation("santa_cruz_village", "Santa Cruz Village", LatLng(34.159573, -119.044044)),
        CampusLocation("solano_hall", "Solano Hall", LatLng(34.159573, -119.044044)),
        CampusLocation("mvs", "MVS", LatLng(34.162633, -119.045323)),
        CampusLocation("napa_hall", "Napa Hall", LatLng(34.163681, -119.045677)),
        CampusLocation("gateway_hall", "Gateway Hall", LatLng(34.164962, -119.045024)),
        CampusLocation("marin_hall", "Marin Hall", LatLng(34.164456, -119.045432)),
        CampusLocation("shasta_hall", "Shasta Hall", LatLng(34.164633, -119.044531)),
    )

private val graph: Map<Int, List<Int>> =
    buildMap {
        routeEdges.forEach { (from, to) ->
            put(from, getOrDefault(from, emptyList()) + to)
            put(to, getOrDefault(to, emptyList()) + from)
        }
    }

fun campusLocations(): List<CampusLocation> = campusLocations.sortedBy { it.name }

fun walkingRoute(
    start: CampusLocation,
    end: CampusLocation,
): List<LatLng> {
    if (start.id == end.id) {
        return listOf(start.coordinate)
    }

    val startNode = nearestRouteNode(start.coordinate)
    val endNode = nearestRouteNode(end.coordinate)
    if (startNode == null || endNode == null) {
        return listOf(start.coordinate, end.coordinate)
    }

    val pathNodeIds = shortestPath(startNode, endNode)
    return buildList {
        add(start.coordinate)
        pathNodeIds.forEach { nodeId ->
            add(routeNodes[nodeId])
        }
        add(end.coordinate)
    }.dedupeAdjacent()
}

private fun nearestRouteNode(coordinate: LatLng): Int? =
    routeNodes.indices.minByOrNull { index ->
        distanceMeters(coordinate, routeNodes[index])
    }

private fun shortestPath(
    startId: Int,
    endId: Int,
): List<Int> {
    val distances = mutableMapOf(startId to 0.0)
    val previous = mutableMapOf<Int, Int?>()
    val unvisited = graph.keys.toMutableSet()

    previous[startId] = null

    while (unvisited.isNotEmpty()) {
        val current =
            unvisited.minByOrNull { distances[it] ?: Double.POSITIVE_INFINITY }
                ?: break

        if ((distances[current] ?: Double.POSITIVE_INFINITY) == Double.POSITIVE_INFINITY) {
            break
        }

        if (current == endId) {
            break
        }

        unvisited.remove(current)

        graph[current].orEmpty()
            .filter { it in unvisited }
            .forEach { neighbor ->
                val candidate =
                    distances.getValue(current) +
                        distanceMeters(routeNodes[current], routeNodes[neighbor])
                if (candidate < (distances[neighbor] ?: Double.POSITIVE_INFINITY)) {
                    distances[neighbor] = candidate
                    previous[neighbor] = current
                }
            }
    }

    if (startId != endId && endId !in previous) {
        return emptyList()
    }

    val path = mutableListOf<Int>()
    var current: Int? = endId
    while (current != null) {
        path += current
        current = previous[current]
    }

    return path.asReversed()
}

fun routeDistanceMeters(points: List<LatLng>): Int {
    if (points.size < 2) return 0
    return points.zipWithNext { first, second -> distanceMeters(first, second) }
        .sum()
        .toInt()
}

private fun List<LatLng>.dedupeAdjacent(): List<LatLng> =
    fold(mutableListOf()) { acc, point ->
        if (acc.lastOrNull() != point) {
            acc += point
        }
        acc
    }

private fun distanceMeters(
    first: LatLng,
    second: LatLng,
): Double {
    val earthRadius = 6371000.0
    val lat1 = Math.toRadians(first.latitude)
    val lat2 = Math.toRadians(second.latitude)
    val deltaLat = Math.toRadians(second.latitude - first.latitude)
    val deltaLng = Math.toRadians(second.longitude - first.longitude)

    val a =
        sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}
