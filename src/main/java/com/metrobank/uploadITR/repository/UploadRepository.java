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

    //Insert is not allowed in native query
    /*@Modifying
    @Transactional
    @Query(value = "INSERT INTO ITR_Records (user_id, year, file_path) VALUES (?1, ?2, ?3)", nativeQuery = true)
    UploadModel uploadItrData(int user_id, int year, String file_path);*/
}
