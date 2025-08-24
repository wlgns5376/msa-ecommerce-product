package com.commerce.product.domain.model;

import java.util.List;

/**
 * 페이징된 결과를 표현하는 도메인 모델
 * Spring Data의 Page 인터페이스에 대한 의존성을 제거하기 위해 사용됩니다.
 * 
 * @param <T> 페이지에 포함된 컨텐츠의 타입
 */
public class PagedResult<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PagedResult(List<T> content, int page, int size, long totalElements) {
        this.content = List.copyOf(content);
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return page < totalPages - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean isFirst() {
        return page == 0;
    }

    public boolean isLast() {
        return page == totalPages - 1;
    }
}