package it.codro.emotiondiary.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.codro.emotiondiary.dto.DiaryListResponse;
import it.codro.emotiondiary.dto.DiaryRequest;
import it.codro.emotiondiary.dto.DiaryResponse;
import it.codro.emotiondiary.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping
    public ResponseEntity<DiaryListResponse> list(
            @RequestParam Long from,
            @RequestParam Long to,
            @RequestParam(defaultValue = "latest") String sort) {
        return ResponseEntity.ok(diaryService.list(from, to, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiaryResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(diaryService.getById(id));
    }

    @PostMapping
    public ResponseEntity<DiaryResponse> create(@Valid @RequestBody DiaryRequest request) {
        DiaryResponse response = diaryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiaryResponse> update(
            @PathVariable String id,
            @Valid @RequestBody DiaryRequest request) {
        return ResponseEntity.ok(diaryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        diaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
