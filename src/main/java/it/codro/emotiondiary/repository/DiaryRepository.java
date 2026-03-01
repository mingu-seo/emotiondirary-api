package it.codro.emotiondiary.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.codro.emotiondiary.entity.Diary;

public interface DiaryRepository extends JpaRepository<Diary, String> {

    List<Diary> findByDateBetweenOrderByDateDesc(Long from, Long to);

    List<Diary> findByDateBetweenOrderByDateAsc(Long from, Long to);
}
