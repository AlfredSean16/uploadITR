package com.metrobank.uploadITR.exception;

public class UserIdValidationException extends RuntimeException{
    public UserIdValidationException(String message){
        super(message);
    }
}
