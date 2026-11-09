package com.starkgroup.vms.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard Paginated Response DTO conforming to Spring Page JSON structure.
 *
 * Exposes:
 * - content: list of items
 * - number: 0-based page index (alias: page)
 * - size: requested page size
 * - totalElements: total number of records across all pages
 * - totalPages: total number of pages
 * - last: boolean indicating if this is the last page
 * - first: boolean indicating if this is the first page
 * - numberOfElements: number of records on the current page
 * - empty: boolean indicating if page is empty
 */
public class PageResponse<T> {

    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
    private int numberOfElements;
    private boolean empty;

    public PageResponse() {
    }

    public PageResponse(
            List<T> content,
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean last,
            boolean first,
            int numberOfElements,
            boolean empty
    ) {
        this.content = content;
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
        this.first = first;
        this.numberOfElements = numberOfElements;
        this.empty = empty;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst(),
                page.getNumberOfElements(),
                page.isEmpty()
        );
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getPage() {
        return number;
    }

    public void setPage(int page) {
        this.number = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
