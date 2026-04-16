package com.agrichain.farmer.dto;

import java.util.List;

/**
 * Paginated response wrapper for farmer lists.
 * Returned by GET /farmers when pagination params are provided.
 */
public class FarmerPageResponse {

    private List<FarmerProfileResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public FarmerPageResponse(List<FarmerProfileResponse> content,
                               int page, int size,
                               long totalElements, int totalPages) {
        this.content       = content;
        this.page          = page;
        this.size          = size;
        this.totalElements = totalElements;
        this.totalPages    = totalPages;
    }

    public List<FarmerProfileResponse> getContent()  { return content; }
    public int getPage()                              { return page; }
    public int getSize()                              { return size; }
    public long getTotalElements()                    { return totalElements; }
    public int getTotalPages()                        { return totalPages; }
}
