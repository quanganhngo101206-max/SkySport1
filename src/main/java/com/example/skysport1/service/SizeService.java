package com.example.skysport1.service;

import com.example.skysport1.entity.Size;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SizeService {

    private final SizeRepository sizeRepository;

    public List<Size> findAll() {
        return sizeRepository.findByDeleteFlagFalse();
    }

    public Size findById(String id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("size", id));
    }

    @Transactional
    public void save(Size size) {
        size.setDeleteFlag(false);
        size.setStatus(1);
        sizeRepository.save(size);
    }

    @Transactional
    public void update(Size size) {
        Size old = findById(size.getId());
        old.setName(size.getName());
        old.setStatus(size.getStatus());
        sizeRepository.save(old);
    }

    @Transactional
    public void delete(String id) {
        Size size = findById(id);
        size.setDeleteFlag(true);
        sizeRepository.save(size);
    }
}