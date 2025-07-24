package com.metrobank.uploadITR.Controller;

import com.metrobank.uploadITR.DTO.UploadDTO;
import com.metrobank.uploadITR.exception.ItrIdValidationException;
import com.metrobank.uploadITR.exception.UserIdValidationException;
import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.service.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public ResponseEntity<?> uploadSample(
            @RequestParam("user_id") String user_id,
            @RequestParam("year") String year,
            @RequestParam("file_path") String file_path,
            @RequestParam("filename") String filename,
            @RequestParam("uploadFile") MultipartFile uploadFile) {

            if (user_id == null || year == null || file_path.isEmpty() || filename.isEmpty() || uploadFile.isEmpty()) {
                throw new UserIdValidationException("Please fill up all the fields.");
            }
            try{
                LocalDateTime dateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("DDMMYYYY_HHMMSS");

                filename = String.format("%s_%s.pdf", filename, formatter.format(dateTime));
                Path directory = Paths.get(file_path);

                if(!Files.exists(directory)){
                    Files.createDirectory(directory);
                }
                if(!uploadFile.getContentType().equals("application/pdf")){
                    return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file type. " + uploadFile.getContentType());
                }

                Path targetFile = directory.resolve(filename);
                if(upload.upload(Integer.parseInt(user_id), Integer.parseInt(year), file_path, filename)){
                    Files.copy(uploadFile.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
                    return ResponseEntity.status(HttpStatus.CREATED).body(String.format("File %s saved successfully.", filename));
                }
                else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed.");
                }
            }
            catch (IOException e){
                return ResponseEntity.status(500).body("File is not copied." + e.getMessage());
            }
    }


    @PostMapping("/remove")
    public ResponseEntity<?> removeSample(@RequestParam(required = false) Integer itr_id) {

        if (itr_id == null) {
            throw new ItrIdValidationException("Please fill up the field.");
        }

        if(!upload.remove(itr_id)) {
            return ResponseEntity.status(500).body(String.format("Itr number: %s is not removed.", itr_id));
        }
        return ResponseEntity.status(200).body(String.format("Itr number: %s is removed.", itr_id));
    }
}
