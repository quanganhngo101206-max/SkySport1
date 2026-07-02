package com.example.skysport1.service;

import com.example.skysport1.entity.Color;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    public List<Color> findAll() {
        return colorRepository.findByDeleteFlagFalse();
    }

    public Color findById(String id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("màu sắc", id));
    }

    @Transactional
    public void save(Color color) {
        color.setDeleteFlag(false);
        color.setStatus(1);
        colorRepository.save(color);
    }

    @Transactional
    public void update(Color color) {
        Color old = findById(color.getId());
        old.setName(color.getName());
        old.setHexCode(color.getHexCode());
        old.setStatus(color.getStatus());
        colorRepository.save(old);
    }

    @Transactional
    public void delete(String id) {
        Color color = findById(id);
        color.setDeleteFlag(true);
        colorRepository.save(color);
    }
}