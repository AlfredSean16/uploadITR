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

    //Upload Backend Logic
    public UploadModel upload (int user_id, int year, String file_path){

        if(uploadRepository.existByUserId(user_id) == 0){
            throw new UserIdValidationException("This user does not exist.");
        }
        if(file_path.isEmpty()){
            throw new FilePathValidationException("File path must not be empty.");
        }
        if(!file_path.contains(".pdf")){
            throw new FilePathValidationException("This is not a .pdf file.");
        }

        UploadModel model = new UploadModel();
        model.setUserId(user_id);
        model.setYear(year);
        model.setFilePath(file_path);
        return uploadRepository.save(model);
    }

    //For removing an itr record
    public void remove (int itr_id){

        if(uploadRepository.existByItrId(itr_id) == 0){
            throw new ItrIdValidationException("This ITR record does not exist.");
        }

        uploadRepository.deleteItrById(itr_id);
    }

    //for selecting all inside the itr records table
    public List<UploadModel> uploadAll(){
        return uploadRepository.streamAll();
    }
}
