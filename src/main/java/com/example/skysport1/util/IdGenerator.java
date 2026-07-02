package com.example.skysport1.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import com.example.skysport1.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdGenerator {

    // Inject tất cả repository cần đọc id
    private final AccountRepository      accountRepository;
    private final CustomerRepository     customerRepository;
    private final StaffRepository        staffRepository;
    private final BillRepository         billRepository;
    private final ImportOrderRepository  importOrderRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final BillReturnRepository   billReturnRepository;
    private final WishlistRepository     wishlistRepository;
    private final CartRepository         cartRepository;
    private final BrandRepository        brandRepository;
    private final CategoryRepository     categoryRepository;
    private final MaterialRepository     materialRepository;
    private final SizeRepository         sizeRepository;
    private final ColorRepository        colorRepository;
    private final ProductRepository      productRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicInteger accountCounter    = new AtomicInteger(1);
    private final AtomicInteger customerCounter   = new AtomicInteger(1);
    private final AtomicInteger staffCounter      = new AtomicInteger(1);
    private final AtomicInteger billCounter       = new AtomicInteger(1);
    private final AtomicInteger importCounter     = new AtomicInteger(1);
    private final AtomicInteger returnCounter     = new AtomicInteger(1);
    private final AtomicInteger billReturnCounter = new AtomicInteger(1);
    private final AtomicInteger wishlistCounter   = new AtomicInteger(1);
    private final AtomicInteger cartCounter       = new AtomicInteger(1);
    private final AtomicInteger brandCounter      = new AtomicInteger(1);
    private final AtomicInteger categoryCounter   = new AtomicInteger(1);
    private final AtomicInteger materialCounter   = new AtomicInteger(1);
    private final AtomicInteger colorCounter      = new AtomicInteger(1);
    private final AtomicInteger sizeCounter       = new AtomicInteger(1);
    private final AtomicInteger supplierCounter   = new AtomicInteger(1);
    private final AtomicInteger productCounter    = new AtomicInteger(1);

    @PostConstruct
    public void init() {
        accountRepository.findAll().stream()
                .map(a -> extractNumber(a.getId(), "ACC")).max(Integer::compareTo)
                .ifPresent(max -> accountCounter.set(max + 1));
        customerRepository.findAll().stream()
                .map(c -> extractNumber(c.getId(), "KH")).max(Integer::compareTo)
                .ifPresent(max -> customerCounter.set(max + 1));
        staffRepository.findAll().stream()
                .map(s -> extractNumber(s.getId(), "NV")).max(Integer::compareTo)
                .ifPresent(max -> staffCounter.set(max + 1));
        billRepository.findAll().stream()
                .map(b -> extractLastN(b.getId(), 4)).max(Integer::compareTo)
                .ifPresent(max -> billCounter.set(max + 1));
        importOrderRepository.findAll().stream()
                .map(p -> extractLastN(p.getId(), 4)).max(Integer::compareTo)
                .ifPresent(max -> importCounter.set(max + 1));
        returnRequestRepository.findAll().stream()
                .map(r -> extractLastN(r.getId(), 4)).max(Integer::compareTo)
                .ifPresent(max -> returnCounter.set(max + 1));
        billReturnRepository.findAll().stream()
                .map(b -> extractNumber(b.getId(), "BRET")).max(Integer::compareTo)
                .ifPresent(max -> billReturnCounter.set(max + 1));
        wishlistRepository.findAll().stream()
                .map(w -> extractNumber(w.getId(), "WL")).max(Integer::compareTo)
                .ifPresent(max -> wishlistCounter.set(max + 1));
        cartRepository.findAll().stream()
                .map(c -> extractNumber(c.getId(), "CART")).max(Integer::compareTo)
                .ifPresent(max -> cartCounter.set(max + 1));
        brandRepository.findAll().stream()
                .map(b -> extractNumber(b.getId(), "BR")).max(Integer::compareTo)
                .ifPresent(max -> brandCounter.set(max + 1));
        categoryRepository.findAll().stream()
                .map(c -> extractNumber(c.getId(), "CAT")).max(Integer::compareTo)
                .ifPresent(max -> categoryCounter.set(max + 1));
        materialRepository.findAll().stream()
                .map(m -> extractNumber(m.getId(), "MAT")).max(Integer::compareTo)
                .ifPresent(max -> materialCounter.set(max + 1));
        sizeRepository.findAll().stream()
                .map(s -> extractNumber(s.getId(), "SIZE")).max(Integer::compareTo)
                .ifPresent(max -> sizeCounter.set(max + 1));
        colorRepository.findAll().stream()
                .map(c -> extractNumber(c.getId(), "COL")).max(Integer::compareTo)
                .ifPresent(max -> colorCounter.set(max + 1));
        productRepository.findAll().stream()
                .map(p -> extractNumber(p.getId(), "SP")).max(Integer::compareTo)
                .ifPresent(max -> productCounter.set(max + 1));

        log.info("IdGenerator init xong — SP={}, BR={}, KH={}, HD={}",
                productCounter.get(), brandCounter.get(),
                customerCounter.get(), billCounter.get());
    }

    // ── Người dùng ─────────────────────────────────────────
    public String generateAccountId() {
        return String.format("ACC%03d", accountCounter.getAndIncrement());
    }

    public String generateCustomerId() {
        return String.format("KH%03d", customerCounter.getAndIncrement());
    }

    public String generateStaffId() {
        return String.format("NV%03d", staffCounter.getAndIncrement());
    }

    // ── Danh mục ───────────────────────────────────────────
    public String generateBrandId() {
        return String.format("BR%03d", brandCounter.getAndIncrement());
    }

    public String generateCategoryId() {
        return String.format("CAT%03d", categoryCounter.getAndIncrement());
    }

    public String generateMaterialId() {
        return String.format("MAT%03d", materialCounter.getAndIncrement());
    }

    public String generateColorId() {
        return String.format("COL%03d", colorCounter.getAndIncrement());
    }

    public String generateSizeId() {
        return String.format("SIZE%03d", sizeCounter.getAndIncrement());
    }

    public String generateSupplierId() {
        return String.format("NCC%03d", supplierCounter.getAndIncrement());
    }

    // ── Chứng từ (có ngày) ─────────────────────────────────
    /** HD + yyyyMMdd + 4 số: HD202608180001 */
    public String generateBillId() {
        return String.format("HD%s%04d", today(), billCounter.getAndIncrement());
    }

    /** PN + yyyyMMdd + 4 số: PN202608180001 */
    public String generateImportOrderId() {
        return String.format("PN%s%04d", today(), importCounter.getAndIncrement());
    }

    /** RR + yyyyMMdd + 4 số: RR202608180001 */
    public String generateReturnRequestId() {
        return String.format("RR%s%04d", today(), returnCounter.getAndIncrement());
    }

    /** BRET + 4 số: BRET0001 */
    public String generateBillReturnId() {
        return String.format("BRET%04d", billReturnCounter.getAndIncrement());
    }

    // ── Khác ───────────────────────────────────────────────
    public String generateWishlistId() {
        return String.format("WL%03d", wishlistCounter.getAndIncrement());
    }

    public String generateCartId() {
        return String.format("CART%03d", cartCounter.getAndIncrement());
    }


    private String today() {
        return LocalDate.now().format(DATE_FMT);
    }

    // Thêm method generate product
    public String generateProductId() {
        return String.format("SP%03d", productCounter.getAndIncrement());
    }

    // Thêm 2 helper method ở cuối class
    private int extractNumber(String id, String prefix) {
        try {
            if (id != null && id.startsWith(prefix))
                return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    private int extractLastN(String id, int n) {
        try {
            if (id != null && id.length() >= n)
                return Integer.parseInt(id.substring(id.length() - n));
        } catch (NumberFormatException ignored) {}
        return 0;
    }
}