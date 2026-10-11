package com.dongly.modules.booking;

import com.dongly.modules.booking.dto.HoldSeatsRequest;
import com.dongly.modules.booking.dto.HoldSeatsResponse;
import com.dongly.modules.booking.entity.BookingItem;
import com.dongly.modules.booking.entity.BookingItemStatus;
import com.dongly.modules.booking.entity.Customer;
import com.dongly.modules.booking.repository.BookingItemRepository;
import com.dongly.modules.booking.repository.BookingRepository;
import com.dongly.modules.booking.repository.CustomerRepository;
import com.dongly.modules.booking.service.CrmBookingTransactionService;
import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.DriverStatus;
import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.SeatType;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import com.dongly.modules.fleet.repository.DriverRepository;
import com.dongly.modules.fleet.repository.VehicleRepository;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Location;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.repository.LocationRepository;
import com.dongly.modules.route.repository.RouteRepository;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CrmBookingConcurrencyIntegrationTest {

    @Autowired
    private CrmBookingTransactionService transactionService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleSeatRepository vehicleSeatRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Trip testTrip;
    private VehicleSeat targetSeat;

    @BeforeEach
    void setUp() {
        Location loc1 = locationRepository.save(Location.builder()
                .code("LOC_TEST_1_" + UUID.randomUUID().toString().substring(0, 5))
                .name("Hà Nội")
                .province("Hà Nội")
                .status(CommonStatus.ACTIVE)
                .build());

        Location loc2 = locationRepository.save(Location.builder()
                .code("LOC_TEST_2_" + UUID.randomUUID().toString().substring(0, 5))
                .name("Thanh Hóa")
                .province("Thanh Hóa")
                .status(CommonStatus.ACTIVE)
                .build());

        Route route = routeRepository.save(Route.builder()
                .code("RT_" + UUID.randomUUID().toString().substring(0, 5))
                .name("Hà Nội - Thanh Hóa")
                .originLocation(loc1)
                .destinationLocation(loc2)
                .status(CommonStatus.ACTIVE)
                .build());

        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .plateNumber("36B-" + UUID.randomUUID().toString().substring(0, 6))
                .vehicleType(VehicleType.LIMOUSINE)
                .brand("Thaco")
                .status(VehicleStatus.ACTIVE)
                .totalFloors(1)
                .totalRows(5)
                .totalColumns(3)
                .totalSeats(10)
                .build());

        targetSeat = vehicleSeatRepository.save(VehicleSeat.builder()
                .vehicle(vehicle)
                .seatCode("HOT_01")
                .floor(1)
                .rowIndex(1)
                .columnIndex(1)
                .seatType(SeatType.VIP)
                .extraPrice(new BigDecimal("50000.00"))
                .status(SeatStatus.ACTIVE)
                .build());

        Driver driver1 = driverRepository.save(Driver.builder()
                .code("TX1_" + UUID.randomUUID().toString().substring(0, 5))
                .fullName("Tài xế 1")
                .phone("0912" + UUID.randomUUID().toString().substring(0, 6).replaceAll("\\D", "0"))
                .licenseNumber("GPLX1_" + UUID.randomUUID().toString().substring(0, 5))
                .licenseClass("E")
                .status(DriverStatus.ACTIVE)
                .build());

        Driver driver2 = driverRepository.save(Driver.builder()
                .code("TX2_" + UUID.randomUUID().toString().substring(0, 5))
                .fullName("Tài xế 2")
                .phone("0913" + UUID.randomUUID().toString().substring(0, 6).replaceAll("\\D", "0"))
                .licenseNumber("GPLX2_" + UUID.randomUUID().toString().substring(0, 5))
                .licenseClass("E")
                .status(DriverStatus.ACTIVE)
                .build());

        testTrip = tripRepository.save(Trip.builder()
                .code("TRP_CONCUR_" + UUID.randomUUID().toString().substring(0, 5))
                .route(route)
                .vehicle(vehicle)
                .driver(driver1)
                .assistantDriver(driver2)
                .departureTime(OffsetDateTime.now().plusDays(2))
                .estimatedArrivalTime(OffsetDateTime.now().plusDays(2).plusHours(3))
                .basePrice(new BigDecimal("250000.00"))
                .status(TripStatus.READY)
                .build());
    }

    @Test
    @DisplayName("Kiểm thử đồng thời (Race Condition): 10 luồng cùng tranh chấp giữ đúng 1 ghế -> Đúng 1 luồng thành công, 9 luồng bị chặn")
    void holdSeats_ConcurrentRaceCondition_ExactlyOneSucceeds() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> failureMessages = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Chờ tín hiệu cùng xuất phát tại một mili-giây
                    startLatch.await();

                    CurrentUser staff = new CurrentUser(
                            UUID.randomUUID(), "staff_" + index, "staff" + index + "@dongly.vn",
                            Set.of("STAFF"), Set.of("BOOKING_MANAGE")
                    );

                    HoldSeatsRequest request = HoldSeatsRequest.builder()
                            .tripId(testTrip.getId())
                            .seatIds(List.of(targetSeat.getId()))
                            .customerPhone("091234567" + (index % 10))
                            .customerName("Khách " + index)
                            .build();

                    transactionService.holdSeats(request, staff);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    failureMessages.add(e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Bắn tín hiệu cho 10 luồng cùng chạy đồng thời
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // KẾT QUẢ NGHIỆM THU:
        // Đúng 1 luồng thành công
        assertThat(successCount.get()).isEqualTo(1);
        // 9 luồng còn lại phải thất bại an toàn
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);

        // Kiểm tra trong database: Chỉ duy nhất 1 booking_item được tạo cho ghế này
        List<BookingItem> activeItems = bookingItemRepository.findActiveItemsByTrip(testTrip.getId(), OffsetDateTime.now());
        assertThat(activeItems).hasSize(1);
        assertThat(activeItems.get(0).getSeat().getId()).isEqualTo(targetSeat.getId());
        assertThat(activeItems.get(0).getStatus()).isEqualTo(BookingItemStatus.HELD);
    }
}
