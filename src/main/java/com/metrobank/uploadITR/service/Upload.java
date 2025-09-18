package com.metrobank.uploadITR.service;

import com.metrobank.uploadITR.exception.FilePathValidationException;
import com.metrobank.uploadITR.exception.ItrIdValidationException;
import com.metrobank.uploadITR.exception.UserIdValidationException;
import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.repository.UploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Upload {
    private final UploadRepository uploadRepository;

    @Autowired
    public Upload(UploadRepository uploadRepository){
        this.uploadRepository = uploadRepository;
    }

    // For uploading new ITR
    public boolean upload(int user_id, int year, String file_path, String filename, String pdf_password) {
        try {
            if (uploadRepository.existsByUserIdAndYear(user_id, year) > 0) {
                return false;
            }
            UploadModel model = new UploadModel();
            model.setUserId(user_id);
            model.setYear(year);
            model.setFilePath(file_path);
            model.setFilename(filename);
            model.setPdfPassword(pdf_password);
            model.setStatus("active");
            uploadRepository.save(model);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // For updating an existing ITR
    public boolean update(int itr_id, int year, String file_path, String filename, String pdf_password) {
        try {
            UploadModel record = uploadRepository.findById((long) itr_id).orElse(null);
            if (record == null || !"active".equals(record.getStatus())) {
                return false;
            }

            int userId = record.getUserId();
            int count = uploadRepository.countByUserIdAndYearExcludingItr(userId, year, itr_id);
            if (count > 0) {
                return false;
            }

            uploadRepository.updateItrById(itr_id, year, file_path, filename, pdf_password);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //removing itr record
    public boolean remove(int itr_id) {
        try {
            if (uploadRepository.existByItrId(itr_id) == 0) {
                throw new ItrIdValidationException("This ITR record does not exist or is already removed.");
            }
            uploadRepository.softDeleteItrById(itr_id);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public List<UploadModel> uploadAll() {
        return uploadRepository.streamAllActive();
    }
}
