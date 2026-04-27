package com.infinite.i18n.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class I18nTreeNode {
    private String key;
    private String message;
    private String fullKey;
    private Integer level;
    
    @Builder.Default
    private List<I18nTreeNode> children = new ArrayList<>();
    
    // Metadata
    private Long totalChildren;
}