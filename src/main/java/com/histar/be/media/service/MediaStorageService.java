package com.histar.be.media.service;

public interface MediaStorageService {

    String uploadImage(byte[] data, String contentType, String objectKey);
}
