package com.metrobank.uploadITR.service;

import com.metrobank.uploadITR.UploadItrApplication;
import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.repository.UploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.*;

@Service
public class Upload {
    private UploadRepository uploadRepository;
    @Autowired
    public Upload(UploadRepository uploadRepository){
        this.uploadRepository = uploadRepository;
    }

    //Upload Backend Logic
    public UploadModel upload (int user_id, int year, String file_path){
        UploadModel model = new UploadModel();
        model.setUserId(user_id);
        model.setYear(year);
        model.setFilePath(file_path);
        return uploadRepository.save(model);
    }
}
