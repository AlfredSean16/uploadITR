package com.metrobank.uploadITR.service;

import com.metrobank.uploadITR.exception.FilePathValidationException;
import com.metrobank.uploadITR.exception.ItrIdValidationException;
import com.metrobank.uploadITR.exception.UserIdValidationException;
import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.repository.UploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class Upload {
    private final UploadRepository uploadRepository;
    @Autowired
    public Upload(UploadRepository uploadRepository){
        this.uploadRepository = uploadRepository;
    }

    //Upload Backend Logic
    public boolean upload (int user_id, int year, String file_path, String filename){

        try {
            if(uploadRepository.existByUserId(user_id) == 0){
                throw new UserIdValidationException("This user does not exist.");
            }
            UploadModel model = new UploadModel();
            model.setUserId(user_id);
            model.setYear(year);
            model.setFilePath(file_path);
            model.setFilename(filename);
            uploadRepository.save(model);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }

    //For updating an itr record
    public boolean update(int itr_id, int year, String file_path, String filename) {
        try {
            if (uploadRepository.existByItrId(itr_id) == 0) {
                throw new ItrIdValidationException("ITR record does not exist.");
            }
            uploadRepository.updateItrById(itr_id, year, file_path, filename);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //For removing an itr record
    public boolean remove (int itr_id){

        try {
            if (uploadRepository.existByItrId(itr_id) == 0) {
                throw new ItrIdValidationException("This ITR record does not exist.");
            }
            uploadRepository.deleteItrById(itr_id);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }

    //for selecting all inside the itr records table
    public List<UploadModel> uploadAll(){
        return uploadRepository.streamAll();
    }
}
