package com.dongly.modules.route;

import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.route.dto.CreateRouteRequest;
import com.dongly.modules.route.dto.CreateStopPointRequest;
import com.dongly.modules.route.dto.RouteStopInputDto;
import com.dongly.modules.route.dto.UpdateRouteRequest;
import com.dongly.modules.route.dto.UpdateRouteStopsRequest;
import com.dongly.modules.route.dto.UpdateStatusRequest;
import com.dongly.modules.route.dto.UpdateStopPointRequest;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Location;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.entity.RouteDirectionType;
import com.dongly.modules.route.entity.RouteStopType;
import com.dongly.modules.route.entity.StopPoint;
import com.dongly.modules.route.repository.LocationRepository;
import com.dongly.modules.route.repository.RouteRepository;
import com.dongly.modules.route.repository.RouteStopRepository;
import com.dongly.modules.route.repository.StopPointRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RouteIntegrationTest {

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
    private LocationRepository locationRepository;

    @Autowired
    private StopPointRepository stopPointRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String readOnlyToken;
    private Location locThanhHoa;
    private Location locHaNoi;

    @BeforeEach
    void setUp() {
        routeStopRepository.deleteAll();
        routeRepository.deleteAll();
        stopPointRepository.deleteAll();
        locationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Tạo Permissions
        Permission permRead = permissionRepository.save(Permission.builder()
                .code("ROUTE_READ")
                .name("Xem tuyến đường")
                .description("Quyền xem danh sách tuyến đường và điểm dừng")
                .module("ROUTE")
                .action("READ")
                .build());

        Permission permManage = permissionRepository.save(Permission.builder()
                .code("ROUTE_MANAGE")
                .name("Quản lý tuyến đường")
                .description("Quyền quản lý cấu hình tuyến và điểm dừng")
                .module("ROUTE")
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
                .username("admin_route")
                .email("admin_route@dongly.vn")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Quản Trị Tuyến")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(adminRole)))
                .build());

        User staffUser = userRepository.save(User.builder()
                .username("staff_route")
                .email("staff_route@dongly.vn")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Nhân Viên Tuyến")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(staffRole)))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(),
                Set.of("ROLE_ADMIN"), Set.of("ROUTE_READ", "ROUTE_MANAGE")
        );

        readOnlyToken = jwtTokenProvider.generateAccessToken(
                staffUser.getId(), staffUser.getUsername(), staffUser.getEmail(),
                Set.of("ROLE_STAFF"), Set.of("ROUTE_READ")
        );

        // 4. Tạo Locations
        locThanhHoa = locationRepository.save(Location.builder()
                .code("THANH_HOA")
                .name("Thanh Hóa")
                .province("Thanh Hóa")
                .status(CommonStatus.ACTIVE)
                .build());

        locHaNoi = locationRepository.save(Location.builder()
                .code("HA_NOI")
                .name("Hà Nội")
                .province("Hà Nội")
                .status(CommonStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("TC-01: Lấy danh sách địa danh hoạt động thành công")
    void getActiveLocations_Success() throws Exception {
        mockMvc.perform(get("/api/v1/locations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("TC-02: Tạo điểm đón/trả thành công với quyền ROUTE_MANAGE")
    void createStopPoint_Success() throws Exception {
        CreateStopPointRequest request = CreateStopPointRequest.builder()
                .code("BX_NUOC_NGAM")
                .name("Bến xe Nước Ngầm")
                .locationId(locHaNoi.getId())
                .address("Số 01 Ngọc Hồi, Hà Nội")
                .latitude(new BigDecimal("20.9765123"))
                .longitude(new BigDecimal("105.8456123"))
                .contactPhone("0243.861.2345")
                .build();

        mockMvc.perform(post("/api/v1/stop-points")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("BX_NUOC_NGAM")))
                .andExpect(jsonPath("$.data.name", is("Bến xe Nước Ngầm")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("TC-03: Tạo điểm đón/trả bị từ chối nếu không có quyền ROUTE_MANAGE (403 Forbidden)")
    void createStopPoint_Forbidden_WithoutPermission() throws Exception {
        CreateStopPointRequest request = CreateStopPointRequest.builder()
                .code("BX_NUOC_NGAM")
                .name("Bến xe Nước Ngầm")
                .locationId(locHaNoi.getId())
                .address("Số 01 Ngọc Hồi, Hà Nội")
                .build();

        mockMvc.perform(post("/api/v1/stop-points")
                        .header("Authorization", "Bearer " + readOnlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-04: Tạo điểm đón/trả thất bại nếu mã code đã tồn tại (409 Conflict)")
    void createStopPoint_DuplicateCode_ThrowsConflict() throws Exception {
        stopPointRepository.save(StopPoint.builder()
                .code("BX_NUOC_NGAM")
                .name("Bến xe Nước Ngầm Cũ")
                .location(locHaNoi)
                .address("Hà Nội")
                .status(CommonStatus.ACTIVE)
                .build());

        CreateStopPointRequest request = CreateStopPointRequest.builder()
                .code("bx_nuoc_ngam")
                .name("Bến xe Nước Ngầm Mới")
                .locationId(locHaNoi.getId())
                .address("Hà Nội mới")
                .build();

        mockMvc.perform(post("/api/v1/stop-points")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESOURCE_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("TC-05: Cập nhật thông tin và trạng thái điểm đón/trả thành công")
    void updateStopPointAndStatus_Success() throws Exception {
        StopPoint sp = stopPointRepository.save(StopPoint.builder()
                .code("BX_PHIASONG")
                .name("Bến xe Phía Nam")
                .location(locThanhHoa)
                .address("TP Thanh Hóa")
                .status(CommonStatus.ACTIVE)
                .build());

        UpdateStopPointRequest updateReq = UpdateStopPointRequest.builder()
                .code("BX_PHIANAM_TH")
                .name("Bến xe Phía Nam Thanh Hóa")
                .locationId(locThanhHoa.getId())
                .address("Quốc lộ 1A, TP Thanh Hóa")
                .contactPhone("0237.123.4567")
                .build();

        mockMvc.perform(put("/api/v1/stop-points/" + sp.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code", is("BX_PHIANAM_TH")));

        UpdateStatusRequest statusReq = UpdateStatusRequest.builder()
                .status(CommonStatus.INACTIVE)
                .build();

        mockMvc.perform(patch("/api/v1/stop-points/" + sp.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("TC-06: Tạo tuyến đường thành công kèm cấu hình điểm dừng hợp lệ")
    void createRouteWithStops_Success() throws Exception {
        StopPoint sp1 = stopPointRepository.save(StopPoint.builder()
                .code("BIGC_TH")
                .name("Big C Thanh Hóa")
                .location(locThanhHoa)
                .address("Nguyễn Hoàng, Thanh Hóa")
                .status(CommonStatus.ACTIVE)
                .build());

        StopPoint sp2 = stopPointRepository.save(StopPoint.builder()
                .code("BX_NUOC_NGAM")
                .name("Bến xe Nước Ngầm")
                .location(locHaNoi)
                .address("Ngọc Hồi, Hà Nội")
                .status(CommonStatus.ACTIVE)
                .build());

        List<RouteStopInputDto> stops = List.of(
                RouteStopInputDto.builder()
                        .stopPointId(sp1.getId())
                        .direction(RouteDirectionType.OUTBOUND)
                        .sequence(1)
                        .stopType(RouteStopType.PICKUP)
                        .extraPrice(BigDecimal.ZERO)
                        .build(),
                RouteStopInputDto.builder()
                        .stopPointId(sp2.getId())
                        .direction(RouteDirectionType.OUTBOUND)
                        .sequence(2)
                        .stopType(RouteStopType.DROPOFF)
                        .extraPrice(BigDecimal.ZERO)
                        .build()
        );

        CreateRouteRequest request = CreateRouteRequest.builder()
                .code("TH_HN")
                .name("Thanh Hóa - Hà Nội")
                .originLocationId(locThanhHoa.getId())
                .destinationLocationId(locHaNoi.getId())
                .distanceKm(new BigDecimal("160.50"))
                .estimatedDurationMinutes(180)
                .description("Tuyến cao tốc Thanh Hóa - Hà Nội")
                .stops(stops)
                .build();

        mockMvc.perform(post("/api/v1/routes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("TH_HN")))
                .andExpect(jsonPath("$.data.stops", hasSize(2)))
                .andExpect(jsonPath("$.data.stops[0].stopPointCode", is("BIGC_TH")))
                .andExpect(jsonPath("$.data.stops[1].stopPointCode", is("BX_NUOC_NGAM")));
    }

    @Test
    @DisplayName("TC-07: Tạo tuyến đường thất bại khi điểm đầu trùng điểm cuối (400 Bad Request)")
    void createRoute_SameOriginAndDestination_ThrowsValidationError() throws Exception {
        CreateRouteRequest request = CreateRouteRequest.builder()
                .code("TH_TH")
                .name("Nội thành Thanh Hóa")
                .originLocationId(locThanhHoa.getId())
                .destinationLocationId(locThanhHoa.getId())
                .build();

        mockMvc.perform(post("/api/v1/routes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("TC-08: Cập nhật danh sách điểm dừng vi phạm quy tắc trạm đầu chỉ được DROPOFF (422 Business Rule)")
    void updateRouteStops_FirstStopDropoff_ThrowsBusinessRuleException() throws Exception {
        Route route = routeRepository.save(Route.builder()
                .code("TH_HN_EXP")
                .name("Thanh Hóa - Hà Nội Express")
                .originLocation(locThanhHoa)
                .destinationLocation(locHaNoi)
                .status(CommonStatus.ACTIVE)
                .build());

        StopPoint sp1 = stopPointRepository.save(StopPoint.builder()
                .code("SP_1")
                .name("Trạm 1")
                .location(locThanhHoa)
                .address("Địa chỉ 1")
                .status(CommonStatus.ACTIVE)
                .build());

        UpdateRouteStopsRequest request = UpdateRouteStopsRequest.builder()
                .stops(List.of(
                        RouteStopInputDto.builder()
                                .stopPointId(sp1.getId())
                                .direction(RouteDirectionType.OUTBOUND)
                                .sequence(1)
                                .stopType(RouteStopType.DROPOFF) // Vi phạm: trạm đầu không thể chỉ cho trả khách
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/routes/" + route.getId() + "/stops")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("TC-09: Tìm kiếm tuyến đường và phân trang chính xác")
    void searchRoutes_WithFilters_Success() throws Exception {
        routeRepository.save(Route.builder()
                .code("TH_HN")
                .name("Thanh Hóa - Hà Nội")
                .originLocation(locThanhHoa)
                .destinationLocation(locHaNoi)
                .status(CommonStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/routes")
                        .param("keyword", "Thanh Hóa")
                        .param("originLocationId", locThanhHoa.getId().toString())
                        .header("Authorization", "Bearer " + readOnlyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.pagination.totalElements", is(1)));
    }
}
