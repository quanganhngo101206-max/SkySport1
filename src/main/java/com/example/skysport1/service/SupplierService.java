package com.example.skysport1.service;

import com.example.skysport1.entity.Supplier;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.SupplierRepository;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final IdGenerator idGenerator;

    public List<Supplier> findAll() {
        return supplierRepository.findByDeleteFlagFalseOrderByNameAsc();
    }

    public Supplier findById(String id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("nhà cung cấp", id));
    }

    public List<Supplier> search(String keyword) {
        return supplierRepository.searchByName(keyword);
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        supplier.setId(idGenerator.generateSupplierId());
        supplier.setDeleteFlag(false);
        supplier.setStatus(1);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(String id, Supplier request) {
        Supplier supplier = findById(id);
        supplier.setName(request.getName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setStatus(request.getStatus());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void delete(String id) {
        Supplier supplier = findById(id);
        supplier.setDeleteFlag(true);
        supplierRepository.save(supplier);
    }
}
