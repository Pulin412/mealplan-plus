package com.mealplanplus.api.domain.social

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FollowRepository : JpaRepository<Follow, FollowId> {
    fun existsByFollowerUidAndFolloweeUid(followerUid: String, followeeUid: String): Boolean
    fun deleteByFollowerUidAndFolloweeUid(followerUid: String, followeeUid: String)
    fun countByFolloweeUid(followeeUid: String): Long   // how many follow this user
    fun countByFollowerUid(followerUid: String): Long   // how many this user follows
    fun findByFolloweeUidOrderByCreatedAtDesc(followeeUid: String): List<Follow>  // followers
    fun findByFollowerUidOrderByCreatedAtDesc(followerUid: String): List<Follow>  // following
}

interface BlockRepository : JpaRepository<Block, BlockId> {
    fun existsByBlockerUidAndBlockedUid(blockerUid: String, blockedUid: String): Boolean
    fun deleteByBlockerUidAndBlockedUid(blockerUid: String, blockedUid: String)
    fun findByBlockerUidOrderByCreatedAtDesc(blockerUid: String): List<Block>

    /** True if either user blocks the other — used to hide profiles/content both ways. */
    @Query(
        """
        SELECT (COUNT(b) > 0) FROM Block b
        WHERE (b.blockerUid = :a AND b.blockedUid = :b)
           OR (b.blockerUid = :b AND b.blockedUid = :a)
        """
    )
    fun blockExistsEitherWay(@Param("a") a: String, @Param("b") b: String): Boolean
}

interface ContentReportRepository : JpaRepository<ContentReport, Long>
