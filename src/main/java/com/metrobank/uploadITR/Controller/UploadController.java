package com.metrobank.uploadITR.Controller;

import com.metrobank.uploadITR.exception.ItrIdValidationException;
import com.metrobank.uploadITR.exception.UserIdValidationException;
import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.service.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/HRHome")
public class UploadController {
    private final Upload upload;

    @Autowired
    public UploadController(Upload upload){
        this.upload = upload;
    }
    @PostMapping("/indexHR")
    public String index() {
        return "indexHR.html";
    }

    @PostMapping("/upload")
    public UploadModel uploadSample(@RequestParam(required = false) Integer user_id, @RequestParam(required = false) Integer year, @RequestParam String file_path){

        if(user_id == null || year == null){
            throw new UserIdValidationException("Please fill up all the fields.");
        }

        return upload.upload(user_id, year, file_path);
    }
    @PostMapping("/remove")
    public List<UploadModel> removeSample(@RequestParam Integer itr_id){

        if(itr_id == null){
            throw new ItrIdValidationException("Please fill up all the fields.");
        }

        upload.remove(itr_id);
        return upload.uploadAll();
    }
}
