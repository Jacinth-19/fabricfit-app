package com.fabricfit.app.data.repository

import com.fabricfit.app.domain.model.CommunityGroup
import com.fabricfit.app.domain.model.GroupCategory
import com.fabricfit.app.domain.model.GroupEvent
import com.fabricfit.app.domain.model.GroupMembership
import com.fabricfit.app.domain.model.GroupPost
import com.fabricfit.app.domain.model.GroupRole
import com.fabricfit.app.domain.model.JoinRequest
import com.fabricfit.app.domain.model.MembershipStatus
import com.fabricfit.app.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Repository for managing community groups.
 */
class GroupRepository {
    
    // In-memory cache (replace with actual database/API in production)
    private val groups = mutableMapOf<String, CommunityGroup>()
    private val memberships = mutableMapOf<String, MutableList<GroupMembership>>()
    private val posts = mutableMapOf<String, MutableList<GroupPost>>()
    private val events = mutableMapOf<String, MutableList<GroupEvent>>()
    private val joinRequests = mutableMapOf<String, MutableList<JoinRequest>>()
    
    init {
        // Initialize with mock data
        initializeMockData()
    }
    
    /**
     * Get all available groups.
     */
    fun observeGroups(): Flow<List<CommunityGroup>> = flow {
        emit(groups.values.toList())
    }
    
    /**
     * Get groups by category.
     */
    suspend fun getGroupsByCategory(category: GroupCategory): List<CommunityGroup> {
        return groups.values.filter { it.category == category }
    }
    
    /**
     * Search groups by name or description.
     */
    suspend fun searchGroups(query: String): List<CommunityGroup> {
        return groups.values.filter { group ->
            group.name.contains(query, ignoreCase = true) ||
            group.description.contains(query, ignoreCase = true) ||
            group.tags.any { it.contains(query, ignoreCase = true) }
        }
    }
    
    /**
     * Get user's joined groups.
     */
    suspend fun getUserGroups(userId: String): List<CommunityGroup> {
        val userMemberships = memberships.values.flatten().filter { 
            it.userId == userId && it.status == MembershipStatus.ACTIVE 
        }
        return userMemberships.mapNotNull { groups[it.groupId] }
    }
    
    /**
     * Get group details.
     */
    suspend fun getGroup(groupId: String): CommunityGroup? {
        return groups[groupId]
    }
    
    /**
     * Get group posts.
     */
    fun observeGroupPosts(groupId: String): Flow<List<GroupPost>> = flow {
        emit(posts[groupId] ?: emptyList())
    }
    
    /**
     * Get group events.
     */
    suspend fun getGroupEvents(groupId: String): List<GroupEvent> {
        return events[groupId] ?: emptyList()
    }
    
    /**
     * Join a public group.
     */
    suspend fun joinGroup(groupId: String, userId: String): Boolean {
        val group = groups[groupId] ?: return false
        
        if (group.requiresApproval) {
            // Create join request
            val request = JoinRequest(
                id = "req_${System.currentTimeMillis()}",
                groupId = groupId,
                userId = userId,
                userName = "User $userId",
                userAvatarUrl = null,
                message = null,
                requestedAt = System.currentTimeMillis(),
                status = RequestStatus.PENDING,
            )
            joinRequests.getOrPut(groupId) { mutableListOf() }.add(request)
            return true
        } else {
            // Direct join
            val membership = GroupMembership(
                groupId = groupId,
                userId = userId,
                role = GroupRole.MEMBER,
                joinedAt = System.currentTimeMillis(),
                status = MembershipStatus.ACTIVE,
            )
            memberships.getOrPut(groupId) { mutableListOf() }.add(membership)
            
            // Update member count
            groups[groupId] = group.copy(memberCount = group.memberCount + 1)
            return true
        }
    }
    
    /**
     * Leave a group.
     */
    suspend fun leaveGroup(groupId: String, userId: String) {
        memberships[groupId]?.removeAll { it.userId == userId }
        groups[groupId]?.let { group ->
            groups[groupId] = group.copy(memberCount = (group.memberCount - 1).coerceAtLeast(0))
        }
    }
    
    /**
     * Create a new group.
     */
    suspend fun createGroup(
        name: String,
        description: String,
        category: GroupCategory,
        isPrivate: Boolean,
        requiresApproval: Boolean,
        userId: String,
    ): CommunityGroup {
        val group = CommunityGroup(
            id = "group_${System.currentTimeMillis()}",
            name = name,
            description = description,
            category = category,
            coverImageUrl = null,
            memberCount = 1,
            postCount = 0,
            isPrivate = isPrivate,
            requiresApproval = requiresApproval,
            createdAt = System.currentTimeMillis(),
            createdBy = userId,
        )
        groups[group.id] = group
        
        // Add creator as owner
        val membership = GroupMembership(
            groupId = group.id,
            userId = userId,
            role = GroupRole.OWNER,
            joinedAt = System.currentTimeMillis(),
            status = MembershipStatus.ACTIVE,
        )
        memberships[group.id] = mutableListOf(membership)
        
        return group
    }
    
    /**
     * Create a post in a group.
     */
    suspend fun createPost(
        groupId: String,
        userId: String,
        content: String,
        imageUrls: List<String> = emptyList(),
    ): GroupPost {
        val post = GroupPost(
            id = "post_${System.currentTimeMillis()}",
            groupId = groupId,
            userId = userId,
            userName = "User $userId",
            userAvatarUrl = null,
            content = content,
            imageUrls = imageUrls,
            likeCount = 0,
            commentCount = 0,
            isLiked = false,
            isPinned = false,
            createdAt = System.currentTimeMillis(),
        )
        posts.getOrPut(groupId) { mutableListOf() }.add(0, post)
        
        // Update post count
        groups[groupId]?.let { group ->
            groups[groupId] = group.copy(postCount = group.postCount + 1)
        }
        
        return post
    }
    
    /**
     * Create an event in a group.
     */
    suspend fun createEvent(
        groupId: String,
        title: String,
        description: String,
        startTime: Long,
        endTime: Long,
        location: String?,
        userId: String,
    ): GroupEvent {
        val event = GroupEvent(
            id = "event_${System.currentTimeMillis()}",
            groupId = groupId,
            title = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            attendeeCount = 0,
            createdBy = userId,
            imageUrl = null,
        )
        events.getOrPut(groupId) { mutableListOf() }.add(event)
        return event
    }
    
    /**
     * Get user's role in a group.
     */
    suspend fun getUserRole(groupId: String, userId: String): GroupRole? {
        return memberships[groupId]?.find { it.userId == userId }?.role
    }
    
    /**
     * Get pending join requests (for admins).
     */
    suspend fun getPendingJoinRequests(groupId: String): List<JoinRequest> {
        return joinRequests[groupId]?.filter { it.status == RequestStatus.PENDING } ?: emptyList()
    }
    
    /**
     * Approve join request.
     */
    suspend fun approveJoinRequest(requestId: String, groupId: String) {
        val request = joinRequests[groupId]?.find { it.id == requestId } ?: return
        
        // Update request status
        request.copy(status = RequestStatus.APPROVED)
        
        // Add membership
        val membership = GroupMembership(
            groupId = groupId,
            userId = request.userId,
            role = GroupRole.MEMBER,
            joinedAt = System.currentTimeMillis(),
            status = MembershipStatus.ACTIVE,
        )
        memberships.getOrPut(groupId) { mutableListOf() }.add(membership)
        
        // Update member count
        groups[groupId]?.let { group ->
            groups[groupId] = group.copy(memberCount = group.memberCount + 1)
        }
    }
    
    /**
     * Initialize mock data for demo.
     */
    private fun initializeMockData() {
        val mockGroups = listOf(
            CommunityGroup(
                id = "group_1",
                name = "Vintage Fashion Lovers",
                description = "A community for vintage fashion enthusiasts",
                category = GroupCategory.VINTAGE,
                coverImageUrl = null,
                memberCount = 1250,
                postCount = 342,
                isPrivate = false,
                requiresApproval = false,
                createdAt = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                createdBy = "user_1",
                tags = listOf("vintage", "retro", "classic"),
            ),
            CommunityGroup(
                id = "group_2",
                name = "Sustainable Fashion",
                description = "Eco-friendly and sustainable fashion discussion",
                category = GroupCategory.SUSTAINABLE,
                coverImageUrl = null,
                memberCount = 890,
                postCount = 156,
                isPrivate = false,
                requiresApproval = true,
                createdAt = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000,
                createdBy = "user_2",
                tags = listOf("sustainable", "eco", "ethical"),
            ),
            CommunityGroup(
                id = "group_3",
                name = "Street Style Collective",
                description = "Urban and street fashion inspiration",
                category = GroupCategory.STREETWEAR,
                coverImageUrl = null,
                memberCount = 2100,
                postCount = 567,
                isPrivate = false,
                requiresApproval = false,
                createdAt = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000,
                createdBy = "user_3",
                tags = listOf("streetwear", "urban", "hiphop"),
            ),
        )
        
        mockGroups.forEach { groups[it.id] = it }
    }
}
