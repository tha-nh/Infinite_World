package com.infinite.i18n.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class I18nPageResponse {
    private List<I18nTreeNode> data;
    
    // Pagination info
    private Integer currentPage;
    private Integer pageSize;
    private Long totalPages;
    
    // Statistics
    private Long totalKeys;      // Tổng số key (tất cả levels)
    private Long totalKey1;      // Tổng số key1 (root level)
    private Long totalRecords;   // Tổng số records trong page hiện tại
    
    // Search info
    private String searchKey;
    private String searchMessage;
    private Boolean hasSearch;
}