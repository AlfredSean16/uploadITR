package com.metrobank.uploadITR.Controller;

import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.service.Upload;
import org.springframework.beans.factory.annotation.Autowired;
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
    public UploadModel uploadSample(@RequestParam int user_id, @RequestParam int year, @RequestParam String file_path){
        return upload.upload(user_id, year, file_path);
    }
    @PostMapping("/remove")
    public List<UploadModel> removeSample(@RequestParam int itr_id){
        upload.remove(itr_id);
        return upload.uploadAll();
    }
}
