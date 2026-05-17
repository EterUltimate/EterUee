package com.eterultimate.eteruee.roleplay.domain.service

import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.data.model.GroupMember
import kotlin.random.Random

/**
 * 群组聊天发言人管理服务
 * 负责在群组聊天中智能选择下一个发言的角色
 */
interface GroupSpeakerService {
    
    /**
     * 根据策略选择下一个发言人
     * 
     * @param group 群组信息
     * @param strategy 选择策略
     * @return 选中的发言人，如果没有合适的发言人则返回 null
     */
    fun selectNextSpeaker(
        group: Group,
        strategy: SpeakerSelectionStrategy = SpeakerSelectionStrategy.PROBABILITY_BASED
    ): GroupMember?
    
    /**
     * 批量选择多个发言人（用于同时发言场景）
     * 
     * @param group 群组信息
     * @param count 需要选择的发言人数
     * @return 选中的发言人列表
     */
    fun selectMultipleSpeakers(group: Group, count: Int): List<GroupMember>
    
    /**
     * 计算每个成员的发言权重
     * 
     * @param group 群组信息
     * @return 成员ID到权重的映射
     */
    fun calculateSpeakerWeights(group: Group): Map<kotlin.uuid.Uuid, Float>
}

/**
 * 发言人选择策略
 */
enum class SpeakerSelectionStrategy {
    /**
     * 基于概率和优先级（默认）
     */
    PROBABILITY_BASED,
    
    /**
     * 轮询模式（按顺序轮流发言）
     */
    ROUND_ROBIN,
    
    /**
     * 随机模式（完全随机）
     */
    RANDOM,
    
    /**
     * 仅强制响应成员
     */
    FORCED_ONLY
}

/**
 * 群组聊天发言人管理服务实现
 */
class GroupSpeakerServiceImpl : GroupSpeakerService {
    
    private var lastSpeakerIndex = 0  // 用于轮询模式
    
    override fun selectNextSpeaker(
        group: Group,
        strategy: SpeakerSelectionStrategy
    ): GroupMember? {
        val activeMembers = group.members.filter { it.characterId in group.activeMembers }
        
        if (activeMembers.isEmpty()) {
            return null
        }
        
        return when (strategy) {
            SpeakerSelectionStrategy.PROBABILITY_BASED -> selectByProbability(activeMembers)
            SpeakerSelectionStrategy.ROUND_ROBIN -> selectByRoundRobin(activeMembers)
            SpeakerSelectionStrategy.RANDOM -> selectRandomly(activeMembers)
            SpeakerSelectionStrategy.FORCED_ONLY -> selectForcedOnly(activeMembers)
        }
    }
    
    override fun selectMultipleSpeakers(group: Group, count: Int): List<GroupMember> {
        val activeMembers = group.members.filter { it.characterId in group.activeMembers }
        
        if (activeMembers.isEmpty() || count <= 0) {
            return emptyList()
        }
        
        // 先选出强制响应的成员
        val forcedMembers = activeMembers.filter { it.forcedResponse }
        
        // 如果强制成员已经足够，直接返回
        if (forcedMembers.size >= count) {
            return forcedMembers.take(count)
        }
        
        // 否则从剩余成员中按概率补充
        val remainingCount = count - forcedMembers.size
        val otherMembers = activeMembers.filter { !it.forcedResponse }
        val additionalMembers = otherMembers
            .shuffled()
            .take(remainingCount)
        
        return forcedMembers + additionalMembers
    }
    
    override fun calculateSpeakerWeights(group: Group): Map<kotlin.uuid.Uuid, Float> {
        val activeMembers = group.members.filter { it.characterId in group.activeMembers }
        
        if (activeMembers.isEmpty()) {
            return emptyMap()
        }
        
        return activeMembers.associate { member ->
            // 权重 = 优先级 * 响应概率 * (强制响应加成)
            val forcedBonus = if (member.forcedResponse) 2.0f else 1.0f
            val weight = (member.priority + 1) * member.responseProbability * forcedBonus
            member.characterId to weight
        }
    }
    
    /**
     * 基于概率和优先级选择发言人
     */
    private fun selectByProbability(members: List<GroupMember>): GroupMember? {
        // 先检查是否有强制响应的成员
        val forcedMembers = members.filter { it.forcedResponse }
        if (forcedMembers.isNotEmpty()) {
            return forcedMembers.maxByOrNull { it.priority }
        }
        
        // 按概率过滤候选者
        val candidates = members.filter { Random.nextFloat() <= it.responseProbability }
        
        if (candidates.isEmpty()) {
            // 如果没有候选者，选择优先级最高的
            return members.maxByOrNull { it.priority }
        }
        
        // 从候选者中按优先级选择
        return candidates.maxByOrNull { it.priority }
    }
    
    /**
     * 轮询模式选择发言人
     */
    private fun selectByRoundRobin(members: List<GroupMember>): GroupMember? {
        if (members.isEmpty()) return null
        
        val index = lastSpeakerIndex % members.size
        lastSpeakerIndex = (lastSpeakerIndex + 1) % members.size
        
        return members[index]
    }
    
    /**
     * 完全随机选择发言人
     */
    private fun selectRandomly(members: List<GroupMember>): GroupMember? {
        if (members.isEmpty()) return null
        return members.random()
    }
    
    /**
     * 仅选择强制响应成员
     */
    private fun selectForcedOnly(members: List<GroupMember>): GroupMember? {
        val forcedMembers = members.filter { it.forcedResponse }
        if (forcedMembers.isEmpty()) return null
        return forcedMembers.maxByOrNull { it.priority }
    }
}
