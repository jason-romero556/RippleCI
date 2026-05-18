package com.example.rippleci.data.models

data class UserGroupProfile(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerUserId: String = "",
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val visibility: String = "public",
    val bulletinVisibility: String = "public",
    val membersCanInvite: Boolean = false,
    val membersCanCreateEvents: Boolean = false,
    val membersCanPostBulletin: Boolean = false,
    val adminsCanManageInvites: Boolean = false,
    val eventDefaultVisibility: String = "members",
    val pastEventsVisibility: String = GROUP_PAST_EVENTS_ADMINS,
    val adminPermissions: UserGroupAdminPermissions = UserGroupAdminPermissions(),
    val profilePictureUrl: String = "",
)
