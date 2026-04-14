package com.infinite.user.repository;

import com.infinite.user.dto.response.UserDto;
import com.infinite.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
                SELECT u
                FROM User u
                WHERE u.username = :username
                  AND u.isDelete = false
            """)
    Optional<User> login(@Param("username") String username);

    @Query("""
                SELECT new com.infinite.user.dto.response.UserDto(
                    u.id,
                    u.username,
                    u.name,
                    u.email,
                    u.active
                )
                FROM User u
                WHERE (u.active IS NOT NULL)
                  AND (COALESCE(:searchKey, '') = ''
                       OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                       OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchKey, '%')))
                  AND (:active IS NULL OR u.active = :active)
                ORDER BY u.createdTime DESC
            """)
    Page<UserDto> searchUsers(
            @Param("searchKey") String searchKey,
            @Param("active") Integer active,
            Pageable pageable);

    @Query("""
            SELECT COUNT(*) > 0
            FROM User u
            WHERE (:id IS NULL OR u.id != :id)
                AND u.username = :username
                AND u.isDelete = false
            """)
    boolean existsByUsername(
            @Param("id") Long id,
            @Param("username") String username);

    @Query("""
            SELECT COUNT(*) > 0
            FROM User u
            WHERE (:id IS NULL OR u.id != :id)
                AND u.email = :email
                AND u.isDelete = false
            """)
    boolean existsByEmail(
            @Param("id") Long id,
            @Param("email") String email);

    Optional<User> findByUsername(String username);
}
