package com.example.skysport1.service;

import com.example.skysport1.entity.Supplier;
import com.example.skysport1.exception.DuplicateException;
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
        // Kiểm tra tên trùng — dùng bản IgnoreCase (không phân biệt hoa/thường,
        // vd "Nike" và "nike" vẫn tính là trùng) + DeleteFlagFalse (NCC đã xóa
        // mềm thì không tính là đang chiếm tên/SĐT/email nữa).
        if (supplierRepository.existsByNameIgnoreCaseAndDeleteFlagFalse(supplier.getName())) {
            throw new DuplicateException("Tên nhà cung cấp '" + supplier.getName() + "' đã tồn tại!");
        }
        // Kiểm tra phone trùng — isBlank() thay vì chỉ != null, vì form có thể
        // submit chuỗi rỗng "" cho ô để trống thay vì null.
        if (supplier.getPhone() != null && !supplier.getPhone().isBlank()
                && supplierRepository.existsByPhone(supplier.getPhone())) {
            throw new DuplicateException("Số điện thoại '" + supplier.getPhone() + "' đã được sử dụng!");
        }
        // Kiểm tra email trùng
        if (supplier.getEmail() != null && !supplier.getEmail().isBlank()
                && supplierRepository.existsByEmail(supplier.getEmail())) {
            throw new DuplicateException("Email '" + supplier.getEmail() + "' đã được sử dụng!");
        }

        supplier.setId(idGenerator.generateSupplierId());
        supplier.setDeleteFlag(false);
        supplier.setStatus(1);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(String id, Supplier request) {
        Supplier supplier = findById(id);

        // Kiểm tra tên trùng (trừ chính nó)
        if (!request.getName().equalsIgnoreCase(supplier.getName())
                && supplierRepository.existsByNameIgnoreCaseAndDeleteFlagFalseAndIdNot(request.getName(), id)) {
            throw new DuplicateException("Tên nhà cung cấp '" + request.getName() + "' đã tồn tại!");
        }
        // Kiểm tra phone trùng (trừ chính nó)
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(supplier.getPhone())
                && supplierRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new DuplicateException("Số điện thoại '" + request.getPhone() + "' đã được sử dụng!");
        }
        // Kiểm tra email trùng (trừ chính nó)
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(supplier.getEmail())
                && supplierRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateException("Email '" + request.getEmail() + "' đã được sử dụng!");
        }

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