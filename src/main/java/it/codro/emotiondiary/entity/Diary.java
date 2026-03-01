package it.codro.emotiondiary.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Long date;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "emotion_id", nullable = false)
    private Integer emotionId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Diary(Long date, String content, Integer emotionId) {
        this.date = date;
        this.content = content;
        this.emotionId = emotionId;
    }

    @PrePersist
    protected void onCreate() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Long date, String content, Integer emotionId) {
        this.date = date;
        this.content = content;
        this.emotionId = emotionId;
    }
}
