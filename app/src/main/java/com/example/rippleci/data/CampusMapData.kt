package com.example.rippleci.data

import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
)

data class CampusLocation(
    val id: String,
    val name: String,
    val coordinate: GeoCoordinate,
)

private data class GraphEdge(
    val destination: GeoCoordinate,
    val distanceMeters: Double,
)

private data class QueueEntry(
    val node: GeoCoordinate,
    val distanceMeters: Double,
)

private data class RouteCandidate(
    val points: List<GeoCoordinate>,
    val totalDistanceMeters: Double,
)

private val campusLocations =
    listOf(
        CampusLocation("broome_library", "Broome Library", GeoCoordinate(34.162592, -119.041285)),
        CampusLocation("bell_tower", "Bell Tower", GeoCoordinate(34.160856, -119.043071)),
        CampusLocation("student_union", "Student Union", GeoCoordinate(34.161316, -119.044425)),
        CampusLocation("del_norte", "Del Norte", GeoCoordinate(34.163257, -119.044194)),
        CampusLocation("sierra_hall", "Sierra Hall", GeoCoordinate(34.162014, -119.044434)),
        CampusLocation("aliso_hall", "Aliso Hall", GeoCoordinate(34.161740, -119.045438)),
        CampusLocation("placer_hall", "Placer Hall", GeoCoordinate(34.163273, -119.042941)),
        CampusLocation("anacapa_village", "Anacapa Village", GeoCoordinate(34.159529, -119.044269)),
        CampusLocation("santa_rosa_village", "Santa Rosa Village", GeoCoordinate(34.159014, -119.042951)),
        CampusLocation("islands_cafe", "Islands Cafe", GeoCoordinate(34.160622, -119.042374)),
        CampusLocation("topanga_hall", "Topanga Hall", GeoCoordinate(34.160195, -119.041997)),
        CampusLocation("santa_cruz_village", "Santa Cruz Village", GeoCoordinate(34.159573, -119.044044)),
        CampusLocation("solano_hall", "Solano Hall", GeoCoordinate(34.163286, -119.045328)),
        CampusLocation("mvs", "MVS", GeoCoordinate(34.162633, -119.045323)),
        CampusLocation("napa_hall", "Napa Hall", GeoCoordinate(34.163681, -119.045677)),
        CampusLocation("gateway_hall", "Gateway Hall", GeoCoordinate(34.164962, -119.045024)),
        CampusLocation("marin_hall", "Marin Hall", GeoCoordinate(34.164456, -119.045432)),
        CampusLocation("shasta_hall", "Shasta Hall", GeoCoordinate(34.164633, -119.044531)),
    )

// OSM-derived walkable campus geometry: footways, paths, steps, corridors, and campus service paths.
private val osmWalkingWays =
    listOf(
        // OSM way 10726436 (service)
        listOf(
            GeoCoordinate(34.1623915, -119.0461876),
            GeoCoordinate(34.1618736, -119.0463820),
            GeoCoordinate(34.1618110, -119.0464963),
            GeoCoordinate(34.1617865, -119.0465808),
            GeoCoordinate(34.1618496, -119.0469894),
            GeoCoordinate(34.1618447, -119.0470484),
            GeoCoordinate(34.1618000, -119.0470900),
            GeoCoordinate(34.1617380, -119.0471280),
            GeoCoordinate(34.1617010, -119.0472070),
            GeoCoordinate(34.1616744, -119.0472838),
            GeoCoordinate(34.1616345, -119.0474651),
            GeoCoordinate(34.1616007, -119.0476853),
            GeoCoordinate(34.1615590, -119.0480510),
            GeoCoordinate(34.1615490, -119.0483406),
            GeoCoordinate(34.1615280, -119.0491754),
            GeoCoordinate(34.1615250, -119.0492228),
            GeoCoordinate(34.1615143, -119.0492517),
            GeoCoordinate(34.1614815, -119.0492849),
            GeoCoordinate(34.1614379, -119.0493077),
            GeoCoordinate(34.1611629, -119.0493584),
            GeoCoordinate(34.1605664, -119.0493315),
            GeoCoordinate(34.1598920, -119.0495707),
            GeoCoordinate(34.1598367, -119.0495916),
        ),
        // OSM way 10728185 (service)
        listOf(
            GeoCoordinate(34.1639664, -119.0405212),
            GeoCoordinate(34.1639338, -119.0405071),
            GeoCoordinate(34.1633620, -119.0402871),
            GeoCoordinate(34.1629910, -119.0401454),
            GeoCoordinate(34.1619448, -119.0397581),
            GeoCoordinate(34.1619049, -119.0397433),
            GeoCoordinate(34.1618484, -119.0397211),
        ),
        // OSM way 10734018 (footway)
        listOf(
            GeoCoordinate(34.1625278, -119.0421282),
            GeoCoordinate(34.1625056, -119.0422048),
            GeoCoordinate(34.1624979, -119.0422356),
            GeoCoordinate(34.1624622, -119.0423553),
            GeoCoordinate(34.1624696, -119.0424235),
            GeoCoordinate(34.1623082, -119.0430534),
            GeoCoordinate(34.1622262, -119.0433509),
            GeoCoordinate(34.1621918, -119.0434755),
            GeoCoordinate(34.1621652, -119.0435826),
            GeoCoordinate(34.1621337, -119.0436933),
            GeoCoordinate(34.1621051, -119.0438055),
            GeoCoordinate(34.1619460, -119.0444294),
            GeoCoordinate(34.1618311, -119.0448909),
            GeoCoordinate(34.1618202, -119.0449453),
            GeoCoordinate(34.1618007, -119.0450265),
        ),
        // OSM way 10734019 (footway)
        listOf(
            GeoCoordinate(34.1615104, -119.0449181),
            GeoCoordinate(34.1615299, -119.0448449),
            GeoCoordinate(34.1615434, -119.0447923),
            GeoCoordinate(34.1616637, -119.0443233),
            GeoCoordinate(34.1618562, -119.0435827),
            GeoCoordinate(34.1619108, -119.0433616),
            GeoCoordinate(34.1620486, -119.0428464),
            GeoCoordinate(34.1622062, -119.0422564),
            GeoCoordinate(34.1622468, -119.0421059),
            GeoCoordinate(34.1622657, -119.0420307),
        ),
        // OSM way 200130230 (service)
        listOf(
            GeoCoordinate(34.1605664, -119.0493315),
            GeoCoordinate(34.1604990, -119.0492680),
            GeoCoordinate(34.1604637, -119.0492180),
            GeoCoordinate(34.1604282, -119.0491543),
            GeoCoordinate(34.1604016, -119.0490678),
            GeoCoordinate(34.1603903, -119.0489509),
            GeoCoordinate(34.1603869, -119.0488081),
            GeoCoordinate(34.1603975, -119.0485815),
            GeoCoordinate(34.1604218, -119.0483707),
            GeoCoordinate(34.1604764, -119.0481096),
            GeoCoordinate(34.1605805, -119.0476282),
            GeoCoordinate(34.1606005, -119.0474919),
            GeoCoordinate(34.1606026, -119.0473809),
            GeoCoordinate(34.1605961, -119.0472715),
            GeoCoordinate(34.1605742, -119.0471694),
            GeoCoordinate(34.1605327, -119.0470364),
            GeoCoordinate(34.1604925, -119.0469375),
            GeoCoordinate(34.1604388, -119.0468359),
            GeoCoordinate(34.1603972, -119.0467597),
            GeoCoordinate(34.1603427, -119.0466785),
            GeoCoordinate(34.1602220, -119.0465550),
            GeoCoordinate(34.1601702, -119.0465111),
            GeoCoordinate(34.1600380, -119.0463990),
            GeoCoordinate(34.1597103, -119.0461565),
            GeoCoordinate(34.1596606, -119.0461198),
            GeoCoordinate(34.1595754, -119.0460788),
        ),
        // OSM way 461618016 (footway)
        listOf(
            GeoCoordinate(34.1641175, -119.0448728),
            GeoCoordinate(34.1641420, -119.0447472),
            GeoCoordinate(34.1642747, -119.0442392),
            GeoCoordinate(34.1642700, -119.0441762),
            GeoCoordinate(34.1642429, -119.0441376),
            GeoCoordinate(34.1640919, -119.0440763),
            GeoCoordinate(34.1634429, -119.0438369),
            GeoCoordinate(34.1633952, -119.0438380),
            GeoCoordinate(34.1633600, -119.0438778),
            GeoCoordinate(34.1632711, -119.0442002),
            GeoCoordinate(34.1632675, -119.0442696),
            GeoCoordinate(34.1631983, -119.0445266),
            GeoCoordinate(34.1634940, -119.0446387),
            GeoCoordinate(34.1637762, -119.0447457),
            GeoCoordinate(34.1641175, -119.0448728),
            GeoCoordinate(34.1640639, -119.0450905),
        ),
        // OSM way 461618679 (footway)
        listOf(
            GeoCoordinate(34.1609088, -119.0422842),
            GeoCoordinate(34.1609078, -119.0422882),
            GeoCoordinate(34.1608686, -119.0424439),
            GeoCoordinate(34.1608510, -119.0425084),
            GeoCoordinate(34.1607717, -119.0428002),
            GeoCoordinate(34.1607285, -119.0429592),
            GeoCoordinate(34.1607106, -119.0430252),
            GeoCoordinate(34.1605629, -119.0436056),
            GeoCoordinate(34.1605119, -119.0438006),
            GeoCoordinate(34.1605097, -119.0438088),
            GeoCoordinate(34.1605023, -119.0438374),
            GeoCoordinate(34.1603496, -119.0444288),
        ),
        // OSM way 544485497 (footway)
        listOf(
            GeoCoordinate(34.1605629, -119.0436056),
            GeoCoordinate(34.1593881, -119.0431537),
            GeoCoordinate(34.1593970, -119.0430887),
            GeoCoordinate(34.1595312, -119.0425837),
            GeoCoordinate(34.1596795, -119.0420156),
            GeoCoordinate(34.1608686, -119.0424439),
        ),
        // OSM way 614570505 (footway)
        listOf(
            GeoCoordinate(34.1593970, -119.0430887),
            GeoCoordinate(34.1592267, -119.0430250),
        ),
        // OSM way 614570506 (footway)
        listOf(
            GeoCoordinate(34.1595312, -119.0425837),
            GeoCoordinate(34.1593204, -119.0425155),
        ),
        // OSM way 634478774 (service)
        listOf(
            GeoCoordinate(34.1627157, -119.0421978),
            GeoCoordinate(34.1627324, -119.0421380),
            GeoCoordinate(34.1627493, -119.0420903),
            GeoCoordinate(34.1628263, -119.0419642),
            GeoCoordinate(34.1628610, -119.0419489),
            GeoCoordinate(34.1629435, -119.0419737),
            GeoCoordinate(34.1633893, -119.0421322),
            GeoCoordinate(34.1634837, -119.0421678),
            GeoCoordinate(34.1635029, -119.0421750),
            GeoCoordinate(34.1635367, -119.0421890),
        ),
        // OSM way 634482428 (service)
        listOf(
            GeoCoordinate(34.1628788, -119.0430141),
            GeoCoordinate(34.1630128, -119.0430708),
            GeoCoordinate(34.1630196, -119.0430855),
            GeoCoordinate(34.1629301, -119.0434246),
            GeoCoordinate(34.1629098, -119.0434346),
            GeoCoordinate(34.1627790, -119.0433851),
            GeoCoordinate(34.1628788, -119.0430141),
        ),
        // OSM way 634482429 (service)
        listOf(
            GeoCoordinate(34.1644671, -119.0428496),
            GeoCoordinate(34.1644363, -119.0429250),
            GeoCoordinate(34.1644185, -119.0429542),
            GeoCoordinate(34.1643652, -119.0430209),
            GeoCoordinate(34.1643388, -119.0430376),
            GeoCoordinate(34.1643004, -119.0430349),
            GeoCoordinate(34.1638270, -119.0428531),
            GeoCoordinate(34.1630020, -119.0425411),
            GeoCoordinate(34.1628453, -119.0424789),
            GeoCoordinate(34.1628201, -119.0424520),
            GeoCoordinate(34.1628088, -119.0424047),
            GeoCoordinate(34.1628345, -119.0423023),
            GeoCoordinate(34.1628497, -119.0422456),
        ),
        // OSM way 641097814 (footway)
        listOf(
            GeoCoordinate(34.1642129, -119.0392703),
            GeoCoordinate(34.1642229, -119.0393593),
            GeoCoordinate(34.1642315, -119.0394610),
            GeoCoordinate(34.1642278, -119.0396193),
            GeoCoordinate(34.1642105, -119.0397633),
            GeoCoordinate(34.1642092, -119.0399051),
            GeoCoordinate(34.1642168, -119.0400203),
            GeoCoordinate(34.1642434, -119.0400533),
            GeoCoordinate(34.1642817, -119.0402394),
            GeoCoordinate(34.1642679, -119.0402926),
            GeoCoordinate(34.1646503, -119.0421250),
            GeoCoordinate(34.1646571, -119.0422040),
            GeoCoordinate(34.1646566, -119.0422872),
            GeoCoordinate(34.1646522, -119.0423534),
            GeoCoordinate(34.1646400, -119.0424206),
            GeoCoordinate(34.1645664, -119.0427164),
            GeoCoordinate(34.1645675, -119.0428030),
        ),
        // OSM way 641097816 (footway)
        listOf(
            GeoCoordinate(34.1611028, -119.0447108),
            GeoCoordinate(34.1611545, -119.0445105),
        ),
        // OSM way 641233462 (footway)
        listOf(
            GeoCoordinate(34.1639582, -119.0404302),
            GeoCoordinate(34.1640342, -119.0401583),
            GeoCoordinate(34.1640371, -119.0401374),
            GeoCoordinate(34.1640345, -119.0401151),
            GeoCoordinate(34.1640197, -119.0400987),
            GeoCoordinate(34.1639977, -119.0400912),
            GeoCoordinate(34.1639413, -119.0401198),
            GeoCoordinate(34.1638521, -119.0401472),
            GeoCoordinate(34.1637974, -119.0401555),
            GeoCoordinate(34.1637496, -119.0401495),
            GeoCoordinate(34.1636933, -119.0401231),
            GeoCoordinate(34.1635121, -119.0399786),
            GeoCoordinate(34.1633885, -119.0399320),
            GeoCoordinate(34.1633465, -119.0399469),
            GeoCoordinate(34.1632965, -119.0399544),
            GeoCoordinate(34.1632832, -119.0399533),
            GeoCoordinate(34.1632699, -119.0399455),
            GeoCoordinate(34.1632566, -119.0399358),
            GeoCoordinate(34.1632477, -119.0399206),
            GeoCoordinate(34.1632400, -119.0398987),
            GeoCoordinate(34.1632289, -119.0398768),
            GeoCoordinate(34.1631549, -119.0398025),
            GeoCoordinate(34.1630804, -119.0397690),
            GeoCoordinate(34.1630091, -119.0397466),
            GeoCoordinate(34.1629600, -119.0397446),
            GeoCoordinate(34.1629279, -119.0397500),
            GeoCoordinate(34.1629046, -119.0397540),
            GeoCoordinate(34.1628746, -119.0397526),
            GeoCoordinate(34.1628441, -119.0397459),
            GeoCoordinate(34.1628141, -119.0397292),
            GeoCoordinate(34.1627481, -119.0396936),
            GeoCoordinate(34.1627176, -119.0396722),
            GeoCoordinate(34.1626571, -119.0396393),
            GeoCoordinate(34.1626189, -119.0396172),
            GeoCoordinate(34.1625034, -119.0396038),
            GeoCoordinate(34.1624718, -119.0395897),
            GeoCoordinate(34.1624413, -119.0395729),
            GeoCoordinate(34.1624296, -119.0395461),
            GeoCoordinate(34.1624235, -119.0395220),
            GeoCoordinate(34.1623896, -119.0395093),
            GeoCoordinate(34.1623624, -119.0394992),
            GeoCoordinate(34.1623087, -119.0394690),
            GeoCoordinate(34.1622709, -119.0394394),
            GeoCoordinate(34.1622448, -119.0394321),
            GeoCoordinate(34.1622110, -119.0394321),
            GeoCoordinate(34.1621488, -119.0394375),
            GeoCoordinate(34.1620750, -119.0394650),
            GeoCoordinate(34.1619952, -119.0395186),
            GeoCoordinate(34.1619707, -119.0395321),
            GeoCoordinate(34.1619281, -119.0395408),
        ),
        // OSM way 641233465 (footway)
        listOf(
            GeoCoordinate(34.1640410, -119.0411375),
            GeoCoordinate(34.1640324, -119.0411557),
            GeoCoordinate(34.1639327, -119.0411213),
            GeoCoordinate(34.1639172, -119.0411506),
            GeoCoordinate(34.1638554, -119.0411307),
            GeoCoordinate(34.1638280, -119.0412336),
            GeoCoordinate(34.1637278, -119.0416130),
            GeoCoordinate(34.1636530, -119.0418984),
            GeoCoordinate(34.1635319, -119.0423632),
            GeoCoordinate(34.1635270, -119.0424217),
            GeoCoordinate(34.1635665, -119.0424517),
            GeoCoordinate(34.1639754, -119.0426067),
            GeoCoordinate(34.1641916, -119.0426872),
            GeoCoordinate(34.1643637, -119.0427513),
            GeoCoordinate(34.1644181, -119.0427465),
            GeoCoordinate(34.1644390, -119.0426714),
            GeoCoordinate(34.1645109, -119.0423984),
            GeoCoordinate(34.1645218, -119.0423261),
            GeoCoordinate(34.1645258, -119.0422603),
            GeoCoordinate(34.1645218, -119.0421908),
            GeoCoordinate(34.1645159, -119.0421409),
            GeoCoordinate(34.1644265, -119.0417031),
            GeoCoordinate(34.1641031, -119.0401778),
            GeoCoordinate(34.1640900, -119.0400994),
            GeoCoordinate(34.1640799, -119.0400306),
            GeoCoordinate(34.1640718, -119.0399360),
            GeoCoordinate(34.1640715, -119.0398235),
            GeoCoordinate(34.1640945, -119.0395598),
            GeoCoordinate(34.1640945, -119.0394757),
            GeoCoordinate(34.1640863, -119.0393960),
            GeoCoordinate(34.1640675, -119.0393116),
            GeoCoordinate(34.1640417, -119.0392395),
            GeoCoordinate(34.1639985, -119.0391550),
            GeoCoordinate(34.1639390, -119.0390845),
            GeoCoordinate(34.1638821, -119.0390302),
            GeoCoordinate(34.1638124, -119.0389860),
            GeoCoordinate(34.1636495, -119.0389260),
            GeoCoordinate(34.1634131, -119.0388307),
        ),
        // OSM way 641233466 (service)
        listOf(
            GeoCoordinate(34.1634616, -119.0424773),
            GeoCoordinate(34.1634798, -119.0424044),
            GeoCoordinate(34.1635367, -119.0421890),
            GeoCoordinate(34.1636230, -119.0418582),
            GeoCoordinate(34.1636911, -119.0415979),
            GeoCoordinate(34.1637913, -119.0412189),
            GeoCoordinate(34.1639508, -119.0405986),
            GeoCoordinate(34.1639664, -119.0405212),
            GeoCoordinate(34.1639859, -119.0404293),
            GeoCoordinate(34.1640549, -119.0401735),
            GeoCoordinate(34.1640900, -119.0400994),
        ),
        // OSM way 641233467 (service)
        listOf(
            GeoCoordinate(34.1640900, -119.0400994),
            GeoCoordinate(34.1640523, -119.0400749),
            GeoCoordinate(34.1640129, -119.0400502),
            GeoCoordinate(34.1637991, -119.0399585),
            GeoCoordinate(34.1636387, -119.0398953),
            GeoCoordinate(34.1634682, -119.0398335),
            GeoCoordinate(34.1634181, -119.0398155),
            GeoCoordinate(34.1630266, -119.0396752),
            GeoCoordinate(34.1626435, -119.0395293),
            GeoCoordinate(34.1624414, -119.0394537),
            GeoCoordinate(34.1624139, -119.0394408),
            GeoCoordinate(34.1623623, -119.0394210),
            GeoCoordinate(34.1622315, -119.0393939),
            GeoCoordinate(34.1621406, -119.0394018),
            GeoCoordinate(34.1620810, -119.0394183),
            GeoCoordinate(34.1619794, -119.0394802),
            GeoCoordinate(34.1619424, -119.0394916),
            GeoCoordinate(34.1619277, -119.0394930),
            GeoCoordinate(34.1618527, -119.0394955),
        ),
        // OSM way 646586048 (footway)
        listOf(
            GeoCoordinate(34.1623497, -119.0453133),
            GeoCoordinate(34.1624156, -119.0450690),
            GeoCoordinate(34.1625310, -119.0446417),
            GeoCoordinate(34.1625583, -119.0446225),
            GeoCoordinate(34.1626459, -119.0442698),
            GeoCoordinate(34.1624023, -119.0441806),
            GeoCoordinate(34.1622870, -119.0441509),
            GeoCoordinate(34.1621397, -119.0441088),
        ),
        // OSM way 646586049 (footway)
        listOf(
            GeoCoordinate(34.1593881, -119.0431537),
            GeoCoordinate(34.1593346, -119.0433573),
            GeoCoordinate(34.1592980, -119.0434963),
            GeoCoordinate(34.1590555, -119.0434071),
            GeoCoordinate(34.1590234, -119.0434247),
            GeoCoordinate(34.1589398, -119.0436750),
            GeoCoordinate(34.1588852, -119.0438820),
        ),
        // OSM way 646586051 (footway)
        listOf(
            GeoCoordinate(34.1624127, -119.0420845),
            GeoCoordinate(34.1624257, -119.0420312),
            GeoCoordinate(34.1624832, -119.0417949),
            GeoCoordinate(34.1626077, -119.0412964),
        ),
        // OSM way 654057654 (service)
        listOf(
            GeoCoordinate(34.1657255, -119.0484923),
            GeoCoordinate(34.1656141, -119.0484594),
            GeoCoordinate(34.1655876, -119.0484275),
            GeoCoordinate(34.1655812, -119.0483791),
            GeoCoordinate(34.1657305, -119.0476759),
            GeoCoordinate(34.1659028, -119.0468670),
            GeoCoordinate(34.1659404, -119.0467001),
            GeoCoordinate(34.1659555, -119.0466326),
            GeoCoordinate(34.1661292, -119.0458378),
            GeoCoordinate(34.1661641, -119.0457856),
        ),
        // OSM way 654057655 (service)
        listOf(
            GeoCoordinate(34.1665602, -119.0471101),
            GeoCoordinate(34.1665007, -119.0470908),
            GeoCoordinate(34.1663868, -119.0470431),
            GeoCoordinate(34.1662262, -119.0469816),
            GeoCoordinate(34.1660670, -119.0469263),
            GeoCoordinate(34.1659028, -119.0468670),
        ),
        // OSM way 654057656 (service)
        listOf(
            GeoCoordinate(34.1666063, -119.0468753),
            GeoCoordinate(34.1665472, -119.0468509),
            GeoCoordinate(34.1664391, -119.0468046),
            GeoCoordinate(34.1662784, -119.0467437),
            GeoCoordinate(34.1661152, -119.0466841),
            GeoCoordinate(34.1659555, -119.0466326),
        ),
        // OSM way 654058767 (footway)
        listOf(
            GeoCoordinate(34.1596385, -119.0458909),
            GeoCoordinate(34.1595866, -119.0458724),
            GeoCoordinate(34.1593254, -119.0457762),
            GeoCoordinate(34.1592663, -119.0455739),
            GeoCoordinate(34.1592463, -119.0455544),
            GeoCoordinate(34.1592134, -119.0455433),
        ),
        // OSM way 654058770 (service)
        listOf(
            GeoCoordinate(34.1595754, -119.0460788),
            GeoCoordinate(34.1595015, -119.0460727),
            GeoCoordinate(34.1594499, -119.0460452),
        ),
        // OSM way 654058771 (service)
        listOf(
            GeoCoordinate(34.1587636, -119.0440254),
            GeoCoordinate(34.1588062, -119.0440963),
            GeoCoordinate(34.1589574, -119.0447008),
            GeoCoordinate(34.1591689, -119.0455380),
            GeoCoordinate(34.1591754, -119.0455568),
            GeoCoordinate(34.1592564, -119.0458858),
            GeoCoordinate(34.1594240, -119.0459423),
            GeoCoordinate(34.1594499, -119.0460452),
            GeoCoordinate(34.1591595, -119.0461343),
            GeoCoordinate(34.1586472, -119.0440769),
            GeoCoordinate(34.1586531, -119.0440630),
            GeoCoordinate(34.1587636, -119.0440254),
        ),
        // OSM way 654058772 (service)
        listOf(
            GeoCoordinate(34.1587636, -119.0440254),
            GeoCoordinate(34.1587784, -119.0439552),
            GeoCoordinate(34.1587671, -119.0438909),
        ),
        // OSM way 654058773 (footway)
        listOf(
            GeoCoordinate(34.1591531, -119.0424275),
            GeoCoordinate(34.1588995, -119.0423264),
            GeoCoordinate(34.1584440, -119.0421572),
            GeoCoordinate(34.1584058, -119.0421413),
        ),
        // OSM way 654058775 (footway)
        listOf(
            GeoCoordinate(34.1613808, -119.0437775),
            GeoCoordinate(34.1612940, -119.0438493),
            GeoCoordinate(34.1612049, -119.0438987),
            GeoCoordinate(34.1611318, -119.0439194),
            GeoCoordinate(34.1610650, -119.0439312),
            GeoCoordinate(34.1610030, -119.0439036),
            GeoCoordinate(34.1609596, -119.0438656),
            GeoCoordinate(34.1609261, -119.0437983),
            GeoCoordinate(34.1609111, -119.0437224),
            GeoCoordinate(34.1609143, -119.0436553),
            GeoCoordinate(34.1609247, -119.0436053),
        ),
        // OSM way 654058776 (footway)
        listOf(
            GeoCoordinate(34.1613808, -119.0437775),
            GeoCoordinate(34.1609247, -119.0436053),
        ),
        // OSM way 654058777 (footway)
        listOf(
            GeoCoordinate(34.1621847, -119.0439425),
            GeoCoordinate(34.1622083, -119.0438480),
            GeoCoordinate(34.1622665, -119.0436148),
        ),
        // OSM way 654058779 (footway)
        listOf(
            GeoCoordinate(34.1621918, -119.0434755),
            GeoCoordinate(34.1619108, -119.0433616),
            GeoCoordinate(34.1615415, -119.0432232),
        ),
        // OSM way 654058780 (footway)
        listOf(
            GeoCoordinate(34.1625492, -119.0437181),
            GeoCoordinate(34.1623441, -119.0436401),
        ),
        // OSM way 654058781 (footway)
        listOf(
            GeoCoordinate(34.1625838, -119.0431568),
            GeoCoordinate(34.1623649, -119.0430768),
            GeoCoordinate(34.1623082, -119.0430534),
        ),
        // OSM way 654058782 (service)
        listOf(
            GeoCoordinate(34.1621128, -119.0419728),
            GeoCoordinate(34.1621213, -119.0419176),
            GeoCoordinate(34.1621274, -119.0418776),
            GeoCoordinate(34.1621196, -119.0417406),
            GeoCoordinate(34.1621081, -119.0416906),
            GeoCoordinate(34.1620853, -119.0416550),
            GeoCoordinate(34.1620230, -119.0416264),
            GeoCoordinate(34.1614854, -119.0414152),
            GeoCoordinate(34.1614536, -119.0414031),
            GeoCoordinate(34.1614214, -119.0413915),
        ),
        // OSM way 654058784 (service)
        listOf(
            GeoCoordinate(34.1602340, -119.0411045),
            GeoCoordinate(34.1602768, -119.0411098),
            GeoCoordinate(34.1606132, -119.0412369),
            GeoCoordinate(34.1612079, -119.0414511),
        ),
        // OSM way 654058785 (service)
        listOf(
            GeoCoordinate(34.1605690, -119.0414010),
            GeoCoordinate(34.1605832, -119.0413484),
            GeoCoordinate(34.1605883, -119.0413294),
            GeoCoordinate(34.1606132, -119.0412369),
        ),
        // OSM way 654058786 (service)
        listOf(
            GeoCoordinate(34.1612079, -119.0414511),
            GeoCoordinate(34.1611813, -119.0415525),
            GeoCoordinate(34.1611773, -119.0415676),
            GeoCoordinate(34.1611634, -119.0416205),
        ),
        // OSM way 654058788 (service)
        listOf(
            GeoCoordinate(34.1601252, -119.0449910),
            GeoCoordinate(34.1599372, -119.0449225),
            GeoCoordinate(34.1598922, -119.0449061),
        ),
        // OSM way 654058789 (service)
        listOf(
            GeoCoordinate(34.1608739, -119.0448628),
            GeoCoordinate(34.1601384, -119.0445893),
            GeoCoordinate(34.1601220, -119.0445965),
            GeoCoordinate(34.1600841, -119.0447428),
            GeoCoordinate(34.1600674, -119.0447606),
            GeoCoordinate(34.1599930, -119.0447327),
            GeoCoordinate(34.1599416, -119.0447134),
        ),
        // OSM way 654058790 (service)
        listOf(
            GeoCoordinate(34.1607633, -119.0453062),
            GeoCoordinate(34.1608301, -119.0450374),
            GeoCoordinate(34.1608739, -119.0448628),
            GeoCoordinate(34.1608933, -119.0447727),
            GeoCoordinate(34.1608993, -119.0447504),
            GeoCoordinate(34.1609135, -119.0447011),
        ),
        // OSM way 654058793 (service)
        listOf(
            GeoCoordinate(34.1622474, -119.0454039),
            GeoCoordinate(34.1622228, -119.0454112),
            GeoCoordinate(34.1618400, -119.0452604),
            GeoCoordinate(34.1618639, -119.0451631),
        ),
        // OSM way 654058796 (service)
        listOf(
            GeoCoordinate(34.1622474, -119.0454039),
            GeoCoordinate(34.1622795, -119.0453734),
            GeoCoordinate(34.1623129, -119.0453433),
        ),
        // OSM way 654058797 (service)
        listOf(
            GeoCoordinate(34.1625723, -119.0454932),
            GeoCoordinate(34.1625576, -119.0455512),
            GeoCoordinate(34.1624481, -119.0459833),
            GeoCoordinate(34.1624262, -119.0460699),
            GeoCoordinate(34.1623915, -119.0461876),
        ),
        // OSM way 654058807 (service)
        listOf(
            GeoCoordinate(34.1637356, -119.0474047),
            GeoCoordinate(34.1637966, -119.0471639),
            GeoCoordinate(34.1638396, -119.0469872),
            GeoCoordinate(34.1638281, -119.0468746),
            GeoCoordinate(34.1638302, -119.0468377),
            GeoCoordinate(34.1638535, -119.0467790),
            GeoCoordinate(34.1638715, -119.0467315),
            GeoCoordinate(34.1640192, -119.0461124),
            GeoCoordinate(34.1640236, -119.0460940),
            GeoCoordinate(34.1640382, -119.0460384),
        ),
        // OSM way 654058808 (service)
        listOf(
            GeoCoordinate(34.1644646, -119.0461988),
            GeoCoordinate(34.1644516, -119.0462508),
            GeoCoordinate(34.1642833, -119.0469211),
            GeoCoordinate(34.1642449, -119.0469407),
            GeoCoordinate(34.1641519, -119.0469084),
            GeoCoordinate(34.1641323, -119.0468877),
        ),
        // OSM way 675445780 (footway)
        listOf(
            GeoCoordinate(34.1591531, -119.0424275),
            GeoCoordinate(34.1591225, -119.0425212),
            GeoCoordinate(34.1591625, -119.0425373),
            GeoCoordinate(34.1590382, -119.0430040),
            GeoCoordinate(34.1590071, -119.0431221),
            GeoCoordinate(34.1589716, -119.0431060),
            GeoCoordinate(34.1589133, -119.0433253),
            GeoCoordinate(34.1588488, -119.0434372),
            GeoCoordinate(34.1587550, -119.0438312),
            GeoCoordinate(34.1587671, -119.0438909),
        ),
        // OSM way 679599753 (footway)
        listOf(
            GeoCoordinate(34.1595440, -119.0460051),
            GeoCoordinate(34.1595973, -119.0460287),
            GeoCoordinate(34.1596507, -119.0460553),
        ),
        // OSM way 679599755 (footway)
        listOf(
            GeoCoordinate(34.1595440, -119.0460051),
            GeoCoordinate(34.1595866, -119.0458724),
            GeoCoordinate(34.1597980, -119.0450634),
            GeoCoordinate(34.1599548, -119.0444574),
            GeoCoordinate(34.1599512, -119.0444086),
            GeoCoordinate(34.1599190, -119.0443784),
            GeoCoordinate(34.1595226, -119.0442261),
            GeoCoordinate(34.1588778, -119.0439918),
            GeoCoordinate(34.1588283, -119.0439743),
        ),
        // OSM way 679599757 (footway)
        listOf(
            GeoCoordinate(34.1611545, -119.0445105),
            GeoCoordinate(34.1610716, -119.0444887),
            GeoCoordinate(34.1610456, -119.0444161),
            GeoCoordinate(34.1610350, -119.0443329),
            GeoCoordinate(34.1610381, -119.0442632),
            GeoCoordinate(34.1610602, -119.0442060),
            GeoCoordinate(34.1610854, -119.0441913),
            GeoCoordinate(34.1611205, -119.0442136),
            GeoCoordinate(34.1611456, -119.0442635),
            GeoCoordinate(34.1611573, -119.0443214),
            GeoCoordinate(34.1611600, -119.0443864),
            GeoCoordinate(34.1611545, -119.0445105),
        ),
        // OSM way 679599759 (footway)
        listOf(
            GeoCoordinate(34.1610854, -119.0441913),
            GeoCoordinate(34.1610779, -119.0441224),
            GeoCoordinate(34.1610705, -119.0440321),
            GeoCoordinate(34.1610690, -119.0440084),
        ),
        // OSM way 679826345 (footway)
        listOf(
            GeoCoordinate(34.1652356, -119.0464456),
            GeoCoordinate(34.1655389, -119.0465617),
        ),
        // OSM way 679826346 (footway)
        listOf(
            GeoCoordinate(34.1655389, -119.0465617),
            GeoCoordinate(34.1655885, -119.0465800),
            GeoCoordinate(34.1659030, -119.0466894),
        ),
        // OSM way 679826347 (footway)
        listOf(
            GeoCoordinate(34.1649748, -119.0462882),
            GeoCoordinate(34.1649892, -119.0463538),
            GeoCoordinate(34.1652104, -119.0464349),
            GeoCoordinate(34.1652356, -119.0464456),
        ),
        // OSM way 679826348 (footway)
        listOf(
            GeoCoordinate(34.1648168, -119.0463848),
            GeoCoordinate(34.1648284, -119.0463305),
            GeoCoordinate(34.1648481, -119.0462360),
            GeoCoordinate(34.1649226, -119.0462675),
            GeoCoordinate(34.1649748, -119.0462882),
        ),
        // OSM way 679826349 (footway)
        listOf(
            GeoCoordinate(34.1639707, -119.0460743),
            GeoCoordinate(34.1636913, -119.0459702),
            GeoCoordinate(34.1626962, -119.0455995),
            GeoCoordinate(34.1625576, -119.0455512),
            GeoCoordinate(34.1624238, -119.0454962),
            GeoCoordinate(34.1623905, -119.0454804),
            GeoCoordinate(34.1623501, -119.0454464),
            GeoCoordinate(34.1622795, -119.0453734),
            GeoCoordinate(34.1621771, -119.0452618),
            GeoCoordinate(34.1621530, -119.0452408),
            GeoCoordinate(34.1621072, -119.0452090),
            GeoCoordinate(34.1620565, -119.0451810),
            GeoCoordinate(34.1618600, -119.0451017),
            GeoCoordinate(34.1616475, -119.0450228),
            GeoCoordinate(34.1614046, -119.0449376),
            GeoCoordinate(34.1612568, -119.0448838),
            GeoCoordinate(34.1610318, -119.0447978),
            GeoCoordinate(34.1609194, -119.0447576),
        ),
        // OSM way 679826350 (service)
        listOf(
            GeoCoordinate(34.1641323, -119.0468877),
            GeoCoordinate(34.1640544, -119.0468537),
            GeoCoordinate(34.1639925, -119.0468273),
            GeoCoordinate(34.1638535, -119.0467790),
        ),
        // OSM way 679826351 (service)
        listOf(
            GeoCoordinate(34.1641323, -119.0468877),
            GeoCoordinate(34.1642974, -119.0462585),
            GeoCoordinate(34.1641521, -119.0462050),
            GeoCoordinate(34.1640048, -119.0467790),
            GeoCoordinate(34.1639925, -119.0468273),
        ),
        // OSM way 679826352 (service)
        listOf(
            GeoCoordinate(34.1640544, -119.0468537),
            GeoCoordinate(34.1639584, -119.0472256),
            GeoCoordinate(34.1637966, -119.0471639),
        ),
        // OSM way 679826353 (footway)
        listOf(
            GeoCoordinate(34.1645575, -119.0462904),
            GeoCoordinate(34.1644881, -119.0465691),
            GeoCoordinate(34.1644021, -119.0469138),
            GeoCoordinate(34.1643820, -119.0469851),
            GeoCoordinate(34.1644030, -119.0470300),
            GeoCoordinate(34.1643662, -119.0471770),
        ),
        // OSM way 679835189 (footway)
        listOf(
            GeoCoordinate(34.1621847, -119.0439425),
            GeoCoordinate(34.1621397, -119.0441088),
            GeoCoordinate(34.1620723, -119.0442078),
            GeoCoordinate(34.1620084, -119.0444496),
        ),
        // OSM way 679835191 (footway)
        listOf(
            GeoCoordinate(34.1613808, -119.0437775),
            GeoCoordinate(34.1613992, -119.0437419),
        ),
        // OSM way 679835192 (footway)
        listOf(
            GeoCoordinate(34.1614802, -119.0430199),
            GeoCoordinate(34.1614797, -119.0429693),
            GeoCoordinate(34.1614936, -119.0429059),
            GeoCoordinate(34.1615001, -119.0428698),
            GeoCoordinate(34.1614979, -119.0428463),
            GeoCoordinate(34.1614905, -119.0428150),
            GeoCoordinate(34.1614794, -119.0427839),
            GeoCoordinate(34.1614431, -119.0427084),
            GeoCoordinate(34.1614038, -119.0426240),
            GeoCoordinate(34.1614042, -119.0425899),
            GeoCoordinate(34.1614270, -119.0425036),
        ),
        // OSM way 679835193 (footway)
        listOf(
            GeoCoordinate(34.1613992, -119.0437419),
            GeoCoordinate(34.1614681, -119.0435019),
            GeoCoordinate(34.1614818, -119.0434370),
            GeoCoordinate(34.1615415, -119.0432232),
            GeoCoordinate(34.1615362, -119.0431568),
            GeoCoordinate(34.1615286, -119.0431342),
            GeoCoordinate(34.1614847, -119.0430358),
            GeoCoordinate(34.1614802, -119.0430199),
        ),
        // OSM way 679835194 (footway)
        listOf(
            GeoCoordinate(34.1621337, -119.0436933),
            GeoCoordinate(34.1618562, -119.0435827),
            GeoCoordinate(34.1614818, -119.0434370),
        ),
        // OSM way 679835195 (footway)
        listOf(
            GeoCoordinate(34.1624622, -119.0423553),
            GeoCoordinate(34.1622062, -119.0422564),
        ),
        // OSM way 679835196 (footway)
        listOf(
            GeoCoordinate(34.1621652, -119.0435826),
            GeoCoordinate(34.1622665, -119.0436148),
            GeoCoordinate(34.1623204, -119.0436324),
            GeoCoordinate(34.1623441, -119.0436401),
        ),
        // OSM way 679835197 (service)
        listOf(
            GeoCoordinate(34.1641447, -119.0438728),
            GeoCoordinate(34.1642678, -119.0433922),
            GeoCoordinate(34.1643434, -119.0430933),
            GeoCoordinate(34.1643388, -119.0430376),
        ),
        // OSM way 679845446 (footway)
        listOf(
            GeoCoordinate(34.1595226, -119.0442261),
            GeoCoordinate(34.1595334, -119.0441771),
            GeoCoordinate(34.1595453, -119.0441275),
        ),
        // OSM way 679845450 (footway)
        listOf(
            GeoCoordinate(34.1621631, -119.0420761),
            GeoCoordinate(34.1621053, -119.0420298),
            GeoCoordinate(34.1617856, -119.0419105),
            GeoCoordinate(34.1613864, -119.0417594),
            GeoCoordinate(34.1610699, -119.0416396),
            GeoCoordinate(34.1603949, -119.0413822),
            GeoCoordinate(34.1599241, -119.0412096),
            GeoCoordinate(34.1598864, -119.0411950),
            GeoCoordinate(34.1598645, -119.0411859),
            GeoCoordinate(34.1596785, -119.0411129),
            GeoCoordinate(34.1595668, -119.0410555),
            GeoCoordinate(34.1589105, -119.0408071),
            GeoCoordinate(34.1588882, -119.0408034),
            GeoCoordinate(34.1588684, -119.0408038),
            GeoCoordinate(34.1588407, -119.0408126),
            GeoCoordinate(34.1588153, -119.0408276),
            GeoCoordinate(34.1587904, -119.0408571),
            GeoCoordinate(34.1587724, -119.0408973),
            GeoCoordinate(34.1587219, -119.0410846),
            GeoCoordinate(34.1585487, -119.0417572),
            GeoCoordinate(34.1584440, -119.0421572),
            GeoCoordinate(34.1584099, -119.0423028),
            GeoCoordinate(34.1583488, -119.0425640),
            GeoCoordinate(34.1583534, -119.0426450),
            GeoCoordinate(34.1583713, -119.0427310),
            GeoCoordinate(34.1586253, -119.0437098),
            GeoCoordinate(34.1586481, -119.0437501),
            GeoCoordinate(34.1587019, -119.0438078),
            GeoCoordinate(34.1587550, -119.0438312),
            GeoCoordinate(34.1588852, -119.0438820),
            GeoCoordinate(34.1595453, -119.0441275),
            GeoCoordinate(34.1599785, -119.0442923),
            GeoCoordinate(34.1603496, -119.0444288),
            GeoCoordinate(34.1606457, -119.0445412),
            GeoCoordinate(34.1606816, -119.0445546),
            GeoCoordinate(34.1611028, -119.0447108),
            GeoCoordinate(34.1613854, -119.0448153),
            GeoCoordinate(34.1614154, -119.0448177),
            GeoCoordinate(34.1614476, -119.0448126),
        ),
        // OSM way 679845451 (service)
        listOf(
            GeoCoordinate(34.1597627, -119.0417042),
            GeoCoordinate(34.1598864, -119.0411950),
            GeoCoordinate(34.1598984, -119.0411458),
        ),
        // OSM way 679845452 (footway)
        listOf(
            GeoCoordinate(34.1600893, -119.0444624),
            GeoCoordinate(34.1600147, -119.0444353),
            GeoCoordinate(34.1599512, -119.0444086),
        ),
        // OSM way 679845453 (footway)
        listOf(
            GeoCoordinate(34.1599512, -119.0444086),
            GeoCoordinate(34.1599670, -119.0443394),
            GeoCoordinate(34.1599785, -119.0442923),
        ),
        // OSM way 679845454 (path)
        listOf(
            GeoCoordinate(34.1596507, -119.0460553),
            GeoCoordinate(34.1597159, -119.0461212),
            GeoCoordinate(34.1597103, -119.0461565),
        ),
        // OSM way 686527762 (footway)
        listOf(
            GeoCoordinate(34.1629212, -119.0400986),
            GeoCoordinate(34.1630091, -119.0397466),
            GeoCoordinate(34.1630266, -119.0396752),
        ),
        // OSM way 686988467 (footway)
        listOf(
            GeoCoordinate(34.1628544, -119.0403729),
            GeoCoordinate(34.1629212, -119.0400986),
        ),
        // OSM way 686988478 (footway)
        listOf(
            GeoCoordinate(34.1611702, -119.0444515),
            GeoCoordinate(34.1612411, -119.0441836),
            GeoCoordinate(34.1610779, -119.0441224),
            GeoCoordinate(34.1608661, -119.0440433),
            GeoCoordinate(34.1607801, -119.0443774),
            GeoCoordinate(34.1610716, -119.0444887),
        ),
        // OSM way 686988480 (footway)
        listOf(
            GeoCoordinate(34.1606207, -119.0446375),
            GeoCoordinate(34.1606322, -119.0445937),
            GeoCoordinate(34.1606457, -119.0445412),
        ),
        // OSM way 686988481 (footway)
        listOf(
            GeoCoordinate(34.1621631, -119.0420761),
            GeoCoordinate(34.1621666, -119.0421396),
            GeoCoordinate(34.1621150, -119.0423356),
            GeoCoordinate(34.1619879, -119.0428231),
            GeoCoordinate(34.1618977, -119.0431704),
            GeoCoordinate(34.1618818, -119.0432003),
            GeoCoordinate(34.1618494, -119.0432312),
            GeoCoordinate(34.1618143, -119.0432453),
            GeoCoordinate(34.1617866, -119.0432455),
            GeoCoordinate(34.1617569, -119.0432378),
            GeoCoordinate(34.1615613, -119.0431658),
            GeoCoordinate(34.1615362, -119.0431568),
            GeoCoordinate(34.1615211, -119.0431570),
            GeoCoordinate(34.1614999, -119.0431611),
            GeoCoordinate(34.1614817, -119.0431677),
            GeoCoordinate(34.1614625, -119.0431888),
            GeoCoordinate(34.1614473, -119.0432224),
            GeoCoordinate(34.1614289, -119.0432923),
            GeoCoordinate(34.1614106, -119.0433622),
            GeoCoordinate(34.1614079, -119.0434018),
            GeoCoordinate(34.1614095, -119.0434319),
            GeoCoordinate(34.1614170, -119.0434571),
            GeoCoordinate(34.1614289, -119.0434759),
            GeoCoordinate(34.1614425, -119.0434863),
            GeoCoordinate(34.1614681, -119.0435019),
        ),
        // OSM way 686988484 (footway)
        listOf(
            GeoCoordinate(34.1627023, -119.0432024),
            GeoCoordinate(34.1627614, -119.0429887),
            GeoCoordinate(34.1627864, -119.0429978),
        ),
        // OSM way 686988485 (steps)
        listOf(
            GeoCoordinate(34.1627318, -119.0432129),
            GeoCoordinate(34.1627023, -119.0432024),
        ),
        // OSM way 686988487 (footway)
        listOf(
            GeoCoordinate(34.1628487, -119.0428880),
            GeoCoordinate(34.1627639, -119.0428548),
        ),
        // OSM way 686988488 (footway)
        listOf(
            GeoCoordinate(34.1628856, -119.0428270),
            GeoCoordinate(34.1628631, -119.0428391),
            GeoCoordinate(34.1628487, -119.0428880),
            GeoCoordinate(34.1628290, -119.0429591),
            GeoCoordinate(34.1628082, -119.0429545),
            GeoCoordinate(34.1627969, -119.0429619),
            GeoCoordinate(34.1627864, -119.0429978),
            GeoCoordinate(34.1627318, -119.0432129),
            GeoCoordinate(34.1626797, -119.0434180),
            GeoCoordinate(34.1627209, -119.0434821),
            GeoCoordinate(34.1629229, -119.0435579),
            GeoCoordinate(34.1629433, -119.0435319),
            GeoCoordinate(34.1629806, -119.0433868),
            GeoCoordinate(34.1629928, -119.0433787),
            GeoCoordinate(34.1631042, -119.0434185),
        ),
        // OSM way 686988490 (footway)
        listOf(
            GeoCoordinate(34.1632911, -119.0429307),
            GeoCoordinate(34.1632788, -119.0429271),
            GeoCoordinate(34.1631659, -119.0429340),
            GeoCoordinate(34.1629545, -119.0428533),
        ),
        // OSM way 686988491 (footway)
        listOf(
            GeoCoordinate(34.1633451, -119.0427207),
            GeoCoordinate(34.1632911, -119.0429307),
            GeoCoordinate(34.1632821, -119.0429652),
        ),
        // OSM way 686988493 (footway)
        listOf(
            GeoCoordinate(34.1626362, -119.0421106),
            GeoCoordinate(34.1626230, -119.0421635),
            GeoCoordinate(34.1626043, -119.0422443),
        ),
        // OSM way 690830273 (service)
        listOf(
            GeoCoordinate(34.1633620, -119.0402871),
            GeoCoordinate(34.1633229, -119.0404312),
            GeoCoordinate(34.1629270, -119.0402788),
        ),
        // OSM way 690830279 (footway)
        listOf(
            GeoCoordinate(34.1639582, -119.0404302),
            GeoCoordinate(34.1639338, -119.0405071),
            GeoCoordinate(34.1639173, -119.0405635),
        ),
        // OSM way 712128814 (steps)
        listOf(
            GeoCoordinate(34.1630674, -119.0398279),
            GeoCoordinate(34.1629910, -119.0401454),
        ),
        // OSM way 712128815 (service)
        listOf(
            GeoCoordinate(34.1639121, -119.0412654),
            GeoCoordinate(34.1638380, -119.0412373),
            GeoCoordinate(34.1638280, -119.0412336),
            GeoCoordinate(34.1637913, -119.0412189),
        ),
        // OSM way 712128816 (service)
        listOf(
            GeoCoordinate(34.1638122, -119.0416498),
            GeoCoordinate(34.1643390, -119.0418372),
            GeoCoordinate(34.1643850, -119.0418313),
        ),
        // OSM way 712128817 (service)
        listOf(
            GeoCoordinate(34.1644994, -119.0416817),
            GeoCoordinate(34.1644265, -119.0417031),
            GeoCoordinate(34.1644020, -119.0417106),
            GeoCoordinate(34.1643623, -119.0417250),
        ),
        // OSM way 712128818 (service)
        listOf(
            GeoCoordinate(34.1644161, -119.0420737),
            GeoCoordinate(34.1644025, -119.0420774),
            GeoCoordinate(34.1637633, -119.0418375),
        ),
        // OSM way 712128819 (service)
        listOf(
            GeoCoordinate(34.1639121, -119.0412654),
            GeoCoordinate(34.1638614, -119.0414605),
            GeoCoordinate(34.1638122, -119.0416498),
        ),
        // OSM way 712128820 (service)
        listOf(
            GeoCoordinate(34.1639121, -119.0412654),
            GeoCoordinate(34.1642994, -119.0414049),
        ),
        // OSM way 712128821 (service)
        listOf(
            GeoCoordinate(34.1643420, -119.0416320),
            GeoCoordinate(34.1643239, -119.0416286),
            GeoCoordinate(34.1638614, -119.0414605),
        ),
        // OSM way 712128822 (service)
        listOf(
            GeoCoordinate(34.1638122, -119.0416498),
            GeoCoordinate(34.1637390, -119.0416178),
            GeoCoordinate(34.1637278, -119.0416130),
            GeoCoordinate(34.1636911, -119.0415979),
        ),
        // OSM way 712128823 (service)
        listOf(
            GeoCoordinate(34.1643850, -119.0418313),
            GeoCoordinate(34.1643623, -119.0417250),
            GeoCoordinate(34.1643420, -119.0416320),
        ),
        // OSM way 712128847 (service)
        listOf(
            GeoCoordinate(34.1617994, -119.0418588),
            GeoCoordinate(34.1617856, -119.0419105),
            GeoCoordinate(34.1617791, -119.0419350),
            GeoCoordinate(34.1616883, -119.0422753),
        ),
        // OSM way 712128848 (footway)
        listOf(
            GeoCoordinate(34.1611937, -119.0427549),
            GeoCoordinate(34.1612804, -119.0427300),
            GeoCoordinate(34.1612987, -119.0427316),
        ),
        // OSM way 712128849 (footway)
        listOf(
            GeoCoordinate(34.1612509, -119.0428825),
            GeoCoordinate(34.1612656, -119.0428192),
            GeoCoordinate(34.1612808, -119.0427645),
            GeoCoordinate(34.1612987, -119.0427316),
            GeoCoordinate(34.1613838, -119.0426558),
            GeoCoordinate(34.1614038, -119.0426240),
        ),
        // OSM way 712128850 (footway)
        listOf(
            GeoCoordinate(34.1614936, -119.0429059),
            GeoCoordinate(34.1612656, -119.0428192),
            GeoCoordinate(34.1612400, -119.0427849),
            GeoCoordinate(34.1612231, -119.0427711),
            GeoCoordinate(34.1611937, -119.0427549),
            GeoCoordinate(34.1611418, -119.0427327),
            GeoCoordinate(34.1611160, -119.0428299),
        ),
        // OSM way 712128851 (footway)
        listOf(
            GeoCoordinate(34.1628377, -119.0413823),
            GeoCoordinate(34.1626077, -119.0412964),
            GeoCoordinate(34.1623656, -119.0412038),
        ),
        // OSM way 712128853 (footway)
        listOf(
            GeoCoordinate(34.1644881, -119.0465691),
            GeoCoordinate(34.1645328, -119.0466003),
            GeoCoordinate(34.1645649, -119.0466479),
            GeoCoordinate(34.1645914, -119.0467067),
            GeoCoordinate(34.1646003, -119.0467653),
            GeoCoordinate(34.1645936, -119.0468148),
            GeoCoordinate(34.1645717, -119.0468614),
            GeoCoordinate(34.1645332, -119.0468969),
            GeoCoordinate(34.1644942, -119.0469153),
            GeoCoordinate(34.1644478, -119.0469211),
            GeoCoordinate(34.1644021, -119.0469138),
        ),
        // OSM way 712146765 (footway)
        listOf(
            GeoCoordinate(34.1610695, -119.0449570),
            GeoCoordinate(34.1610780, -119.0449273),
        ),
        // OSM way 712146766 (footway)
        listOf(
            GeoCoordinate(34.1611233, -119.0449779),
            GeoCoordinate(34.1611379, -119.0449246),
        ),
        // OSM way 712146767 (footway)
        listOf(
            GeoCoordinate(34.1610508, -119.0450262),
            GeoCoordinate(34.1610336, -119.0450749),
            GeoCoordinate(34.1611589, -119.0451213),
            GeoCoordinate(34.1611900, -119.0449986),
            GeoCoordinate(34.1612031, -119.0449445),
            GeoCoordinate(34.1611379, -119.0449246),
            GeoCoordinate(34.1610402, -119.0448891),
            GeoCoordinate(34.1609963, -119.0450594),
            GeoCoordinate(34.1610336, -119.0450749),
        ),
        // OSM way 712146768 (footway)
        listOf(
            GeoCoordinate(34.1612229, -119.0450110),
            GeoCoordinate(34.1611900, -119.0449986),
            GeoCoordinate(34.1611424, -119.0449863),
            GeoCoordinate(34.1611233, -119.0449779),
            GeoCoordinate(34.1610695, -119.0449570),
            GeoCoordinate(34.1610508, -119.0450262),
            GeoCoordinate(34.1611233, -119.0450552),
            GeoCoordinate(34.1611424, -119.0449863),
        ),
        // OSM way 712146769 (footway)
        listOf(
            GeoCoordinate(34.1612568, -119.0448838),
            GeoCoordinate(34.1612229, -119.0450110),
            GeoCoordinate(34.1611819, -119.0451644),
            GeoCoordinate(34.1610236, -119.0451103),
            GeoCoordinate(34.1609570, -119.0450875),
            GeoCoordinate(34.1610318, -119.0447978),
        ),
        // OSM way 712146771 (footway)
        listOf(
            GeoCoordinate(34.1621150, -119.0423356),
            GeoCoordinate(34.1618963, -119.0422455),
        ),
        // OSM way 712146772 (footway)
        listOf(
            GeoCoordinate(34.1620486, -119.0428464),
            GeoCoordinate(34.1619879, -119.0428231),
            GeoCoordinate(34.1618898, -119.0427852),
            GeoCoordinate(34.1618900, -119.0427623),
            GeoCoordinate(34.1617907, -119.0427214),
            GeoCoordinate(34.1617315, -119.0427015),
        ),
        // OSM way 712146773 (footway)
        listOf(
            GeoCoordinate(34.1622262, -119.0433509),
            GeoCoordinate(34.1622889, -119.0433769),
            GeoCoordinate(34.1623649, -119.0430768),
            GeoCoordinate(34.1625277, -119.0424453),
            GeoCoordinate(34.1625361, -119.0424164),
            GeoCoordinate(34.1625628, -119.0423083),
            GeoCoordinate(34.1626043, -119.0422443),
        ),
        // OSM way 712146775 (service)
        listOf(
            GeoCoordinate(34.1628788, -119.0430141),
            GeoCoordinate(34.1629242, -119.0428418),
            GeoCoordinate(34.1629891, -119.0426004),
            GeoCoordinate(34.1629928, -119.0425861),
            GeoCoordinate(34.1630020, -119.0425411),
        ),
        // OSM way 712146776 (service)
        listOf(
            GeoCoordinate(34.1635247, -119.0432178),
            GeoCoordinate(34.1634699, -119.0434229),
        ),
        // OSM way 712146777 (service)
        listOf(
            GeoCoordinate(34.1638270, -119.0428531),
            GeoCoordinate(34.1638156, -119.0428995),
            GeoCoordinate(34.1637962, -119.0429711),
        ),
        // OSM way 712146778 (footway)
        listOf(
            GeoCoordinate(34.1639068, -119.0429322),
            GeoCoordinate(34.1639022, -119.0429516),
            GeoCoordinate(34.1638874, -119.0430083),
            GeoCoordinate(34.1640172, -119.0430553),
            GeoCoordinate(34.1640314, -119.0429983),
            GeoCoordinate(34.1639022, -119.0429516),
        ),
        // OSM way 712146779 (footway)
        listOf(
            GeoCoordinate(34.1642678, -119.0433922),
            GeoCoordinate(34.1642198, -119.0433733),
            GeoCoordinate(34.1641210, -119.0431464),
            GeoCoordinate(34.1642376, -119.0430732),
            GeoCoordinate(34.1642416, -119.0430575),
        ),
        // OSM way 712146781 (service)
        listOf(
            GeoCoordinate(34.1601178, -119.0412283),
            GeoCoordinate(34.1602340, -119.0411045),
        ),
        // OSM way 712146782 (service)
        listOf(
            GeoCoordinate(34.1602340, -119.0411045),
            GeoCoordinate(34.1603389, -119.0409985),
            GeoCoordinate(34.1604055, -119.0409757),
            GeoCoordinate(34.1604305, -119.0409717),
            GeoCoordinate(34.1606496, -119.0410575),
        ),
        // OSM way 712148229 (service)
        listOf(
            GeoCoordinate(34.1637633, -119.0418375),
            GeoCoordinate(34.1638122, -119.0416498),
        ),
        // OSM way 712148230 (service)
        listOf(
            GeoCoordinate(34.1643850, -119.0418313),
            GeoCoordinate(34.1644210, -119.0420426),
            GeoCoordinate(34.1644161, -119.0420737),
        ),
        // OSM way 712148231 (service)
        listOf(
            GeoCoordinate(34.1642994, -119.0414049),
            GeoCoordinate(34.1643420, -119.0416320),
        ),
        // OSM way 712148232 (service)
        listOf(
            GeoCoordinate(34.1595909, -119.0410220),
            GeoCoordinate(34.1596018, -119.0409656),
            GeoCoordinate(34.1596265, -119.0408382),
        ),
        // OSM way 712148233 (service)
        listOf(
            GeoCoordinate(34.1586923, -119.0410592),
            GeoCoordinate(34.1586612, -119.0410465),
            GeoCoordinate(34.1585880, -119.0410168),
        ),
        // OSM way 712148234 (service)
        listOf(
            GeoCoordinate(34.1585127, -119.0399173),
            GeoCoordinate(34.1589417, -119.0400766),
        ),
        // OSM way 712148235 (service)
        listOf(
            GeoCoordinate(34.1589035, -119.0402304),
            GeoCoordinate(34.1582954, -119.0400009),
        ),
        // OSM way 712148236 (service)
        listOf(
            GeoCoordinate(34.1589338, -119.0405680),
            GeoCoordinate(34.1581421, -119.0402864),
        ),
        // OSM way 712148237 (service)
        listOf(
            GeoCoordinate(34.1581803, -119.0401193),
            GeoCoordinate(34.1588305, -119.0403588),
            GeoCoordinate(34.1588754, -119.0403402),
        ),
        // OSM way 712148238 (service)
        listOf(
            GeoCoordinate(34.1589338, -119.0405680),
            GeoCoordinate(34.1589810, -119.0403762),
            GeoCoordinate(34.1590328, -119.0403970),
            GeoCoordinate(34.1596479, -119.0406472),
            GeoCoordinate(34.1596562, -119.0406728),
            GeoCoordinate(34.1596265, -119.0408382),
            GeoCoordinate(34.1596054, -119.0408215),
            GeoCoordinate(34.1589338, -119.0405680),
        ),
        // OSM way 712148239 (service)
        listOf(
            GeoCoordinate(34.1580958, -119.0404673),
            GeoCoordinate(34.1581421, -119.0402864),
            GeoCoordinate(34.1581803, -119.0401193),
            GeoCoordinate(34.1582127, -119.0400363),
            GeoCoordinate(34.1582954, -119.0400009),
            GeoCoordinate(34.1585127, -119.0399173),
            GeoCoordinate(34.1588708, -119.0398050),
            GeoCoordinate(34.1589974, -119.0398638),
            GeoCoordinate(34.1589417, -119.0400766),
            GeoCoordinate(34.1589035, -119.0402304),
            GeoCoordinate(34.1588754, -119.0403402),
            GeoCoordinate(34.1589597, -119.0403697),
            GeoCoordinate(34.1589810, -119.0403762),
        ),
        // OSM way 712148240 (service)
        listOf(
            GeoCoordinate(34.1581803, -119.0408754),
            GeoCoordinate(34.1579951, -119.0415926),
        ),
        // OSM way 712148241 (service)
        listOf(
            GeoCoordinate(34.1583398, -119.0409355),
            GeoCoordinate(34.1580717, -119.0419663),
        ),
        // OSM way 712148242 (service)
        listOf(
            GeoCoordinate(34.1580072, -119.0408100),
            GeoCoordinate(34.1579852, -119.0409556),
            GeoCoordinate(34.1579508, -119.0412882),
            GeoCoordinate(34.1579951, -119.0415926),
            GeoCoordinate(34.1580162, -119.0417509),
            GeoCoordinate(34.1580717, -119.0419663),
            GeoCoordinate(34.1581527, -119.0422806),
            GeoCoordinate(34.1581983, -119.0424067),
            GeoCoordinate(34.1582249, -119.0424379),
            GeoCoordinate(34.1582775, -119.0424642),
            GeoCoordinate(34.1583201, -119.0424853),
        ),
        // OSM way 712148243 (service)
        listOf(
            GeoCoordinate(34.1584969, -119.0409948),
            GeoCoordinate(34.1581527, -119.0422806),
        ),
        // OSM way 712148244 (service)
        listOf(
            GeoCoordinate(34.1584340, -119.0409711),
            GeoCoordinate(34.1585205, -119.0406276),
        ),
        // OSM way 712148245 (service)
        listOf(
            GeoCoordinate(34.1582749, -119.0409110),
            GeoCoordinate(34.1583671, -119.0405696),
        ),
        // OSM way 712148246 (service)
        listOf(
            GeoCoordinate(34.1581178, -119.0408518),
            GeoCoordinate(34.1582097, -119.0405102),
        ),
        // OSM way 712148247 (service)
        listOf(
            GeoCoordinate(34.1585880, -119.0410168),
            GeoCoordinate(34.1584969, -119.0409948),
            GeoCoordinate(34.1584340, -119.0409711),
            GeoCoordinate(34.1583398, -119.0409355),
            GeoCoordinate(34.1582749, -119.0409110),
            GeoCoordinate(34.1581803, -119.0408754),
            GeoCoordinate(34.1581178, -119.0408518),
            GeoCoordinate(34.1580072, -119.0408100),
            GeoCoordinate(34.1580958, -119.0404673),
            GeoCoordinate(34.1582097, -119.0405102),
            GeoCoordinate(34.1583671, -119.0405696),
            GeoCoordinate(34.1585205, -119.0406276),
            GeoCoordinate(34.1586739, -119.0406854),
            GeoCoordinate(34.1585880, -119.0410168),
        ),
        // OSM way 714860850 (service)
        listOf(
            GeoCoordinate(34.1630899, -119.0461041),
            GeoCoordinate(34.1627936, -119.0460049),
        ),
        // OSM way 714860851 (service)
        listOf(
            GeoCoordinate(34.1618086, -119.0461235),
            GeoCoordinate(34.1622177, -119.0460263),
            GeoCoordinate(34.1622576, -119.0460290),
            GeoCoordinate(34.1623364, -119.0460451),
            GeoCoordinate(34.1624262, -119.0460699),
            GeoCoordinate(34.1624929, -119.0461081),
            GeoCoordinate(34.1626138, -119.0461363),
            GeoCoordinate(34.1630899, -119.0461041),
            GeoCoordinate(34.1632103, -119.0461449),
            GeoCoordinate(34.1632641, -119.0461631),
            GeoCoordinate(34.1637546, -119.0463388),
            GeoCoordinate(34.1637246, -119.0465574),
            GeoCoordinate(34.1632253, -119.0463924),
            GeoCoordinate(34.1632103, -119.0461449),
        ),
        // OSM way 714860852 (service)
        listOf(
            GeoCoordinate(34.1618736, -119.0463820),
            GeoCoordinate(34.1618086, -119.0461235),
            GeoCoordinate(34.1617614, -119.0459262),
            GeoCoordinate(34.1616484, -119.0458868),
        ),
        // OSM way 715646565 (footway)
        listOf(
            GeoCoordinate(34.1585738, -119.0426660),
            GeoCoordinate(34.1583713, -119.0427310),
            GeoCoordinate(34.1583259, -119.0427428),
        ),
        // OSM way 715646566 (footway)
        listOf(
            GeoCoordinate(34.1588995, -119.0423264),
            GeoCoordinate(34.1588064, -119.0426937),
            GeoCoordinate(34.1588883, -119.0427240),
            GeoCoordinate(34.1588124, -119.0430231),
            GeoCoordinate(34.1585115, -119.0429117),
            GeoCoordinate(34.1585738, -119.0426660),
            GeoCoordinate(34.1585809, -119.0426381),
            GeoCoordinate(34.1583488, -119.0425640),
            GeoCoordinate(34.1583100, -119.0425509),
        ),
        // OSM way 715646567 (path)
        listOf(
            GeoCoordinate(34.1601702, -119.0465111),
            GeoCoordinate(34.1602479, -119.0465225),
            GeoCoordinate(34.1602912, -119.0465346),
            GeoCoordinate(34.1603494, -119.0465508),
            GeoCoordinate(34.1604022, -119.0465708),
            GeoCoordinate(34.1604432, -119.0465775),
            GeoCoordinate(34.1604932, -119.0466003),
            GeoCoordinate(34.1605675, -119.0466325),
            GeoCoordinate(34.1606452, -119.0466808),
            GeoCoordinate(34.1607029, -119.0467183),
            GeoCoordinate(34.1607650, -119.0467559),
            GeoCoordinate(34.1607994, -119.0467894),
            GeoCoordinate(34.1608427, -119.0468256),
            GeoCoordinate(34.1608904, -119.0468685),
            GeoCoordinate(34.1609271, -119.0469128),
            GeoCoordinate(34.1609515, -119.0469530),
            GeoCoordinate(34.1609925, -119.0470375),
            GeoCoordinate(34.1610114, -119.0470885),
            GeoCoordinate(34.1610458, -119.0471864),
            GeoCoordinate(34.1610702, -119.0472588),
            GeoCoordinate(34.1610902, -119.0473178),
            GeoCoordinate(34.1611068, -119.0474103),
            GeoCoordinate(34.1611035, -119.0474653),
            GeoCoordinate(34.1611213, -119.0475579),
            GeoCoordinate(34.1611357, -119.0476061),
            GeoCoordinate(34.1611503, -119.0476351),
        ),
        // OSM way 715646568 (path)
        listOf(
            GeoCoordinate(34.1607633, -119.0453062),
            GeoCoordinate(34.1607806, -119.0453397),
            GeoCoordinate(34.1607528, -119.0454148),
            GeoCoordinate(34.1607351, -119.0454885),
            GeoCoordinate(34.1607318, -119.0455140),
            GeoCoordinate(34.1607351, -119.0455395),
            GeoCoordinate(34.1607395, -119.0455744),
            GeoCoordinate(34.1607584, -119.0456347),
            GeoCoordinate(34.1607806, -119.0456763),
            GeoCoordinate(34.1608006, -119.0457045),
            GeoCoordinate(34.1608216, -119.0457393),
            GeoCoordinate(34.1608361, -119.0457581),
            GeoCoordinate(34.1608449, -119.0457755),
            GeoCoordinate(34.1608494, -119.0458131),
            GeoCoordinate(34.1608549, -119.0458466),
            GeoCoordinate(34.1608638, -119.0458882),
            GeoCoordinate(34.1608627, -119.0459164),
            GeoCoordinate(34.1608694, -119.0459552),
            GeoCoordinate(34.1608738, -119.0459767),
            GeoCoordinate(34.1608927, -119.0460049),
            GeoCoordinate(34.1609160, -119.0460518),
            GeoCoordinate(34.1609204, -119.0460733),
            GeoCoordinate(34.1609237, -119.0461350),
            GeoCoordinate(34.1609326, -119.0461698),
            GeoCoordinate(34.1609459, -119.0461980),
            GeoCoordinate(34.1609604, -119.0462208),
            GeoCoordinate(34.1609626, -119.0462945),
            GeoCoordinate(34.1609792, -119.0463509),
            GeoCoordinate(34.1609970, -119.0463763),
            GeoCoordinate(34.1610158, -119.0463978),
            GeoCoordinate(34.1610491, -119.0464273),
            GeoCoordinate(34.1610813, -119.0464568),
            GeoCoordinate(34.1610969, -119.0464702),
            GeoCoordinate(34.1611091, -119.0465145),
            GeoCoordinate(34.1611124, -119.0465735),
            GeoCoordinate(34.1611179, -119.0466057),
            GeoCoordinate(34.1611324, -119.0466312),
            GeoCoordinate(34.1611457, -119.0466593),
            GeoCoordinate(34.1611590, -119.0466888),
            GeoCoordinate(34.1611690, -119.0467224),
            GeoCoordinate(34.1611779, -119.0467854),
            GeoCoordinate(34.1611812, -119.0468283),
            GeoCoordinate(34.1611823, -119.0468900),
            GeoCoordinate(34.1611801, -119.0469383),
            GeoCoordinate(34.1611878, -119.0469959),
            GeoCoordinate(34.1611967, -119.0470362),
            GeoCoordinate(34.1612200, -119.0470845),
            GeoCoordinate(34.1612234, -119.0471260),
            GeoCoordinate(34.1612089, -119.0471622),
            GeoCoordinate(34.1612078, -119.0472025),
            GeoCoordinate(34.1612123, -119.0472709),
            GeoCoordinate(34.1611856, -119.0473741),
            GeoCoordinate(34.1611856, -119.0474425),
            GeoCoordinate(34.1611790, -119.0474895),
            GeoCoordinate(34.1611679, -119.0475324),
            GeoCoordinate(34.1611579, -119.0476035),
            GeoCoordinate(34.1611503, -119.0476351),
            GeoCoordinate(34.1611457, -119.0476544),
            GeoCoordinate(34.1611379, -119.0477107),
            GeoCoordinate(34.1611235, -119.0477885),
            GeoCoordinate(34.1611102, -119.0478891),
            GeoCoordinate(34.1610858, -119.0479937),
            GeoCoordinate(34.1610724, -119.0480755),
            GeoCoordinate(34.1610380, -119.0482070),
            GeoCoordinate(34.1610280, -119.0482727),
            GeoCoordinate(34.1610247, -119.0483384),
            GeoCoordinate(34.1610103, -119.0483853),
        ),
        // OSM way 723728189 (service)
        listOf(
            GeoCoordinate(34.1618447, -119.0470484),
            GeoCoordinate(34.1619205, -119.0470149),
            GeoCoordinate(34.1622354, -119.0469104),
            GeoCoordinate(34.1623869, -119.0469199),
            GeoCoordinate(34.1627702, -119.0467894),
            GeoCoordinate(34.1631091, -119.0467346),
            GeoCoordinate(34.1633077, -119.0467391),
            GeoCoordinate(34.1636465, -119.0467444),
        ),
        // OSM way 723728190 (service)
        listOf(
            GeoCoordinate(34.1636465, -119.0467444),
            GeoCoordinate(34.1637407, -119.0467636),
            GeoCoordinate(34.1638535, -119.0467790),
        ),
        // OSM way 768029990 (footway)
        listOf(
            GeoCoordinate(34.1587384, -119.0439401),
            GeoCoordinate(34.1586358, -119.0438918),
            GeoCoordinate(34.1585875, -119.0438543),
            GeoCoordinate(34.1585642, -119.0438214),
        ),
        // OSM way 768029991 (footway)
        listOf(
            GeoCoordinate(34.1587384, -119.0439401),
            GeoCoordinate(34.1587784, -119.0439552),
            GeoCoordinate(34.1588283, -119.0439743),
        ),
        // OSM way 770244856 (footway)
        listOf(
            GeoCoordinate(34.1643794, -119.0429027),
            GeoCoordinate(34.1643533, -119.0428752),
            GeoCoordinate(34.1634948, -119.0425515),
            GeoCoordinate(34.1628345, -119.0423023),
            GeoCoordinate(34.1626677, -119.0422402),
            GeoCoordinate(34.1626043, -119.0422443),
        ),
        // OSM way 770244857 (footway)
        listOf(
            GeoCoordinate(34.1634307, -119.0423859),
            GeoCoordinate(34.1634798, -119.0424044),
            GeoCoordinate(34.1635270, -119.0424217),
            GeoCoordinate(34.1635084, -119.0424943),
            GeoCoordinate(34.1634948, -119.0425515),
        ),
        // OSM way 812573177 (footway)
        listOf(
            GeoCoordinate(34.1618852, -119.0450068),
            GeoCoordinate(34.1618724, -119.0450537),
            GeoCoordinate(34.1618600, -119.0451017),
        ),
        // OSM way 812573178 (footway)
        listOf(
            GeoCoordinate(34.1614476, -119.0448126),
            GeoCoordinate(34.1614239, -119.0448854),
            GeoCoordinate(34.1614046, -119.0449376),
        ),
        // OSM way 812573179 (footway)
        listOf(
            GeoCoordinate(34.1618729, -119.0449647),
            GeoCoordinate(34.1618852, -119.0450068),
            GeoCoordinate(34.1620896, -119.0450888),
            GeoCoordinate(34.1621586, -119.0451222),
            GeoCoordinate(34.1622285, -119.0451811),
            GeoCoordinate(34.1623497, -119.0453133),
            GeoCoordinate(34.1624120, -119.0453677),
            GeoCoordinate(34.1624382, -119.0453881),
            GeoCoordinate(34.1624718, -119.0454022),
            GeoCoordinate(34.1626123, -119.0454556),
            GeoCoordinate(34.1629193, -119.0455724),
            GeoCoordinate(34.1634908, -119.0457857),
            GeoCoordinate(34.1638968, -119.0459417),
            GeoCoordinate(34.1647488, -119.0462494),
            GeoCoordinate(34.1648481, -119.0462360),
            GeoCoordinate(34.1649057, -119.0461509),
            GeoCoordinate(34.1651852, -119.0450758),
            GeoCoordinate(34.1652450, -119.0448459),
            GeoCoordinate(34.1652867, -119.0446850),
            GeoCoordinate(34.1653545, -119.0444254),
            GeoCoordinate(34.1653488, -119.0443125),
            GeoCoordinate(34.1654772, -119.0438275),
            GeoCoordinate(34.1655338, -119.0437335),
            GeoCoordinate(34.1655995, -119.0434811),
            GeoCoordinate(34.1656023, -119.0433975),
            GeoCoordinate(34.1655520, -119.0433434),
            GeoCoordinate(34.1651785, -119.0432046),
            GeoCoordinate(34.1651504, -119.0431944),
        ),
        // OSM way 812573180 (footway)
        listOf(
            GeoCoordinate(34.1615179, -119.0437380),
            GeoCoordinate(34.1615729, -119.0435258),
            GeoCoordinate(34.1616977, -119.0435762),
            GeoCoordinate(34.1617255, -119.0436057),
            GeoCoordinate(34.1617410, -119.0436311),
            GeoCoordinate(34.1617516, -119.0436572),
            GeoCoordinate(34.1617560, -119.0436921),
            GeoCoordinate(34.1617533, -119.0437317),
            GeoCoordinate(34.1616042, -119.0443021),
            GeoCoordinate(34.1614783, -119.0447697),
            GeoCoordinate(34.1614476, -119.0448126),
        ),
        // OSM way 812573181 (footway)
        listOf(
            GeoCoordinate(34.1614148, -119.0416477),
            GeoCoordinate(34.1614517, -119.0416722),
            GeoCoordinate(34.1621213, -119.0419176),
            GeoCoordinate(34.1621960, -119.0419430),
            GeoCoordinate(34.1624146, -119.0420249),
            GeoCoordinate(34.1624257, -119.0420312),
            GeoCoordinate(34.1624376, -119.0420341),
            GeoCoordinate(34.1626203, -119.0421009),
            GeoCoordinate(34.1626362, -119.0421106),
            GeoCoordinate(34.1626549, -119.0421128),
            GeoCoordinate(34.1627324, -119.0421380),
            GeoCoordinate(34.1633732, -119.0423818),
            GeoCoordinate(34.1634307, -119.0423859),
            GeoCoordinate(34.1634606, -119.0423430),
            GeoCoordinate(34.1635029, -119.0421750),
            GeoCoordinate(34.1635993, -119.0418225),
            GeoCoordinate(34.1638638, -119.0408024),
            GeoCoordinate(34.1639148, -119.0406059),
            GeoCoordinate(34.1639161, -119.0405836),
            GeoCoordinate(34.1639173, -119.0405635),
        ),
        // OSM way 812573182 (footway)
        listOf(
            GeoCoordinate(34.1625277, -119.0424453),
            GeoCoordinate(34.1624696, -119.0424235),
        ),
        // OSM way 812573183 (footway)
        listOf(
            GeoCoordinate(34.1629545, -119.0428533),
            GeoCoordinate(34.1629242, -119.0428418),
            GeoCoordinate(34.1628856, -119.0428270),
        ),
        // OSM way 812573184 (footway)
        listOf(
            GeoCoordinate(34.1627023, -119.0432024),
            GeoCoordinate(34.1626942, -119.0431918),
        ),
        // OSM way 812573185 (footway)
        listOf(
            GeoCoordinate(34.1628856, -119.0428270),
            GeoCoordinate(34.1628886, -119.0428106),
            GeoCoordinate(34.1629513, -119.0425717),
        ),
        // OSM way 812573187 (footway)
        listOf(
            GeoCoordinate(34.1644787, -119.0429430),
            GeoCoordinate(34.1644363, -119.0429250),
            GeoCoordinate(34.1643794, -119.0429027),
            GeoCoordinate(34.1643983, -119.0428241),
            GeoCoordinate(34.1644181, -119.0427465),
            GeoCoordinate(34.1644894, -119.0427747),
            GeoCoordinate(34.1645675, -119.0428030),
        ),
        // OSM way 812630320 (service)
        listOf(
            GeoCoordinate(34.1615758, -119.0407888),
            GeoCoordinate(34.1616166, -119.0407997),
            GeoCoordinate(34.1616526, -119.0408101),
            GeoCoordinate(34.1616781, -119.0408218),
            GeoCoordinate(34.1616987, -119.0408433),
            GeoCoordinate(34.1617121, -119.0408719),
            GeoCoordinate(34.1617166, -119.0409043),
            GeoCoordinate(34.1617117, -119.0409367),
            GeoCoordinate(34.1616980, -119.0409651),
            GeoCoordinate(34.1616795, -119.0409846),
            GeoCoordinate(34.1616569, -119.0409962),
            GeoCoordinate(34.1616324, -119.0409988),
            GeoCoordinate(34.1616085, -119.0409922),
            GeoCoordinate(34.1615740, -119.0409760),
            GeoCoordinate(34.1615330, -119.0409571),
        ),
        // OSM way 812654706 (footway)
        listOf(
            GeoCoordinate(34.1629435, -119.0419737),
            GeoCoordinate(34.1629407, -119.0419417),
            GeoCoordinate(34.1628324, -119.0419043),
            GeoCoordinate(34.1628055, -119.0419124),
            GeoCoordinate(34.1624832, -119.0417949),
            GeoCoordinate(34.1621587, -119.0416681),
            GeoCoordinate(34.1621403, -119.0416445),
            GeoCoordinate(34.1620390, -119.0416047),
            GeoCoordinate(34.1620230, -119.0416264),
        ),
        // OSM way 812654707 (footway)
        listOf(
            GeoCoordinate(34.1623501, -119.0454464),
            GeoCoordinate(34.1623192, -119.0455709),
        ),
        // OSM way 812654708 (footway)
        listOf(
            GeoCoordinate(34.1634940, -119.0446387),
            GeoCoordinate(34.1634138, -119.0449219),
        ),
        // OSM way 817294738 (footway)
        listOf(
            GeoCoordinate(34.1604214, -119.0412890),
            GeoCoordinate(34.1604064, -119.0413374),
            GeoCoordinate(34.1603949, -119.0413822),
        ),
        // OSM way 817294739 (footway)
        listOf(
            GeoCoordinate(34.1604214, -119.0412890),
            GeoCoordinate(34.1605832, -119.0413484),
            GeoCoordinate(34.1611773, -119.0415676),
            GeoCoordinate(34.1612792, -119.0416055),
            GeoCoordinate(34.1613080, -119.0416055),
            GeoCoordinate(34.1613346, -119.0415613),
            GeoCoordinate(34.1614552, -119.0411043),
            GeoCoordinate(34.1614578, -119.0410946),
            GeoCoordinate(34.1614456, -119.0410315),
            GeoCoordinate(34.1615055, -119.0407941),
            GeoCoordinate(34.1615455, -119.0407566),
        ),
        // OSM way 817294740 (footway)
        listOf(
            GeoCoordinate(34.1613080, -119.0416055),
            GeoCoordinate(34.1613591, -119.0416259),
            GeoCoordinate(34.1614148, -119.0416477),
            GeoCoordinate(34.1613997, -119.0417112),
            GeoCoordinate(34.1613864, -119.0417594),
        ),
        // OSM way 817294741 (footway)
        listOf(
            GeoCoordinate(34.1621631, -119.0420761),
            GeoCoordinate(34.1621815, -119.0419988),
            GeoCoordinate(34.1621960, -119.0419430),
        ),
        // OSM way 817294742 (footway)
        listOf(
            GeoCoordinate(34.1624387, -119.0421784),
            GeoCoordinate(34.1623266, -119.0421355),
        ),
        // OSM way 817294743 (footway)
        listOf(
            GeoCoordinate(34.1623656, -119.0412038),
            GeoCoordinate(34.1622390, -119.0411564),
            GeoCoordinate(34.1622194, -119.0412320),
            GeoCoordinate(34.1621685, -119.0412646),
            GeoCoordinate(34.1621356, -119.0412568),
            GeoCoordinate(34.1619161, -119.0411762),
            GeoCoordinate(34.1618894, -119.0411645),
            GeoCoordinate(34.1618673, -119.0411484),
            GeoCoordinate(34.1616980, -119.0409651),
        ),
        // OSM way 817294744 (footway)
        listOf(
            GeoCoordinate(34.1628377, -119.0413823),
            GeoCoordinate(34.1629734, -119.0414279),
            GeoCoordinate(34.1629528, -119.0415113),
            GeoCoordinate(34.1629822, -119.0415740),
            GeoCoordinate(34.1632741, -119.0416852),
            GeoCoordinate(34.1634716, -119.0416596),
            GeoCoordinate(34.1635160, -119.0417303),
            GeoCoordinate(34.1635866, -119.0417845),
            GeoCoordinate(34.1635993, -119.0418225),
            GeoCoordinate(34.1636048, -119.0418313),
        ),
        // OSM way 817294745 (footway)
        listOf(
            GeoCoordinate(34.1626077, -119.0412964),
            GeoCoordinate(34.1626619, -119.0410829),
        ),
        // OSM way 850348156 (service)
        listOf(
            GeoCoordinate(34.1651298, -119.0431044),
            GeoCoordinate(34.1651084, -119.0431792),
            GeoCoordinate(34.1650004, -119.0435568),
        ),
        // OSM way 868550405 (footway)
        listOf(
            GeoCoordinate(34.1631983, -119.0445266),
            GeoCoordinate(34.1631719, -119.0445586),
            GeoCoordinate(34.1631166, -119.0447727),
            GeoCoordinate(34.1630901, -119.0447957),
            GeoCoordinate(34.1630362, -119.0450076),
        ),
        // OSM way 868550406 (footway)
        listOf(
            GeoCoordinate(34.1616475, -119.0450228),
            GeoCoordinate(34.1615086, -119.0455626),
            GeoCoordinate(34.1615145, -119.0456699),
            GeoCoordinate(34.1614649, -119.0457260),
            GeoCoordinate(34.1614434, -119.0458100),
            GeoCoordinate(34.1615854, -119.0458793),
            GeoCoordinate(34.1616484, -119.0458868),
        ),
        // OSM way 868550410 (footway)
        listOf(
            GeoCoordinate(34.1652450, -119.0448459),
            GeoCoordinate(34.1652933, -119.0448665),
            GeoCoordinate(34.1653463, -119.0448879),
        ),
        // OSM way 868550411 (footway)
        listOf(
            GeoCoordinate(34.1651852, -119.0450758),
            GeoCoordinate(34.1652334, -119.0450943),
            GeoCoordinate(34.1652908, -119.0451165),
        ),
        // OSM way 868550412 (footway)
        listOf(
            GeoCoordinate(34.1655017, -119.0450025),
            GeoCoordinate(34.1658653, -119.0451409),
        ),
        // OSM way 868550413 (footway)
        listOf(
            GeoCoordinate(34.1658303, -119.0452772),
            GeoCoordinate(34.1654679, -119.0451392),
        ),
        // OSM way 868550414 (footway)
        listOf(
            GeoCoordinate(34.1654679, -119.0451392),
            GeoCoordinate(34.1653713, -119.0451024),
            GeoCoordinate(34.1652908, -119.0451165),
        ),
        // OSM way 868550415 (footway)
        listOf(
            GeoCoordinate(34.1653463, -119.0448879),
            GeoCoordinate(34.1654085, -119.0449670),
            GeoCoordinate(34.1655017, -119.0450025),
        ),
        // OSM way 868550416 (footway)
        listOf(
            GeoCoordinate(34.1659283, -119.0453145),
            GeoCoordinate(34.1658303, -119.0452772),
        ),
        // OSM way 868550417 (footway)
        listOf(
            GeoCoordinate(34.1658653, -119.0451409),
            GeoCoordinate(34.1659631, -119.0451781),
        ),
        // OSM way 955219019 (service)
        listOf(
            GeoCoordinate(34.1616883, -119.0422753),
            GeoCoordinate(34.1615111, -119.0422107),
            GeoCoordinate(34.1614564, -119.0421908),
        ),
        // OSM way 955221399 (footway)
        listOf(
            GeoCoordinate(34.1626043, -119.0422443),
            GeoCoordinate(34.1625056, -119.0422048),
            GeoCoordinate(34.1624387, -119.0421784),
        ),
        // OSM way 955221400 (footway)
        listOf(
            GeoCoordinate(34.1622206, -119.0420959),
            GeoCoordinate(34.1621631, -119.0420761),
        ),
        // OSM way 955221401 (footway)
        listOf(
            GeoCoordinate(34.1623266, -119.0421355),
            GeoCoordinate(34.1622468, -119.0421059),
            GeoCoordinate(34.1622206, -119.0420959),
        ),
        // OSM way 955221403 (footway)
        listOf(
            GeoCoordinate(34.1621084, -119.0444844),
            GeoCoordinate(34.1620084, -119.0444496),
            GeoCoordinate(34.1620008, -119.0444471),
            GeoCoordinate(34.1619460, -119.0444294),
            GeoCoordinate(34.1618381, -119.0443883),
        ),
        // OSM way 955221404 (footway)
        listOf(
            GeoCoordinate(34.1618381, -119.0443883),
            GeoCoordinate(34.1618202, -119.0444023),
            GeoCoordinate(34.1618032, -119.0444008),
            GeoCoordinate(34.1617879, -119.0443884),
            GeoCoordinate(34.1617745, -119.0443645),
        ),
        // OSM way 955221405 (footway)
        listOf(
            GeoCoordinate(34.1618381, -119.0443883),
            GeoCoordinate(34.1618257, -119.0443633),
            GeoCoordinate(34.1618103, -119.0443532),
            GeoCoordinate(34.1617933, -119.0443541),
            GeoCoordinate(34.1617745, -119.0443645),
        ),
        // OSM way 955221406 (footway)
        listOf(
            GeoCoordinate(34.1615812, -119.0448653),
            GeoCoordinate(34.1616121, -119.0448981),
            GeoCoordinate(34.1616901, -119.0449259),
            GeoCoordinate(34.1617524, -119.0449176),
        ),
        // OSM way 955221407 (footway)
        listOf(
            GeoCoordinate(34.1617524, -119.0449176),
            GeoCoordinate(34.1618202, -119.0449453),
            GeoCoordinate(34.1618729, -119.0449647),
        ),
        // OSM way 955221409 (footway)
        listOf(
            GeoCoordinate(34.1652450, -119.0448459),
            GeoCoordinate(34.1651485, -119.0448100),
            GeoCoordinate(34.1649880, -119.0447550),
            GeoCoordinate(34.1649529, -119.0447787),
            GeoCoordinate(34.1649277, -119.0448354),
        ),
        // OSM way 955221410 (footway)
        listOf(
            GeoCoordinate(34.1652867, -119.0446850),
            GeoCoordinate(34.1649815, -119.0445681),
        ),
        // OSM way 1036899071 (service)
        listOf(
            GeoCoordinate(34.1590328, -119.0403970),
            GeoCoordinate(34.1590738, -119.0403137),
            GeoCoordinate(34.1590994, -119.0402051),
            GeoCoordinate(34.1591513, -119.0399854),
            GeoCoordinate(34.1592039, -119.0397631),
            GeoCoordinate(34.1592352, -119.0396306),
        ),
        // OSM way 1036899072 (service)
        listOf(
            GeoCoordinate(34.1590994, -119.0402051),
            GeoCoordinate(34.1595658, -119.0403868),
        ),
        // OSM way 1036899073 (service)
        listOf(
            GeoCoordinate(34.1595694, -119.0401396),
            GeoCoordinate(34.1591513, -119.0399854),
        ),
        // OSM way 1107864666 (footway)
        listOf(
            GeoCoordinate(34.1636424, -119.0418854),
            GeoCoordinate(34.1636530, -119.0418984),
            GeoCoordinate(34.1636854, -119.0419260),
            GeoCoordinate(34.1643612, -119.0421756),
        ),
        // OSM way 1107864667 (footway)
        listOf(
            GeoCoordinate(34.1636048, -119.0418313),
            GeoCoordinate(34.1636230, -119.0418582),
            GeoCoordinate(34.1636424, -119.0418854),
        ),
        // OSM way 1107873060 (footway)
        listOf(
            GeoCoordinate(34.1618896, -119.0397928),
            GeoCoordinate(34.1618602, -119.0398393),
            GeoCoordinate(34.1616166, -119.0407997),
            GeoCoordinate(34.1615740, -119.0409760),
            GeoCoordinate(34.1614536, -119.0414031),
            GeoCoordinate(34.1614040, -119.0416002),
        ),
        // OSM way 1107879080 (footway)
        listOf(
            GeoCoordinate(34.1641916, -119.0426872),
            GeoCoordinate(34.1642539, -119.0424513),
            GeoCoordinate(34.1642689, -119.0424570),
        ),
        // OSM way 1107879081 (footway)
        listOf(
            GeoCoordinate(34.1639754, -119.0426067),
            GeoCoordinate(34.1640424, -119.0423608),
        ),
        // OSM way 1107881653 (footway)
        listOf(
            GeoCoordinate(34.1640345, -119.0401151),
            GeoCoordinate(34.1640900, -119.0400994),
        ),
        // OSM way 1170524630 (footway)
        listOf(
            GeoCoordinate(34.1628498, -119.0403930),
            GeoCoordinate(34.1628544, -119.0403729),
        ),
        // OSM way 1170524631 (footway)
        listOf(
            GeoCoordinate(34.1626619, -119.0410829),
            GeoCoordinate(34.1628498, -119.0403930),
        ),
        // OSM way 1303211366 (footway)
        listOf(
            GeoCoordinate(34.1610336, -119.0450749),
            GeoCoordinate(34.1610236, -119.0451103),
        ),
        // OSM way 1303215242 (footway)
        listOf(
            GeoCoordinate(34.1583230, -119.0420002),
            GeoCoordinate(34.1583396, -119.0420069),
            GeoCoordinate(34.1583227, -119.0420768),
        ),
        // OSM way 1303329656 (footway)
        listOf(
            GeoCoordinate(34.1633012, -119.0407074),
            GeoCoordinate(34.1633828, -119.0403672),
            GeoCoordinate(34.1633857, -119.0403590),
            GeoCoordinate(34.1633914, -119.0403518),
            GeoCoordinate(34.1633985, -119.0403473),
            GeoCoordinate(34.1634050, -119.0403471),
            GeoCoordinate(34.1637666, -119.0404820),
            GeoCoordinate(34.1638832, -119.0405255),
            GeoCoordinate(34.1638962, -119.0405363),
            GeoCoordinate(34.1639063, -119.0405525),
            GeoCoordinate(34.1639126, -119.0405671),
            GeoCoordinate(34.1639161, -119.0405836),
        ),
        // OSM way 1303329657 (footway)
        listOf(
            GeoCoordinate(34.1638638, -119.0408024),
            GeoCoordinate(34.1637708, -119.0407659),
            GeoCoordinate(34.1637314, -119.0406150),
            GeoCoordinate(34.1637666, -119.0404820),
        ),
        // OSM way 1303329658 (footway)
        listOf(
            GeoCoordinate(34.1637708, -119.0407659),
            GeoCoordinate(34.1637614, -119.0408028),
        ),
        // OSM way 1303329659 (footway)
        listOf(
            GeoCoordinate(34.1639977, -119.0400912),
            GeoCoordinate(34.1639432, -119.0400697),
        ),
        // OSM way 1303423872 (footway)
        listOf(
            GeoCoordinate(34.1600893, -119.0444624),
            GeoCoordinate(34.1600533, -119.0444972),
            GeoCoordinate(34.1600003, -119.0447041),
        ),
        // OSM way 1303423873 (footway)
        listOf(
            GeoCoordinate(34.1600003, -119.0447041),
            GeoCoordinate(34.1599930, -119.0447327),
            GeoCoordinate(34.1599837, -119.0447692),
        ),
        // OSM way 1303423874 (footway)
        listOf(
            GeoCoordinate(34.1599837, -119.0447692),
            GeoCoordinate(34.1599751, -119.0448030),
        ),
        // OSM way 1303430657 (footway)
        listOf(
            GeoCoordinate(34.1610690, -119.0440084),
            GeoCoordinate(34.1610672, -119.0439738),
            GeoCoordinate(34.1610659, -119.0439505),
        ),
        // OSM way 1303430658 (footway)
        listOf(
            GeoCoordinate(34.1610659, -119.0439505),
            GeoCoordinate(34.1610650, -119.0439312),
        ),
        // OSM way 1303430659 (corridor)
        listOf(
            GeoCoordinate(34.1605485, -119.0438145),
            GeoCoordinate(34.1607750, -119.0438974),
            GeoCoordinate(34.1607831, -119.0438639),
            GeoCoordinate(34.1610672, -119.0439738),
        ),
        // OSM way 1303430660 (footway)
        listOf(
            GeoCoordinate(34.1605119, -119.0438006),
            GeoCoordinate(34.1605485, -119.0438145),
        ),
        // OSM way 1303432467 (corridor)
        listOf(
            GeoCoordinate(34.1608908, -119.0430863),
            GeoCoordinate(34.1610304, -119.0431341),
        ),
        // OSM way 1303432468 (footway)
        listOf(
            GeoCoordinate(34.1613642, -119.0432662),
            GeoCoordinate(34.1614289, -119.0432923),
        ),
        // OSM way 1303432469 (corridor)
        listOf(
            GeoCoordinate(34.1612223, -119.0432149),
            GeoCoordinate(34.1613642, -119.0432662),
        ),
        // OSM way 1303432470 (footway)
        listOf(
            GeoCoordinate(34.1610304, -119.0431341),
            GeoCoordinate(34.1612223, -119.0432149),
        ),
        // OSM way 1303432471 (footway)
        listOf(
            GeoCoordinate(34.1607106, -119.0430252),
            GeoCoordinate(34.1608096, -119.0430597),
            GeoCoordinate(34.1608908, -119.0430863),
        ),
        // OSM way 1303436568 (footway)
        listOf(
            GeoCoordinate(34.1649836, -119.0434859),
            GeoCoordinate(34.1649325, -119.0434551),
            GeoCoordinate(34.1648792, -119.0434323),
            GeoCoordinate(34.1648315, -119.0434135),
            GeoCoordinate(34.1647805, -119.0433961),
            GeoCoordinate(34.1647472, -119.0433867),
            GeoCoordinate(34.1647139, -119.0433625),
            GeoCoordinate(34.1646196, -119.0433263),
            GeoCoordinate(34.1646038, -119.0433689),
        ),
        // OSM way 1303436569 (footway)
        listOf(
            GeoCoordinate(34.1650666, -119.0431662),
            GeoCoordinate(34.1649836, -119.0434859),
        ),
        // OSM way 1303436570 (footway)
        listOf(
            GeoCoordinate(34.1642698, -119.0430660),
            GeoCoordinate(34.1642416, -119.0430575),
            GeoCoordinate(34.1639068, -119.0429322),
            GeoCoordinate(34.1638641, -119.0429164),
            GeoCoordinate(34.1638156, -119.0428995),
            GeoCoordinate(34.1637524, -119.0428737),
            GeoCoordinate(34.1633451, -119.0427207),
            GeoCoordinate(34.1629928, -119.0425861),
            GeoCoordinate(34.1629513, -119.0425717),
            GeoCoordinate(34.1625361, -119.0424164),
        ),
        // OSM way 1303436571 (footway)
        listOf(
            GeoCoordinate(34.1651504, -119.0431944),
            GeoCoordinate(34.1651084, -119.0431792),
            GeoCoordinate(34.1650666, -119.0431662),
        ),
        // OSM way 1303436572 (footway)
        listOf(
            GeoCoordinate(34.1647725, -119.0430298),
            GeoCoordinate(34.1647125, -119.0430250),
            GeoCoordinate(34.1646592, -119.0430250),
            GeoCoordinate(34.1645738, -119.0430371),
            GeoCoordinate(34.1644328, -119.0430713),
            GeoCoordinate(34.1643935, -119.0430767),
            GeoCoordinate(34.1643740, -119.0431049),
        ),
        // OSM way 1303436573 (footway)
        listOf(
            GeoCoordinate(34.1650666, -119.0431662),
            GeoCoordinate(34.1649566, -119.0431106),
            GeoCoordinate(34.1647725, -119.0430298),
            GeoCoordinate(34.1645210, -119.0429405),
            GeoCoordinate(34.1645012, -119.0429380),
            GeoCoordinate(34.1644787, -119.0429430),
            GeoCoordinate(34.1643816, -119.0430602),
            GeoCoordinate(34.1643740, -119.0431049),
        ),
        // OSM way 1303436574 (footway)
        listOf(
            GeoCoordinate(34.1643740, -119.0431049),
            GeoCoordinate(34.1643434, -119.0430933),
            GeoCoordinate(34.1642698, -119.0430660),
        ),
        // OSM way 1303553464 (footway)
        listOf(
            GeoCoordinate(34.1617745, -119.0443645),
            GeoCoordinate(34.1617012, -119.0443372),
        ),
        // OSM way 1303553465 (footway)
        listOf(
            GeoCoordinate(34.1616505, -119.0443186),
            GeoCoordinate(34.1616042, -119.0443021),
            GeoCoordinate(34.1613931, -119.0442253),
            GeoCoordinate(34.1613565, -119.0442951),
            GeoCoordinate(34.1613254, -119.0444241),
            GeoCoordinate(34.1613154, -119.0444654),
            GeoCoordinate(34.1611702, -119.0444515),
        ),
        // OSM way 1303553466 (footway)
        listOf(
            GeoCoordinate(34.1611545, -119.0445105),
            GeoCoordinate(34.1611702, -119.0444515),
        ),
        // OSM way 1303553467 (footway)
        listOf(
            GeoCoordinate(34.1606664, -119.0444556),
            GeoCoordinate(34.1606650, -119.0444620),
            GeoCoordinate(34.1606598, -119.0444812),
        ),
        // OSM way 1303553468 (steps)
        listOf(
            GeoCoordinate(34.1606457, -119.0445412),
            GeoCoordinate(34.1606598, -119.0444812),
        ),
        // OSM way 1303553469 (footway)
        listOf(
            GeoCoordinate(34.1606960, -119.0444954),
            GeoCoordinate(34.1607817, -119.0445273),
            GeoCoordinate(34.1607858, -119.0445095),
            GeoCoordinate(34.1606650, -119.0444620),
        ),
        // OSM way 1303553470 (footway)
        listOf(
            GeoCoordinate(34.1606816, -119.0445546),
            GeoCoordinate(34.1606960, -119.0444954),
        ),
        // OSM way 1303553471 (footway)
        listOf(
            GeoCoordinate(34.1610822, -119.0415916),
            GeoCoordinate(34.1610699, -119.0416396),
            GeoCoordinate(34.1610447, -119.0417416),
            GeoCoordinate(34.1609273, -119.0422155),
            GeoCoordinate(34.1609267, -119.0422182),
        ),
        // OSM way 1303553472 (footway)
        listOf(
            GeoCoordinate(34.1609267, -119.0422182),
            GeoCoordinate(34.1609088, -119.0422842),
        ),
        // OSM way 1303553473 (footway)
        listOf(
            GeoCoordinate(34.1607285, -119.0429592),
            GeoCoordinate(34.1606620, -119.0429336),
            GeoCoordinate(34.1607039, -119.0427741),
            GeoCoordinate(34.1607717, -119.0428002),
        ),
        // OSM way 1303553474 (footway)
        listOf(
            GeoCoordinate(34.1608128, -119.0431252),
            GeoCoordinate(34.1607970, -119.0431046),
            GeoCoordinate(34.1608096, -119.0430597),
            GeoCoordinate(34.1608192, -119.0430134),
            GeoCoordinate(34.1608403, -119.0430080),
        ),
        // OSM way 1303553475 (footway)
        listOf(
            GeoCoordinate(34.1608510, -119.0425084),
            GeoCoordinate(34.1608730, -119.0425200),
            GeoCoordinate(34.1608891, -119.0425347),
            GeoCoordinate(34.1609030, -119.0425589),
            GeoCoordinate(34.1609113, -119.0425817),
            GeoCoordinate(34.1609119, -119.0426018),
            GeoCoordinate(34.1609096, -119.0426165),
            GeoCoordinate(34.1608775, -119.0427379),
            GeoCoordinate(34.1608655, -119.0427338),
        ),
        // OSM way 1303553476 (footway)
        listOf(
            GeoCoordinate(34.1608655, -119.0427338),
            GeoCoordinate(34.1608542, -119.0427826),
        ),
        // OSM way 1303553477 (footway)
        listOf(
            GeoCoordinate(34.1608542, -119.0427826),
            GeoCoordinate(34.1609049, -119.0428013),
            GeoCoordinate(34.1609014, -119.0428148),
        ),
        // OSM way 1303553478 (service)
        listOf(
            GeoCoordinate(34.1603496, -119.0444288),
            GeoCoordinate(34.1603374, -119.0444804),
        ),
        // OSM way 1303553479 (footway)
        listOf(
            GeoCoordinate(34.1621051, -119.0438055),
            GeoCoordinate(34.1622083, -119.0438480),
        ),
        // OSM way 1303553480 (footway)
        listOf(
            GeoCoordinate(34.1614476, -119.0448126),
            GeoCoordinate(34.1615299, -119.0448449),
            GeoCoordinate(34.1615812, -119.0448653),
        ),
        // OSM way 1303553481 (footway)
        listOf(
            GeoCoordinate(34.1614585, -119.0423933),
            GeoCoordinate(34.1615111, -119.0422107),
        ),
        // OSM way 1303553482 (footway)
        listOf(
            GeoCoordinate(34.1614270, -119.0425036),
            GeoCoordinate(34.1614585, -119.0423933),
        ),
        // OSM way 1303553483 (footway)
        listOf(
            GeoCoordinate(34.1611418, -119.0427327),
            GeoCoordinate(34.1611154, -119.0427238),
        ),
        // OSM way 1303557656 (footway)
        listOf(
            GeoCoordinate(34.1622870, -119.0441509),
            GeoCoordinate(34.1622379, -119.0443425),
        ),
        // OSM way 1303557657 (footway)
        listOf(
            GeoCoordinate(34.1613254, -119.0444241),
            GeoCoordinate(34.1613653, -119.0444415),
        ),
        // OSM way 1303557658 (footway)
        listOf(
            GeoCoordinate(34.1584099, -119.0423028),
            GeoCoordinate(34.1583694, -119.0422875),
            GeoCoordinate(34.1582800, -119.0422537),
        ),
        // OSM way 1303561586 (service)
        listOf(
            GeoCoordinate(34.1616987, -119.0408433),
            GeoCoordinate(34.1617409, -119.0407639),
            GeoCoordinate(34.1617698, -119.0406533),
            GeoCoordinate(34.1619873, -119.0407385),
        ),
        // OSM way 1348774426 (footway)
        listOf(
            GeoCoordinate(34.1659030, -119.0466894),
            GeoCoordinate(34.1659404, -119.0467001),
            GeoCoordinate(34.1659876, -119.0467158),
        ),
        // OSM way 1348774427 (footway)
        listOf(
            GeoCoordinate(34.1659876, -119.0467158),
            GeoCoordinate(34.1665349, -119.0469112),
        ),
        // OSM way 1348774442 (footway)
        listOf(
            GeoCoordinate(34.1649892, -119.0463538),
            GeoCoordinate(34.1649584, -119.0463443),
            GeoCoordinate(34.1649333, -119.0463721),
            GeoCoordinate(34.1649103, -119.0463888),
            GeoCoordinate(34.1648682, -119.0464000),
            GeoCoordinate(34.1648168, -119.0463848),
            GeoCoordinate(34.1647969, -119.0463829),
            GeoCoordinate(34.1645575, -119.0462904),
            GeoCoordinate(34.1644516, -119.0462508),
            GeoCoordinate(34.1640660, -119.0461095),
        ),
        // OSM way 1348774443 (footway)
        listOf(
            GeoCoordinate(34.1640660, -119.0461095),
            GeoCoordinate(34.1640236, -119.0460940),
            GeoCoordinate(34.1639707, -119.0460743),
        ),
        // OSM way 1348774444 (footway)
        listOf(
            GeoCoordinate(34.1609194, -119.0447576),
            GeoCoordinate(34.1608993, -119.0447504),
            GeoCoordinate(34.1608606, -119.0447347),
        ),
        // OSM way 1348774445 (footway)
        listOf(
            GeoCoordinate(34.1608606, -119.0447347),
            GeoCoordinate(34.1606207, -119.0446375),
            GeoCoordinate(34.1601236, -119.0444554),
            GeoCoordinate(34.1600893, -119.0444624),
        ),
        // OSM way 1361026267 (footway)
        listOf(
            GeoCoordinate(34.1624396, -119.0446072),
            GeoCoordinate(34.1621936, -119.0445160),
            GeoCoordinate(34.1621084, -119.0444844),
        ),
        // OSM way 1361026268 (footway)
        listOf(
            GeoCoordinate(34.1625310, -119.0446417),
            GeoCoordinate(34.1624396, -119.0446072),
        ),
        // OSM way 1361026269 (footway)
        listOf(
            GeoCoordinate(34.1622379, -119.0443425),
            GeoCoordinate(34.1621936, -119.0445160),
        ),
        // OSM way 1361026270 (footway)
        listOf(
            GeoCoordinate(34.1626118, -119.0451462),
            GeoCoordinate(34.1624156, -119.0450690),
            GeoCoordinate(34.1623819, -119.0450564),
        ),
        // OSM way 1361026271 (footway)
        listOf(
            GeoCoordinate(34.1626123, -119.0454556),
            GeoCoordinate(34.1626261, -119.0453995),
        ),
        // OSM way 1429503704 (footway)
        listOf(
            GeoCoordinate(34.1617012, -119.0443372),
            GeoCoordinate(34.1616637, -119.0443233),
        ),
        // OSM way 1429503714 (footway)
        listOf(
            GeoCoordinate(34.1616637, -119.0443233),
            GeoCoordinate(34.1616505, -119.0443186),
        ),
        // OSM way 1429503737 (footway)
        listOf(
            GeoCoordinate(34.1583227, -119.0420768),
            GeoCoordinate(34.1582800, -119.0422537),
            GeoCoordinate(34.1582586, -119.0423422),
        ),
        // OSM way 1445904800 (service)
        listOf(
            GeoCoordinate(34.1637962, -119.0429711),
            GeoCoordinate(34.1637159, -119.0432675),
            GeoCoordinate(34.1636033, -119.0432294),
        ),
        // OSM way 1445904801 (service)
        listOf(
            GeoCoordinate(34.1636033, -119.0432294),
            GeoCoordinate(34.1635247, -119.0432178),
            GeoCoordinate(34.1633540, -119.0431537),
        ),
        // OSM way 1445904802 (service)
        listOf(
            GeoCoordinate(34.1650004, -119.0435568),
            GeoCoordinate(34.1649628, -119.0436883),
        ),
    )

private val walkingGraph: Map<GeoCoordinate, List<GraphEdge>> by lazy {
    val graph = mutableMapOf<GeoCoordinate, MutableList<GraphEdge>>()

    fun addEdge(
        first: GeoCoordinate,
        second: GeoCoordinate,
    ) {
        val edgeDistance = distanceMeters(first, second)
        graph.getOrPut(first) { mutableListOf() } += GraphEdge(second, edgeDistance)
        graph.getOrPut(second) { mutableListOf() } += GraphEdge(first, edgeDistance)
    }

    osmWalkingWays.forEach { way ->
        way.zipWithNext(::addEdge)
    }

    graph
}

fun campusLocations(): List<CampusLocation> = campusLocations.sortedBy { it.name }

fun walkingRoute(
    start: CampusLocation,
    end: CampusLocation,
): List<GeoCoordinate> {
    if (start.id == end.id) {
        return listOf(start.coordinate)
    }

    val route = bestOsmRoute(start.coordinate, end.coordinate)
        ?: return listOf(start.coordinate, end.coordinate)

    return route.points.dedupeAdjacent()
}

private fun bestOsmRoute(
    start: GeoCoordinate,
    end: GeoCoordinate,
): RouteCandidate? {
    val startCandidates = nearestWalkNodes(start)
    val endCandidates = nearestWalkNodes(end)
    val endCandidateSet = endCandidates.toSet()

    return startCandidates
        .flatMap { startNode ->
            shortestPaths(startNode, endCandidateSet).map { (endNode, route) ->
                val accessDistanceMeters = distanceMeters(start, startNode) + distanceMeters(endNode, end)
                RouteCandidate(
                    points = route.points,
                    totalDistanceMeters =
                        route.totalDistanceMeters + accessDistanceMeters * 10,
                )
            }
        }
        .minByOrNull { it.totalDistanceMeters }
}

private fun nearestWalkNodes(
    coordinate: GeoCoordinate,
    limit: Int = 12,
): List<GeoCoordinate> =
    walkingGraph.keys
        .asSequence()
        .map { node -> node to distanceMeters(coordinate, node) }
        .sortedBy { it.second }
        .take(limit)
        .map { it.first }
        .toList()

private fun shortestPaths(
    start: GeoCoordinate,
    targets: Set<GeoCoordinate>,
): List<Pair<GeoCoordinate, RouteCandidate>> {
    if (targets.isEmpty()) return emptyList()

    val distances = mutableMapOf(start to 0.0)
    val previous = mutableMapOf<GeoCoordinate, GeoCoordinate?>()
    val queue = PriorityQueue<QueueEntry>(compareBy { it.distanceMeters })
    val remainingTargets = targets.toMutableSet()

    previous[start] = null
    queue += QueueEntry(start, 0.0)

    while (queue.isNotEmpty() && remainingTargets.isNotEmpty()) {
        val current = queue.poll() ?: break
        if (current.distanceMeters != distances[current.node]) continue

        remainingTargets.remove(current.node)

        walkingGraph[current.node].orEmpty().forEach { edge ->
            val candidateDistance = current.distanceMeters + edge.distanceMeters
            if (candidateDistance < (distances[edge.destination] ?: Double.POSITIVE_INFINITY)) {
                distances[edge.destination] = candidateDistance
                previous[edge.destination] = current.node
                queue += QueueEntry(edge.destination, candidateDistance)
            }
        }
    }

    return targets.mapNotNull { target ->
        val distance = distances[target] ?: return@mapNotNull null
        target to RouteCandidate(
            points = reconstructPath(target, previous),
            totalDistanceMeters = distance,
        )
    }
}

private fun reconstructPath(
    end: GeoCoordinate,
    previous: Map<GeoCoordinate, GeoCoordinate?>,
): List<GeoCoordinate> {
    val path = mutableListOf<GeoCoordinate>()
    var current: GeoCoordinate? = end

    while (current != null) {
        path += current
        current = previous[current]
    }

    return path.asReversed()
}

fun routeDistanceMeters(points: List<GeoCoordinate>): Int {
    if (points.size < 2) return 0
    return points.zipWithNext { first, second -> distanceMeters(first, second) }
        .sum()
        .toInt()
}

private fun List<GeoCoordinate>.dedupeAdjacent(): List<GeoCoordinate> =
    fold(mutableListOf()) { acc, point ->
        if (acc.lastOrNull() != point) {
            acc += point
        }
        acc
    }

private fun distanceMeters(
    first: GeoCoordinate,
    second: GeoCoordinate,
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

