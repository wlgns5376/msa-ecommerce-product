package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCategoryTreeService implements GetCategoryTreeUseCase {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    public GetCategoryTreeResponse execute(GetCategoryTreeRequest request) {
        // 모든 카테고리를 한 번에 조회
        List<Category> allCategories = categoryRepository.findAll();
        
        // 부모 ID별로 자식 카테고리를 그룹화
        Map<CategoryId, List<Category>> childrenByParentId = new HashMap<>();
        List<Category> rootCategories = new ArrayList<>();
        
        for (Category category : allCategories) {
            if (category.getParentId() == null) {
                rootCategories.add(category);
            } else {
                childrenByParentId.computeIfAbsent(category.getParentId(), k -> new ArrayList<>())
                        .add(category);
            }
        }
        
        List<CategoryTreeNode> treeNodes = rootCategories.stream()
                .filter(category -> request.isIncludeInactive() || category.isActive())
                .map(category -> buildCategoryTree(category, childrenByParentId))
                .sorted(Comparator.comparingInt(CategoryTreeNode::getSortOrder))
                .collect(Collectors.toList());
        
        if (!request.isIncludeInactive()) {
            treeNodes = filterInactiveNodes(treeNodes);
        }
        
        return GetCategoryTreeResponse.of(treeNodes);
    }
    
    private CategoryTreeNode buildCategoryTree(Category category, Map<CategoryId, List<Category>> childrenByParentId) {
        CategoryTreeNode node = CategoryTreeNode.from(category);
        
        List<Category> children = childrenByParentId.getOrDefault(category.getId(), new ArrayList<>());
        
        for (Category child : children) {
            CategoryTreeNode childNode = buildCategoryTree(child, childrenByParentId);
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