package com.example.rippleci.data.models

data class UserGroupAdminPermissions(
    val canKickMembers: Boolean = false,
    val canPromoteAdmins: Boolean = false,
    val canCreateEvents: Boolean = false,
    val canSetEventVisibility: Boolean = false,
    val canEditGroupProfile: Boolean = false,
    val canManageInviteSettings: Boolean = false,
    val canManagePermissionMenu: Boolean = false,
    val canPostToBulletin: Boolean = false,
    val canManageBulletinPosts: Boolean = false,
)

const val GROUP_PAST_EVENTS_PUBLIC = "public"
const val GROUP_PAST_EVENTS_MEMBERS = "members"
const val GROUP_PAST_EVENTS_ADMINS = "admins"
const val GROUP_PAST_EVENTS_OWNER = "owner"
