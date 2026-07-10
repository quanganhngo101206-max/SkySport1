package com.example.skysport1.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiter đơn giản, in-memory, sliding-window theo key (thường là IP).
 * Không cần thay đổi DB schema.
 *
 * Lưu ý: đây là biện pháp giảm thiểu (mitigation), không thay thế cho việc
 * đổi sang token ký/ngẫu nhiên cho từng đơn guest — nhưng vì DB schema hiện
 * do bên ngoài quản lý (ddl-auto=none, không có migration đi kèm trong repo),
 * nên chưa thể thêm cột token mà không có sự phối hợp thay đổi schema.
 */
@Component
public class SimpleRateLimiter {

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /**
     * @return true nếu request được phép (chưa vượt giới hạn), false nếu bị chặn
     */
    public boolean allow(String key, int maxAttempts, long windowSeconds) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(windowStart)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxAttempts) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}