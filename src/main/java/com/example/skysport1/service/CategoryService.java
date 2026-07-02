package com.example.skysport1.service;

import com.example.skysport1.entity.Category;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findByDeleteFlagFalseOrderByNameAsc();
    }

    public Category findById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("danh mục", id));
    }

    @Transactional
    public void save(Category category) {
        category.setDeleteFlag(false);
        category.setStatus(1);
        category.setCreateDate(LocalDateTime.now());
        category.setUpdateDate(LocalDateTime.now());
        categoryRepository.save(category);
    }

    @Transactional
    public void update(Category category) {
        Category old = findById(category.getId());
        old.setName(category.getName());
        old.setDescription(category.getDescription());
        old.setStatus(category.getStatus());
        old.setUpdateDate(LocalDateTime.now());
        categoryRepository.save(old);
    }

    @Transactional
    public void delete(String id) {
        Category category = findById(id);
        category.setDeleteFlag(true);
        category.setUpdateDate(LocalDateTime.now());
        categoryRepository.save(category);
    }
}