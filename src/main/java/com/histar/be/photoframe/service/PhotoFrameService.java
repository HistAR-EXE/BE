package com.histar.be.photoframe.service;

import com.histar.be.photoframe.entity.PhotoFrame;
import java.util.List;
import java.util.UUID;


public interface PhotoFrameService {

    List<PhotoFrame> findAll();

    PhotoFrame findById(UUID id);

    PhotoFrame save(PhotoFrame entity);

    void deleteById(UUID id);

    long count();
}
