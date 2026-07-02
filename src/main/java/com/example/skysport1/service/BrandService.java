package com.example.skysport1.service;

import com.example.skysport1.entity.Brand;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> findAll() {
        return brandRepository.findByDeleteFlagFalseOrderByNameAsc();
    }

    public Brand findById(String id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("thương hiệu", id));
    }

    @Transactional
    public void save(Brand brand) {
        brand.setDeleteFlag(false);
        brand.setStatus(1);
        brand.setCreateDate(LocalDateTime.now());
        brand.setUpdateDate(LocalDateTime.now());
        brandRepository.save(brand);
    }

    @Transactional
    public void update(Brand brand) {
        Brand old = findById(brand.getId());
        old.setName(brand.getName());
        old.setDescription(brand.getDescription());
        old.setStatus(brand.getStatus());
        old.setUpdateDate(LocalDateTime.now());
        brandRepository.save(old);
    }

    @Transactional
    public void delete(String id) {
        Brand brand = findById(id);
        brand.setDeleteFlag(true);
        brand.setUpdateDate(LocalDateTime.now());
        brandRepository.save(brand);
    }
}