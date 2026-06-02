package com.histar.be.character.controller;

import com.histar.be.character.dto.CharacterResponse;
import com.histar.be.character.service.CharacterService;
import com.histar.be.common.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping("/by-location/{locationId}")
    public ApiResponse<List<CharacterResponse>> findByLocation(@PathVariable UUID locationId) {
        List<CharacterResponse> data = characterService.findByLocationId(locationId).stream()
                .map(CharacterResponse::from)
                .toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<CharacterResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(CharacterResponse.from(characterService.findById(id)));
    }
}
