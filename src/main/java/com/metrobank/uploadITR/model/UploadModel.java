package com.metrobank.uploadITR.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter //not working
@Setter //not working
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ITR_Records")
public class UploadModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "itr_id")
    private int itr_id;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "year")
    private int year;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "filename")
    private String filename;

    @Column(name = "pdf_password")
    private String pdf_password;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilename(String filename){this.filename = filename; }

    public void setPdfPassword(String pdf_password){ this.pdf_password = pdf_password; }

    public String getFile(){
        return filePath.concat(filename);
    }

    public String getFilename() {
        return filename;
    }

    public int getItr_id() {
        return itr_id;
    }

    public int getUserId() {
        return userId;
    }

    public int getYear() {
        return year;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getPdfPassword(){ return pdf_password; }
}
