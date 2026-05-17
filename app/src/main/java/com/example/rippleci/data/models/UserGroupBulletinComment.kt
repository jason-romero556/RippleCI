package com.example.rippleci.data.models

data class UserGroupBulletinComment(
    val id: String = "",
    val authorUserId: String = "",
    val authorName: String = "",
    val authorProfilePictureUrl: String = "",
    val body: String = "",
    val createdAt: Long = 0L,
)
