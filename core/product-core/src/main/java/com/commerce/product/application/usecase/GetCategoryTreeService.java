package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCategoryTreeService implements GetCategoryTreeUseCase {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    public GetCategoryTreeResponse execute(GetCategoryTreeRequest request) {
        List<Category> rootCategories = categoryRepository.findRootCategories();
        
        List<CategoryTreeNode> treeNodes = rootCategories.stream()
                .filter(category -> request.isIncludeInactive() || category.isActive())
                .map(this::buildCategoryTree)
                .sorted(Comparator.comparingInt(CategoryTreeNode::getSortOrder))
                .collect(Collectors.toList());
        
        if (!request.isIncludeInactive()) {
            treeNodes = filterInactiveNodes(treeNodes);
        }
        
        return GetCategoryTreeResponse.of(treeNodes);
    }
    
    private CategoryTreeNode buildCategoryTree(Category category) {
        CategoryTreeNode node = CategoryTreeNode.from(category);
        
        List<Category> children = categoryRepository.findByParentId(category.getId());
        
        for (Category child : children) {
            CategoryTreeNode childNode = buildCategoryTree(child);
            node.addChild(childNode);
        }
        
        node.sortChildren();
        
        return node;
    }
    
    private List<CategoryTreeNode> filterInactiveNodes(List<CategoryTreeNode> nodes) {
        List<CategoryTreeNode> filtered = new ArrayList<>();
        
        for (CategoryTreeNode node : nodes) {
            if (node.isActive()) {
                CategoryTreeNode filteredNode = CategoryTreeNode.builder()
                        .id(node.getId())
                        .name(node.getName())
                        .parentId(node.getParentId())
                        .level(node.getLevel())
                        .sortOrder(node.getSortOrder())
                        .isActive(node.isActive())
                        .fullPath(node.getFullPath())
                        .children(filterInactiveNodes(node.getChildren()))
                        .build();
                filtered.add(filteredNode);
            }
        }
        
        return filtered;
    }
}