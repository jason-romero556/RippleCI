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

private data class PathNode(
    val id: String,
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

private val pathNodes =
    listOf(
        PathNode("ventura_south", LatLng(34.160980, -119.044680)),
        PathNode("la_ventura", LatLng(34.161720, -119.044520)),
        PathNode("la_central", LatLng(34.161870, -119.043720)),
        PathNode("la_camarillo", LatLng(34.161980, -119.042630)),
        PathNode("south_quad_west", LatLng(34.161140, -119.043980)),
        PathNode("south_quad_east", LatLng(34.161220, -119.042820)),
        PathNode("bell_tower_loop", LatLng(34.160930, -119.043150)),
        PathNode("islands_walk", LatLng(34.160730, -119.042320)),
        PathNode("topanga_walk", LatLng(34.160290, -119.042050)),
        PathNode("residence_path", LatLng(34.159760, -119.043960)),
        PathNode("quad_path_south", LatLng(34.162640, -119.043840)),
        PathNode("quad_path_mid", LatLng(34.163210, -119.043780)),
        PathNode("quad_path_north", LatLng(34.163700, -119.043700)),
        PathNode("central_northwest", LatLng(34.162180, -119.044470)),
        PathNode("central_northeast", LatLng(34.162270, -119.043050)),
        PathNode("sierra_walk", LatLng(34.162120, -119.044250)),
        PathNode("student_union_walk", LatLng(34.161520, -119.044100)),
        PathNode("placer_walk", LatLng(34.163060, -119.042940)),
        PathNode("broome_walk", LatLng(34.162520, -119.041500)),
        PathNode("santa_barbara_west", LatLng(34.164050, -119.044120)),
        PathNode("santa_barbara_east", LatLng(34.164040, -119.042760)),
        PathNode("north_camarillo", LatLng(34.163230, -119.042650)),
        PathNode("lindero_mid", LatLng(34.162650, -119.041780)),
        PathNode("chapel_drive", LatLng(34.162280, -119.041020)),
        PathNode("san_luis_east", LatLng(34.163520, -119.041720)),
        PathNode("north_east_walk", LatLng(34.164150, -119.044940)),
        PathNode("gateway_walk", LatLng(34.164760, -119.045020)),
        PathNode("marin_walk", LatLng(34.164360, -119.045250)),
        PathNode("shasta_walk", LatLng(34.164520, -119.044560)),
        PathNode("mvs_walk", LatLng(34.162740, -119.045020)),
        PathNode("aliso_walk", LatLng(34.161120, -119.044960)),
    )

private val nodeLookup = pathNodes.associateBy { it.id }

private val corridorEdges =
    listOf(
        "ventura_south" to "la_ventura",
        "la_ventura" to "la_central",
        "la_central" to "la_camarillo",
        "south_quad_west" to "south_quad_east",
        "south_quad_west" to "student_union_walk",
        "south_quad_east" to "la_camarillo",
        "south_quad_east" to "bell_tower_loop",
        "bell_tower_loop" to "islands_walk",
        "bell_tower_loop" to "south_quad_west",
        "islands_walk" to "topanga_walk",
        "topanga_walk" to "residence_path",
        "student_union_walk" to "la_ventura",
        "student_union_walk" to "la_central",
        "student_union_walk" to "sierra_walk",
        "la_central" to "central_northwest",
        "la_central" to "central_northeast",
        "central_northwest" to "sierra_walk",
        "central_northwest" to "quad_path_south",
        "central_northeast" to "placer_walk",
        "central_northeast" to "north_camarillo",
        "quad_path_south" to "quad_path_mid",
        "quad_path_mid" to "quad_path_north",
        "quad_path_mid" to "north_camarillo",
        "quad_path_north" to "santa_barbara_west",
        "santa_barbara_west" to "santa_barbara_east",
        "santa_barbara_east" to "north_camarillo",
        "santa_barbara_east" to "san_luis_east",
        "san_luis_east" to "lindero_mid",
        "lindero_mid" to "chapel_drive",
        "chapel_drive" to "broome_walk",
        "chapel_drive" to "la_camarillo",
        "north_camarillo" to "lindero_mid",
        "north_camarillo" to "placer_walk",
        "north_camarillo" to "broome_walk",
        "santa_barbara_east" to "shasta_walk",
        "shasta_walk" to "north_east_walk",
        "north_east_walk" to "gateway_walk",
        "north_east_walk" to "marin_walk",
        "north_east_walk" to "mvs_walk",
        "mvs_walk" to "central_northeast",
        "student_union_walk" to "aliso_walk",
    )

private val buildingAccessNodes =
    mapOf(
        "broome_library" to "broome_walk",
        "bell_tower" to "bell_tower_loop",
        "student_union" to "student_union_walk",
        "del_norte" to "quad_path_mid",
        "sierra_hall" to "sierra_walk",
        "aliso_hall" to "aliso_walk",
        "placer_hall" to "placer_walk",
        "anacapa_village" to "residence_path",
        "santa_rosa_village" to "residence_path",
        "islands_cafe" to "islands_walk",
        "topanga_hall" to "topanga_walk",
        "santa_cruz_village" to "residence_path",
        "solano_hall" to "quad_path_south",
        "mvs" to "mvs_walk",
        "napa_hall" to "santa_barbara_west",
        "gateway_hall" to "gateway_walk",
        "marin_hall" to "marin_walk",
        "shasta_hall" to "shasta_walk",
    )

private val graph: Map<String, List<String>> =
    buildMap {
        corridorEdges.forEach { (from, to) ->
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

    val startNode = buildingAccessNodes[start.id]?.let(nodeLookup::get)
    val endNode = buildingAccessNodes[end.id]?.let(nodeLookup::get)

    if (startNode == null || endNode == null) {
        return listOf(start.coordinate, end.coordinate)
    }

    val pathNodeIds = shortestPath(startNode.id, endNode.id)
    if (pathNodeIds.isEmpty()) {
        return listOf(start.coordinate, end.coordinate)
    }

    return buildList {
        add(start.coordinate)
        pathNodeIds.forEach { nodeId ->
            add(nodeLookup.getValue(nodeId).coordinate)
        }
        add(end.coordinate)
    }.dedupeAdjacent()
}

private fun shortestPath(
    startId: String,
    endId: String,
): List<String> {
    val distances = mutableMapOf(startId to 0.0)
    val previous = mutableMapOf<String, String?>()
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
                        distanceMeters(nodeLookup.getValue(current).coordinate, nodeLookup.getValue(neighbor).coordinate)
                if (candidate < (distances[neighbor] ?: Double.POSITIVE_INFINITY)) {
                    distances[neighbor] = candidate
                    previous[neighbor] = current
                }
            }
    }

    if (startId != endId && endId !in previous) {
        return emptyList()
    }

    val path = mutableListOf<String>()
    var current: String? = endId
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
