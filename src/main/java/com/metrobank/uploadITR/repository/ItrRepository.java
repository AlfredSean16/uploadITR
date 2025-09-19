package com.metrobank.uploadITR.repository;

import com.metrobank.uploadITR.entity.ItrRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ItrRepository extends JpaRepository<ItrRecord, Long> {
    // Find active ITR records
    @Query("SELECT i FROM ItrRecord i WHERE i.status = 'active'")
    List<ItrRecord> findAllActive();

    // Find by user ID and active status
    @Query("SELECT i FROM ItrRecord i WHERE i.userId = :userId AND i.status = 'active'")
    List<ItrRecord> findActiveByUserId(@Param("userId") Long userId);

    // Check if user exists in users table
    @Query(value = "SELECT COUNT(*) FROM users WHERE user_id = :userId", nativeQuery = true)
    int countUserById(@Param("userId") Long userId);

    // Check if user has ITR for specific year
    @Query("SELECT COUNT(i) FROM ItrRecord i WHERE i.userId = :userId AND i.year = :year AND i.status = 'active'")
    int countActiveByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    // Check for duplicates when updating (excluding current ITR)
    @Query("SELECT COUNT(i) FROM ItrRecord i WHERE i.userId = :userId AND i.year = :year AND i.itrId <> :itrId AND i.status = 'active'")
    int countDuplicateForUpdate(@Param("userId") Long userId, @Param("year") Integer year, @Param("itrId") Long itrId);

    // Get user email from users table
    @Query(value = "SELECT email FROM users WHERE user_id = :userId", nativeQuery = true)
    Optional<String> findUserEmailById(@Param("userId") Long userId);

    // Get user name from users table
    @Query(value = "SELECT name FROM users WHERE user_id = :userId", nativeQuery = true)
    Optional<String> findUserNameById(@Param("userId") Long userId);

    // Soft delete ITR record
    @Modifying
    @Transactional
    @Query("UPDATE ItrRecord i SET i.status = 'removed' WHERE i.itrId = :itrId AND i.status = 'active'")
    int softDeleteById(@Param("itrId") Long itrId);

    // Update ITR record
    @Modifying
    @Transactional
    @Query("UPDATE ItrRecord i SET i.year = :year, i.filePath = :filePath, i.filename = :filename, i.pdfPassword = :password WHERE i.itrId = :itrId AND i.status = 'active'")
    int updateItrRecord(@Param("itrId") Long itrId,
                        @Param("year") Integer year,
                        @Param("filePath") String filePath,
                        @Param("filename") String filename,
                        @Param("password") String password);

    // Find by user ID and year (active only)
    @Query("SELECT i FROM ItrRecord i WHERE i.userId = :userId AND i.year = :year AND i.status = 'active'")
    List<ItrRecord> findActiveByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    // Find ITR by ID and active status
    @Query("SELECT i FROM ItrRecord i WHERE i.itrId = :itrId AND i.status = 'active'")
    Optional<ItrRecord> findActiveById(@Param("itrId") Long itrId);

    // Get user details with ITR statistics
    @Query(value = "SELECT u.name, u.email, COUNT(i.itr_id) as itr_count FROM users u " +
            "LEFT JOIN itr_records i ON u.id = i.user_id AND i.status = 'active' " +
            "WHERE u.id = :userId GROUP BY u.name, u.email", nativeQuery = true)
    Object[] findUserWithItrStats(@Param("userId") Long userId);
}
