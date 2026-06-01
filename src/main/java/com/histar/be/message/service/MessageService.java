package com.histar.be.message.service;

import com.histar.be.message.entity.Message;
import java.util.List;
import java.util.UUID;
import java.util.List;

public interface MessageService {

    List<Message> findAll();

    Message findById(UUID id);

    Message save(Message entity);

    void deleteById(UUID id);

    long count();
    List<Message> findByConversationIdOrderByCreatedAt(UUID conversationId);
}
