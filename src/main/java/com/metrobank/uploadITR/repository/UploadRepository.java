package com.metrobank.uploadITR.repository;

import com.metrobank.uploadITR.model.UploadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UploadRepository extends JpaRepository<UploadModel, Long> {
    @Query(value = "SELECT * FROM ITR_Records WHERE status = 'active'", nativeQuery = true)
    List<UploadModel> streamAllActive();

    @Query(value = "SELECT COUNT(*) FROM Users WHERE user_id = :user_id", nativeQuery = true)
    int existByUserId(@Param("user_id") int userId);

    @Query(value = "SELECT COUNT(*) FROM ITR_Records WHERE itr_id = :itr_id AND status = 'active'", nativeQuery = true)
    int existByItrId(@Param("itr_id") int itrId);

    @Query(value = "SELECT COUNT(*) FROM ITR_Records WHERE user_id = :user_id AND year = :year AND status = 'active'", nativeQuery = true)
    int existsByUserIdAndYear(@Param("user_id") int userId, @Param("year") int year);

    @Query(value = "SELECT COUNT(*) FROM ITR_Records WHERE user_id = :user_id AND year = :year AND itr_id <> :itr_id AND status = 'active'", nativeQuery = true)
    int countByUserIdAndYearExcludingItr(@Param("user_id") int userId, @Param("year") int year, @Param("itr_id") int itrId);

    @Query(value = "SELECT email FROM Users WHERE user_id = :user_id", nativeQuery = true)
    String findEmailByUserId(@Param("user_id") int userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE ITR_Records SET status = 'removed' WHERE itr_id = :itr_id", nativeQuery = true)
    void softDeleteItrById(@Param("itr_id") int itr_id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE ITR_Records SET year = :year, file_path = :file_path, filename = :filename, pdf_password = :pdf_password WHERE itr_id = :itr_id AND status = 'active'", nativeQuery = true)
    void updateItrById(@Param("itr_id") int itr_id,
                       @Param("year") int year,
                       @Param("file_path") String file_path,
                       @Param("filename") String filename,
                       @Param("pdf_password") String pdf_password);

}
