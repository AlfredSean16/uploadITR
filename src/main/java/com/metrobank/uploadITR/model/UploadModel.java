package com.metrobank.uploadITR.model;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UploadModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int itr_id;
    private int user_id;
    private int year;
    private String file_path;

}
