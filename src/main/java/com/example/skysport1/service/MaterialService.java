package com.example.skysport1.service;

import com.example.skysport1.entity.Material;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    public List<Material> findAll() {
        return materialRepository.findByDeleteFlagFalseOrderByNameAsc();
    }

    public Material findById(String id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("chất liệu", id));
    }

    @Transactional
    public void save(Material material) {
        material.setDeleteFlag(false);
        material.setStatus(1);
        material.setCreateDate(LocalDateTime.now());
        material.setUpdateDate(LocalDateTime.now());
        materialRepository.save(material);
    }

    @Transactional
    public void update(Material material) {
        Material old = findById(material.getId());
        old.setName(material.getName());
        old.setDescription(material.getDescription());
        old.setStatus(material.getStatus());
        old.setUpdateDate(LocalDateTime.now());
        materialRepository.save(old);
    }

    @Transactional
    public void delete(String id) {
        Material material = findById(id);
        material.setDeleteFlag(true);
        material.setUpdateDate(LocalDateTime.now());
        materialRepository.save(material);
    }
}