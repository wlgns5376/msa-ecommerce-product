package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNode {
    
    private String id;
    private String name;
    private String parentId;
    private int level;
    private int sortOrder;
    private boolean isActive;
    private String fullPath;
    
    @Builder.Default
    private List<CategoryTreeNode> children = new ArrayList<>();
    
    public static CategoryTreeNode from(Category category) {
        return CategoryTreeNode.builder()
                .id(category.getId().value())
                .name(category.getName().value())
                .parentId(category.getParentId() != null ? category.getParentId().value() : null)
                .level(category.getLevel())
                .sortOrder(category.getSortOrder())
                .isActive(category.isActive())
                .fullPath(category.getFullPath())
                .children(new ArrayList<>())
                .build();
    }
    
    public void addChild(CategoryTreeNode child) {
        this.children.add(child);
    }
    
    public void sortChildren() {
        children.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        children.forEach(CategoryTreeNode::sortChildren);
    }
}