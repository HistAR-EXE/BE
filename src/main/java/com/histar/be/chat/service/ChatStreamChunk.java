package com.histar.be.chat.service;

import com.histar.be.chat.dto.ChatSource;
import java.util.List;

public record ChatStreamChunk(String content, List<ChatSource> sources) {}
