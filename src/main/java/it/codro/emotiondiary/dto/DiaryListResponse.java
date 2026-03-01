package it.codro.emotiondiary.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiaryListResponse {

    private List<DiaryResponse> items;
    private int total;
}
