package com.commerce.product.application.usecase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCategoryTreeResponse {
    
    private List<CategoryTreeNode> categories;
    private int totalCount;
    
    public static GetCategoryTreeResponse of(List<CategoryTreeNode> categories) {
        return GetCategoryTreeResponse.builder()
                .categories(categories)
                .totalCount(countAllNodes(categories))
                .build();
    }
    
    private static int countAllNodes(List<CategoryTreeNode> nodes) {
        int count = nodes.size();
        for (CategoryTreeNode node : nodes) {
            count += countAllNodes(node.getChildren());
        }
        return count;
    }
}