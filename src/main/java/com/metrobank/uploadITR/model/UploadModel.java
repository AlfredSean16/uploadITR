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
    private int itr_id;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "year")
    private int year;

    @Column(name = "file_path")
    private String filePath;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
