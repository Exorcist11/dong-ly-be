package com.dongly.modules.booking.scheduler;

import com.dongly.modules.booking.service.CrmBookingTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled Worker: Định kỳ quét và giải phóng các đơn giữ chỗ đã hết hạn 10 phút.
 * Tần suất: 30 giây một lần.
 */
@Slf4j
@Component
public class ExpiredSeatReleaseScheduler {

    private final CrmBookingTransactionService bookingTransactionService;

    public ExpiredSeatReleaseScheduler(CrmBookingTransactionService bookingTransactionService) {
        this.bookingTransactionService = bookingTransactionService;
    }

    @Scheduled(fixedRate = 30000)
    public void scheduleReleaseExpiredHolds() {
        try {
            bookingTransactionService.releaseExpiredHolds();
        } catch (Exception e) {
            log.error("Lỗi khi chạy tiến trình giải phóng ghế hết hạn: {}", e.getMessage(), e);
        }
    }
}
