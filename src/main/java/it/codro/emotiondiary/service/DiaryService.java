package it.codro.emotiondiary.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.codro.emotiondiary.dto.DiaryListResponse;
import it.codro.emotiondiary.dto.DiaryRequest;
import it.codro.emotiondiary.dto.DiaryResponse;
import it.codro.emotiondiary.entity.Diary;
import it.codro.emotiondiary.exception.DiaryNotFoundException;
import it.codro.emotiondiary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;

    public DiaryListResponse list(Long from, Long to, String sort) {
        List<Diary> diaries;

        if ("oldest".equals(sort)) {
            diaries = diaryRepository.findByDateBetweenOrderByDateAsc(from, to);
        } else {
            diaries = diaryRepository.findByDateBetweenOrderByDateDesc(from, to);
        }

        List<DiaryResponse> items = diaries.stream()
                .map(DiaryResponse::from)
                .toList();

        return DiaryListResponse.builder()
                .items(items)
                .total(items.size())
                .build();
    }

    public DiaryResponse getById(String id) {
        Diary diary = diaryRepository.findById(id)
                .orElseThrow(() -> new DiaryNotFoundException(id));
        return DiaryResponse.from(diary);
    }

    @Transactional
    public DiaryResponse create(DiaryRequest request) {
        Diary diary = Diary.builder()
                .date(request.getDate())
                .content(request.getContent())
                .emotionId(request.getEmotionId())
                .build();

        Diary saved = diaryRepository.save(diary);
        return DiaryResponse.from(saved);
    }

    @Transactional
    public DiaryResponse update(String id, DiaryRequest request) {
        Diary diary = diaryRepository.findById(id)
                .orElseThrow(() -> new DiaryNotFoundException(id));

        diary.update(request.getDate(), request.getContent(), request.getEmotionId());
        return DiaryResponse.from(diary);
    }

    @Transactional
    public void delete(String id) {
        Diary diary = diaryRepository.findById(id)
                .orElseThrow(() -> new DiaryNotFoundException(id));
        diaryRepository.delete(diary);
    }
}
