package com.mealplanplus.api.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    fun findByFirebaseUid(firebaseUid: String): User?

    // Handles are stored already-normalized (lowercase); compare on lower() so the
    // lookup is case-insensitive on H2 too (prod handle column is citext).
    @Query("SELECT u FROM User u WHERE lower(u.handle) = lower(:handle)")
    fun findByHandle(@Param("handle") handle: String): User?

    fun existsByHandleIgnoreCase(handle: String): Boolean

    /**
     * Searchable users whose handle or display name matches [q] (case-insensitive substring),
     * excluding [selfUid] and anyone in a block relationship either way with the searcher.
     */
    @Query(
        """
        SELECT u FROM User u
        WHERE u.isSearchable = true
          AND u.handle IS NOT NULL
          AND u.firebaseUid <> :selfUid
          AND (lower(u.handle) LIKE lower(concat('%', :q, '%'))
               OR lower(u.displayName) LIKE lower(concat('%', :q, '%')))
          AND NOT EXISTS (
              SELECT b FROM Block b
              WHERE (b.blockerUid = :selfUid AND b.blockedUid = u.firebaseUid)
                 OR (b.blockerUid = u.firebaseUid AND b.blockedUid = :selfUid)
          )
        ORDER BY u.handle ASC
        """
    )
    fun searchProfiles(
        @Param("q") q: String,
        @Param("selfUid") selfUid: String,
        pageable: org.springframework.data.domain.Pageable
    ): List<User>
}
