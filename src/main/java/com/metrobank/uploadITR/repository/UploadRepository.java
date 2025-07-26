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

    @Query(value = "SELECT COUNT(*) FROM Users WHERE user_id = :user_id", nativeQuery = true)
    int existByUserId(int user_id);

    @Query(value = "SELECT COUNT(*) FROM ITR_Records WHERE itr_id = :user_id", nativeQuery = true)
    int existByItrId(int user_id);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ITR_Records WHERE itr_id = :itr_id", nativeQuery = true)
    void deleteItrById(int itr_id);
    @Modifying
    @Transactional
    @Query(value = "UPDATE ITR_Records SET year = :year, file_path = :file_path, filename = :filename WHERE itr_id = :itr_id", nativeQuery = true)
    void updateItrById(int itr_id, int year, String file_path, String filename);

}
