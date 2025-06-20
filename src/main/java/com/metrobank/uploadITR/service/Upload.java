package com.metrobank.uploadITR.service;

import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.repository.UploadRepository;
import org.springframework.stereotype.Service;

@Service
public class Upload {
    private UploadRepository uploadRepository;
    public Upload(UploadRepository uploadRepository){
        this.uploadRepository = uploadRepository;
    }

    //Upload Backend Logic
    public UploadModel upload (int user_id, int year, String file_path){
        return uploadRepository.uploadItrData(user_id, year, file_path);
    }
}
