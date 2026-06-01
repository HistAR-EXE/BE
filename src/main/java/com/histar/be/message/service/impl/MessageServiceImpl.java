package com.histar.be.message.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.message.entity.Message;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.message.service.MessageService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository repository;

    @Override
    public List<Message> findAll() {
        return repository.findAll();
    }

    @Override
    public Message findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + id));
    }

    @Override
    public Message save(Message entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }
    @Override
    public List<Message> findByConversationIdOrderByCreatedAt(UUID conversationId) {
        return repository.findByConversationIdOrderByCreatedAt(conversationId);
    }

}
