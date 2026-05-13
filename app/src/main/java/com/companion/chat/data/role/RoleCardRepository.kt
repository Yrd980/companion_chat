package com.companion.chat.data.role

import com.companion.chat.data.local.dao.RoleCardDao
import com.companion.chat.data.local.entity.RoleCard

class RoleCardRepository(
    private val roleCardDao: RoleCardDao,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun getAllRoleCards(): List<RoleCard> = roleCardDao.getAll()

    suspend fun getActiveRoleCard(): RoleCard? = roleCardDao.getActive()

    suspend fun createRoleCard(
        name: String,
        description: String,
        avatar: String,
        persona: String,
        speakingStyle: String,
        background: String,
        rules: String,
        taboos: String,
        openingMessage: String,
        exampleDialogue: String
    ): Long {
        val normalizedName = name.trim()
        val normalizedPersona = persona.trim()
        require(normalizedName.isNotBlank()) { "角色名称不能为空" }
        require(normalizedPersona.isNotBlank()) { "核心人设不能为空" }

        val now = nowProvider()
        return roleCardDao.insert(
            RoleCard(
                name = normalizedName,
                description = description.trim(),
                avatar = avatar.trim().ifBlank { "person" },
                persona = normalizedPersona,
                speakingStyle = speakingStyle.trim(),
                background = background.trim(),
                rules = rules.trim(),
                taboos = taboos.trim(),
                openingMessage = openingMessage.trim(),
                exampleDialogue = exampleDialogue.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateRoleCard(
        id: Long,
        name: String,
        description: String,
        avatar: String,
        persona: String,
        speakingStyle: String,
        background: String,
        rules: String,
        taboos: String,
        openingMessage: String,
        exampleDialogue: String
    ) {
        val existing = roleCardDao.getById(id) ?: error("未找到角色卡: $id")
        val normalizedName = name.trim()
        val normalizedPersona = persona.trim()
        require(normalizedName.isNotBlank()) { "角色名称不能为空" }
        require(normalizedPersona.isNotBlank()) { "核心人设不能为空" }

        roleCardDao.update(
            existing.copy(
                name = normalizedName,
                description = description.trim(),
                avatar = avatar.trim().ifBlank { existing.avatar },
                persona = normalizedPersona,
                speakingStyle = speakingStyle.trim(),
                background = background.trim(),
                rules = rules.trim(),
                taboos = taboos.trim(),
                openingMessage = openingMessage.trim(),
                exampleDialogue = exampleDialogue.trim(),
                updatedAt = nowProvider()
            )
        )
    }

    suspend fun deleteRoleCard(id: Long) {
        val existing = roleCardDao.getById(id) ?: return
        check(!existing.isBuiltIn) { "内置角色卡不可删除" }
        roleCardDao.delete(existing)
    }

    suspend fun activateRoleCard(id: Long) {
        roleCardDao.deactivateAll()
        roleCardDao.activate(id, nowProvider())
    }
}
