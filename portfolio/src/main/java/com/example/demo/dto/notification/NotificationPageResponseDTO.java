package com.example.demo.dto.notification;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class NotificationPageResponseDTO {

    private List<NotificationResponseDTO> notifications;

    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    private boolean hasPrevious;
    private boolean hasNext;
    private boolean hasFirst;
    private boolean hasLast;

    private int jumpBackwardPage;
    private int jumpForwardPage;

    public static NotificationPageResponseDTO fromPage(Page<NotificationResponseDTO> page) {
        return NotificationPageResponseDTO.builder()
        								  // 한 페이지당 받을 데이터(ex: 1페이지에 10개의 데이터 기준은 Size())
                                          .notifications(page.getContent())
                                          .pageNumber(page.getNumber())
                                          .pageSize(page.getSize())
                                          .totalElements(page.getTotalElements())
                                          .totalPages(page.getTotalPages())
                                          .hasPrevious(page.getNumber() > 0)
                                          .hasNext(page.getNumber() < page.getTotalPages() - 1)
                                          .hasFirst(page.getNumber() > 0)
                                          .hasLast(page.getNumber() < page.getTotalPages() - 1)
                                          .jumpBackwardPage(Math.max(page.getNumber() - 10, 0))
                                          .jumpForwardPage(page.getTotalPages() == 0 ? 0 : Math.min(page.getNumber() + 10, page.getTotalPages() - 1))
                                          .build();
    }
}
