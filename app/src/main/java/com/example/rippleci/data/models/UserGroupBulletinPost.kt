package com.example.rippleci.data.models

data class UserGroupBulletinPost(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val authorUserId: String = "",
    val authorName: String = "",
    val authorProfilePictureUrl: String = "",
    val imageUrl: String = "",
    val linkedEventId: String = "",
    val linkedEventOwnerUserId: String = "",
    val linkedEventGroupId: String = "",
    val linkedEventTitle: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
