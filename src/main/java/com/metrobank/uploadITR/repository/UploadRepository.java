package com.metrobank.uploadITR.repository;

import com.metrobank.uploadITR.model.UploadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UploadRepository extends JpaRepository<UploadModel, Long> {
    @Query(value = "SELECT * FROM ITR_Records", nativeQuery = true)
    List<UploadModel> streamAll();

    @Query(value = "SELECT COUNT(*) FROM Users WHERE user_id = ?1", nativeQuery = true)
    int existByUserId(int user_id);

    @Query(value = "SELECT COUNT(*) FROM ITR_Records WHERE itr_id = ?1", nativeQuery = true)
    int existByItrId(int user_id);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ITR_Records WHERE itr_id = ?1", nativeQuery = true)
    void deleteItrById(int itr_id);

}
