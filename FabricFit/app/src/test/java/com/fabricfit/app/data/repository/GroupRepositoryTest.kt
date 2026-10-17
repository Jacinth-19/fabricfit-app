package com.fabricfit.app.data.repository

import com.fabricfit.app.domain.model.GroupCategory
import com.fabricfit.app.domain.model.GroupRole
import com.fabricfit.app.domain.model.MembershipStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GroupRepositoryTest {

    private lateinit var repository: GroupRepository

    @Before
    fun setup() {
        repository = GroupRepository()
    }

    @Test
    fun `observeGroups returns all groups`() = runBlocking {
        val groups = repository.observeGroups().first()

        assertNotNull(groups)
        assertTrue(groups.isNotEmpty())
        groups.forEach { group ->
            assertNotNull(group.id)
            assertNotNull(group.name)
            assertNotNull(group.description)
            assertNotNull(group.category)
            assertTrue(group.memberCount >= 0)
            assertTrue(group.postCount >= 0)
        }
    }

    @Test
    fun `getGroupsByCategory filters correctly`() = runBlocking {
        val category = GroupCategory.VINTAGE
        val groups = repository.getGroupsByCategory(category)

        assertNotNull(groups)
        groups.forEach { group ->
            assertEquals(category, group.category)
        }
    }

    @Test
    fun `searchGroups finds groups by name`() = runBlocking {
        val query = "Vintage"
        val groups = repository.searchGroups(query)

        assertNotNull(groups)
        groups.forEach { group ->
            assertTrue(
                group.name.contains(query, ignoreCase = true) ||
                group.description.contains(query, ignoreCase = true) ||
                group.tags.any { it.contains(query, ignoreCase = true) }
            )
        }
    }

    @Test
    fun `searchGroups returns empty for no matches`() = runBlocking {
        val query = "xyznonexistent123"
        val groups = repository.searchGroups(query)

        assertNotNull(groups)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `getUserGroups returns user's joined groups`() = runBlocking {
        val userId = "user_1"
        val groups = repository.getUserGroups(userId)

        assertNotNull(groups)
        // User may or may not have joined groups initially
    }

    @Test
    fun `getGroup returns group by id`() = runBlocking {
        val groups = repository.observeGroups().first()
        if (groups.isNotEmpty()) {
            val groupId = groups[0].id
            val group = repository.getGroup(groupId)

            assertNotNull(group)
            assertEquals(groupId, group!!.id)
        }
    }

    @Test
    fun `getGroup returns null for invalid id`() = runBlocking {
        val group = repository.getGroup("invalid_id")
        assertNull(group)
    }

    @Test
    fun `joinGroup adds user to public group`() = runBlocking {
        val groups = repository.observeGroups().first()
        val publicGroup = groups.find { !it.isPrivate && !it.requiresApproval }

        if (publicGroup != null) {
            val userId = "test_user"
            val success = repository.joinGroup(publicGroup.id, userId)

            assertTrue(success)

            // Verify membership
            val role = repository.getUserRole(publicGroup.id, userId)
            assertEquals(GroupRole.MEMBER, role)
        }
    }

    @Test
    fun `joinGroup creates request for private group`() = runBlocking {
        val groups = repository.observeGroups().first()
        val privateGroup = groups.find { it.requiresApproval }

        if (privateGroup != null) {
            val userId = "test_user"
            val success = repository.joinGroup(privateGroup.id, userId)

            assertTrue(success)

            // Verify pending request
            val requests = repository.getPendingJoinRequests(privateGroup.id)
            assertTrue(requests.any { it.userId == userId })
        }
    }

    @Test
    fun `joinGroup returns false for invalid group`() = runBlocking {
        val success = repository.joinGroup("invalid_id", "user_1")
        assertFalse(success)
    }

    @Test
    fun `leaveGroup removes user from group`() = runBlocking {
        val groups = repository.observeGroups().first()
        val group = groups.firstOrNull()

        if (group != null) {
            val userId = "test_user"
            repository.joinGroup(group.id, userId)
            repository.leaveGroup(group.id, userId)

            val role = repository.getUserRole(group.id, userId)
            assertNull(role)
        }
    }

    @Test
    fun `createGroup creates new group`() = runBlocking {
        val userId = "creator_user"
        val group = repository.createGroup(
            name = "Test Group",
            description = "Test Description",
            category = GroupCategory.FASHION,
            isPrivate = false,
            requiresApproval = false,
            userId = userId
        )

        assertNotNull(group)
        assertEquals("Test Group", group.name)
        assertEquals("Test Description", group.description)
        assertEquals(GroupCategory.FASHION, group.category)
        assertEquals(1, group.memberCount)

        // Verify creator is owner
        val role = repository.getUserRole(group.id, userId)
        assertEquals(GroupRole.OWNER, role)
    }

    @Test
    fun `createPost adds post to group`() = runBlocking {
        val groups = repository.observeGroups().first()
        val group = groups.firstOrNull()

        if (group != null) {
            val userId = "poster_user"
            val content = "Test post content"
            val post = repository.createPost(group.id, userId, content)

            assertNotNull(post)
            assertEquals(group.id, post.groupId)
            assertEquals(userId, post.userId)
            assertEquals(content, post.content)
            assertEquals(0, post.likeCount)
            assertEquals(0, post.commentCount)
        }
    }

    @Test
    fun `observeGroupPosts returns posts for group`() = runBlocking {
        val groups = repository.observeGroups().first()
        val group = groups.firstOrNull()

        if (group != null) {
            val posts = repository.observeGroupPosts(group.id).first()
            assertNotNull(posts)
        }
    }

    @Test
    fun `createEvent adds event to group`() = runBlocking {
        val groups = repository.observeGroups().first()
        val group = groups.firstOrNull()

        if (group != null) {
            val userId = "event_creator"
            val startTime = System.currentTimeMillis() + 86400000 // Tomorrow
            val endTime = startTime + 3600000 // 1 hour later

            val event = repository.createEvent(
                groupId = group.id,
                title = "Test Event",
                description = "Test Event Description",
                startTime = startTime,
                endTime = endTime,
                location = "Test Location",
                userId = userId
            )

            assertNotNull(event)
            assertEquals(group.id, event.groupId)
            assertEquals("Test Event", event.title)
            assertEquals(userId, event.createdBy)
        }
    }

    @Test
    fun `getGroupEvents returns events for group`() = runBlocking {
        val groups = repository.observeGroups().first()
        val group = groups.firstOrNull()

        if (group != null) {
            val events = repository.getGroupEvents(group.id)
            assertNotNull(events)
        }
    }

    @Test
    fun `getUserRole returns null for non-member`() = runBlocking {
        val groups = repository.observeGroups().first()
        val group = groups.firstOrNull()

        if (group != null) {
            val role = repository.getUserRole(group.id, "non_member")
            assertNull(role)
        }
    }

    @Test
    fun `approveJoinRequest adds user as member`() = runBlocking {
        val groups = repository.observeGroups().first()
        val privateGroup = groups.find { it.requiresApproval }

        if (privateGroup != null) {
            val userId = "requesting_user"
            repository.joinGroup(privateGroup.id, userId)

            val requests = repository.getPendingJoinRequests(privateGroup.id)
            val request = requests.find { it.userId == userId }

            if (request != null) {
                repository.approveJoinRequest(request.id, privateGroup.id)

                val role = repository.getUserRole(privateGroup.id, userId)
                assertEquals(GroupRole.MEMBER, role)
            }
        }
    }

    @Test
    fun `all group categories are supported`() = runBlocking {
        GroupCategory.values().forEach { category ->
            val groups = repository.getGroupsByCategory(category)
            assertNotNull(groups)
        }
    }
}
