package com.histar.be.media.service;

public interface MediaStorageService {

    String uploadImage(byte[] data, String contentType, String objectKey);

    default byte[] downloadImage(String objectUrl) {
        throw new UnsupportedOperationException("Object download is not supported by this storage provider");
    }

    default void deleteObject(String objectUrl) {
        throw new UnsupportedOperationException("Object deletion is not supported by this storage provider");
    }
}
