package com.histar.be.conversation.service;

import com.histar.be.conversation.entity.Conversation;
import java.util.List;
import java.util.UUID;


public interface ConversationService {

    List<Conversation> findAll();

    Conversation findById(UUID id);

    Conversation save(Conversation entity);

    void deleteById(UUID id);

    long count();
}
