package com.dongly.modules.fleet;

import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.fleet.dto.ConfigureSeatLayoutRequest;
import com.dongly.modules.fleet.dto.CreateDriverRequest;
import com.dongly.modules.fleet.dto.CreateVehicleRequest;
import com.dongly.modules.fleet.dto.SeatItemDto;
import com.dongly.modules.fleet.dto.UpdateDriverRequest;
import com.dongly.modules.fleet.dto.UpdateDriverStatusRequest;
import com.dongly.modules.fleet.dto.UpdateSeatStatusRequest;
import com.dongly.modules.fleet.dto.UpdateVehicleRequest;
import com.dongly.modules.fleet.dto.UpdateVehicleStatusRequest;
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
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.repository.PermissionRepository;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.modules.user.repository.UserRepository;
import com.dongly.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FleetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleSeatRepository seatRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String readOnlyToken;
    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        vehicleRepository.deleteAll();
        driverRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Tạo Permissions
        Permission permRead = permissionRepository.save(Permission.builder()
                .code("FLEET_READ")
                .name("Xem đội xe & tài xế")
                .description("Quyền xem phương tiện, sơ đồ ghế và danh sách tài xế")
                .module("FLEET")
                .action("READ")
                .build());

        Permission permManage = permissionRepository.save(Permission.builder()
                .code("FLEET_MANAGE")
                .name("Quản lý đội xe & tài xế")
                .description("Quyền quản lý phương tiện, cấu hình ghế và tài xế")
                .module("FLEET")
                .action("MANAGE")
                .build());

        // 2. Tạo Roles
        Role adminRole = roleRepository.save(Role.builder()
                .code("ADMIN")
                .name("Quản trị viên")
                .description("Toàn quyền")
                .permissions(new HashSet<>(List.of(permRead, permManage)))
                .build());

        Role staffRole = roleRepository.save(Role.builder()
                .code("STAFF")
                .name("Nhân viên")
                .description("Chỉ đọc")
                .permissions(new HashSet<>(List.of(permRead)))
                .build());

        // 3. Tạo Users
        User adminUser = userRepository.save(User.builder()
                .username("admin_fleet")
                .email("admin_fleet@dongly.vn")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Quản Trị Đội Xe")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(adminRole)))
                .build());

        User staffUser = userRepository.save(User.builder()
                .username("staff_fleet")
                .email("staff_fleet@dongly.vn")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Nhân Viên Đội Xe")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(staffRole)))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(),
                Set.of("ROLE_ADMIN"), Set.of("FLEET_READ", "FLEET_MANAGE")
        );

        readOnlyToken = jwtTokenProvider.generateAccessToken(
                staffUser.getId(), staffUser.getUsername(), staffUser.getEmail(),
                Set.of("ROLE_STAFF"), Set.of("FLEET_READ")
        );

        // 4. Tạo sample vehicle
        sampleVehicle = vehicleRepository.save(Vehicle.builder()
                .plateNumber("36B-028.68")
                .vehicleType(VehicleType.LIMOUSINE)
                .brand("Thaco Mobihome")
                .model("VIP 22 Phòng")
                .manufactureYear(2024)
                .totalFloors(2)
                .totalRows(6)
                .totalColumns(3)
                .totalSeats(2)
                .status(VehicleStatus.ACTIVE)
                .createdBy("admin_fleet")
                .updatedBy("admin_fleet")
                .build());

        // Thêm 2 ghế
        seatRepository.save(VehicleSeat.builder()
                .vehicle(sampleVehicle)
                .seatCode("A01")
                .floor(1)
                .rowIndex(1)
                .columnIndex(1)
                .seatType(SeatType.LUXURY_ROOM)
                .extraPrice(BigDecimal.valueOf(50000))
                .status(SeatStatus.ACTIVE)
                .build());

        seatRepository.save(VehicleSeat.builder()
                .vehicle(sampleVehicle)
                .seatCode("A02")
                .floor(1)
                .rowIndex(1)
                .columnIndex(3)
                .seatType(SeatType.LUXURY_ROOM)
                .extraPrice(BigDecimal.valueOf(50000))
                .status(SeatStatus.ACTIVE)
                .build());
    }

    // ==================== VEHICLE TESTS ====================

    @Test
    @DisplayName("Tìm kiếm phương tiện - Phân trang thành công")
    void testSearchVehicles_Success() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].plateNumber", is("36B-028.68")));
    }

    @Test
    @DisplayName("Tạo phương tiện mới - Thành công")
    void testCreateVehicle_Success() throws Exception {
        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .plateNumber("36B-999.99")
                .vehicleType(VehicleType.SLEEPER)
                .brand("Hyundai")
                .model("Universe")
                .manufactureYear(2023)
                .totalFloors(2)
                .totalRows(6)
                .totalColumns(3)
                .description("Xe mới bổ sung")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.vehicle.plateNumber", is("36B-999.99")));
    }

    @Test
    @DisplayName("Tạo phương tiện - Trùng biển số ném lỗi 409")
    void testCreateVehicle_DuplicatePlate_ThrowsConflict() throws Exception {
        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .plateNumber("36B-028.68")
                .vehicleType(VehicleType.SLEEPER)
                .brand("Hyundai")
                .manufactureYear(2023)
                .totalFloors(2)
                .totalRows(6)
                .totalColumns(3)
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESOURCE_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Tạo phương tiện - Không đủ quyền FLEET_MANAGE bị cấm 403")
    void testCreateVehicle_Forbidden() throws Exception {
        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .plateNumber("36B-888.88")
                .vehicleType(VehicleType.SEATER)
                .brand("Ford")
                .totalFloors(1)
                .totalRows(4)
                .totalColumns(4)
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + readOnlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cập nhật trạng thái xe - Thành công")
    void testUpdateVehicleStatus_Success() throws Exception {
        UpdateVehicleStatusRequest request = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE);

        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", sampleVehicle.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicle.status", is("MAINTENANCE")));
    }

    // ==================== SEAT LAYOUT TESTS ====================

    @Test
    @DisplayName("Lấy sơ đồ ghế - Thành công")
    void testGetSeatLayout_Success() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/{id}/seats", sampleVehicle.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.seats", hasSize(2)))
                .andExpect(jsonPath("$.data.seats[0].seatCode", is("A01")));
    }

    @Test
    @DisplayName("Cấu hình lại sơ đồ ghế - Thành công")
    void testConfigureSeatLayout_Success() throws Exception {
        ConfigureSeatLayoutRequest request = ConfigureSeatLayoutRequest.builder()
                .totalFloors(1)
                .totalRows(2)
                .totalColumns(2)
                .seats(List.of(
                        SeatItemDto.builder()
                                .seatCode("G01")
                                .floor(1)
                                .rowIndex(1)
                                .columnIndex(1)
                                .seatType(SeatType.VIP)
                                .extraPrice(BigDecimal.valueOf(20000))
                                .status(SeatStatus.ACTIVE)
                                .build(),
                        SeatItemDto.builder()
                                .seatCode("G02")
                                .floor(1)
                                .rowIndex(1)
                                .columnIndex(2)
                                .seatType(SeatType.VIP)
                                .extraPrice(BigDecimal.valueOf(20000))
                                .status(SeatStatus.ACTIVE)
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/vehicles/{id}/seats", sampleVehicle.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSeats", is(2)))
                .andExpect(jsonPath("$.data.seats", hasSize(2)))
                .andExpect(jsonPath("$.data.seats[0].seatCode", is("G01")));
    }

    @Test
    @DisplayName("Cấu hình sơ đồ ghế - Trùng lặp tọa độ ném lỗi 422")
    void testConfigureSeatLayout_DuplicateCoords_ThrowsError() throws Exception {
        ConfigureSeatLayoutRequest request = ConfigureSeatLayoutRequest.builder()
                .totalFloors(1)
                .totalRows(2)
                .totalColumns(2)
                .seats(List.of(
                        SeatItemDto.builder()
                                .seatCode("G01")
                                .floor(1)
                                .rowIndex(1)
                                .columnIndex(1)
                                .seatType(SeatType.VIP)
                                .build(),
                        SeatItemDto.builder()
                                .seatCode("G02")
                                .floor(1)
                                .rowIndex(1)
                                .columnIndex(1) // Trùng tầng 1, hàng 1, cột 1
                                .seatType(SeatType.VIP)
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/vehicles/{id}/seats", sampleVehicle.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ==================== DRIVER TESTS ====================

    @Test
    @DisplayName("Tạo hồ sơ tài xế - Thành công")
    void testCreateDriver_Success() throws Exception {
        CreateDriverRequest request = CreateDriverRequest.builder()
                .code("TX-999")
                .fullName("Đỗ Văn Hưng")
                .phone("0981122334")
                .licenseNumber("010999888777")
                .licenseClass("E")
                .licenseExpiryDate(LocalDate.of(2030, 1, 1))
                .dateOfBirth(LocalDate.of(1987, 5, 10))
                .note("Tài xế chạy tuyến Hà Nội")
                .build();

        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("TX-999")))
                .andExpect(jsonPath("$.data.phone", is("0981122334")));
    }

    @Test
    @DisplayName("Tạo hồ sơ tài xế - Trùng SĐT ném lỗi 409")
    void testCreateDriver_DuplicatePhone_ThrowsConflict() throws Exception {
        driverRepository.save(Driver.builder()
                .code("TX-100")
                .fullName("Trần A")
                .phone("0981122334")
                .licenseNumber("010111222333")
                .licenseClass("E")
                .status(DriverStatus.ACTIVE)
                .build());

        CreateDriverRequest request = CreateDriverRequest.builder()
                .code("TX-101")
                .fullName("Trần B")
                .phone("0981122334") // Trùng SĐT
                .licenseNumber("010444555666")
                .licenseClass("E")
                .build();

        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESOURCE_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Cập nhật trạng thái tài xế - Thành công")
    void testUpdateDriverStatus_Success() throws Exception {
        Driver driver = driverRepository.save(Driver.builder()
                .code("TX-200")
                .fullName("Nguyễn C")
                .phone("0912987654")
                .licenseNumber("010777888999")
                .licenseClass("E")
                .status(DriverStatus.ACTIVE)
                .build());

        UpdateDriverStatusRequest request = new UpdateDriverStatusRequest(DriverStatus.ON_LEAVE);

        mockMvc.perform(patch("/api/v1/drivers/{id}/status", driver.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ON_LEAVE")));
    }

    @Test
    @DisplayName("Tạo hồ sơ tài xế - Trùng GPLX ném lỗi 409")
    void testCreateDriver_DuplicateLicense_ThrowsConflict() throws Exception {
        driverRepository.save(Driver.builder()
                .code("TX-300")
                .fullName("Tài Xế A")
                .phone("0911000111")
                .licenseNumber("010888999000")
                .licenseClass("E")
                .status(DriverStatus.ACTIVE)
                .build());

        CreateDriverRequest request = CreateDriverRequest.builder()
                .code("TX-301")
                .fullName("Tài Xế B")
                .phone("0911000222")
                .licenseNumber("010888999000") // Trùng GPLX
                .licenseClass("E")
                .build();

        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESOURCE_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Cấu hình sơ đồ ghế - Trùng lặp mã ghế trong cùng xe ném lỗi 422")
    void testConfigureSeatLayout_DuplicateSeatCode_ThrowsError() throws Exception {
        ConfigureSeatLayoutRequest request = ConfigureSeatLayoutRequest.builder()
                .totalFloors(1)
                .totalRows(2)
                .totalColumns(2)
                .seats(List.of(
                        SeatItemDto.builder()
                                .seatCode("A01")
                                .floor(1)
                                .rowIndex(1)
                                .columnIndex(1)
                                .seatType(SeatType.STANDARD)
                                .build(),
                        SeatItemDto.builder()
                                .seatCode("A01") // Trùng mã ghế A01 ở tọa độ khác
                                .floor(1)
                                .rowIndex(1)
                                .columnIndex(2)
                                .seatType(SeatType.STANDARD)
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/vehicles/{id}/seats", sampleVehicle.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Xóa phương tiện - Cascade xóa toàn bộ ghế của phương tiện")
    void testDeleteVehicle_CascadesSeatsSuccessfully() throws Exception {
        UUID vehicleId = sampleVehicle.getId();

        mockMvc.perform(delete("/api/v1/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Xác thực phương tiện và ghế liên kết đã được dọn sạch
        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        // Đảm bảo ghế của xe đó không còn tồn tại
        long remainingSeats = seatRepository.countByVehicleId(vehicleId);
        org.junit.jupiter.api.Assertions.assertEquals(0, remainingSeats);
    }
}
