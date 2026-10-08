# ==========================================
# GIAI ĐOẠN 1: Biên dịch mã nguồn (Build Stage)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Sao chép các tệp Maven wrapper và pom.xml trước để tận dụng Docker cache layer
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/

RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Sao chép mã nguồn và thực hiện đóng gói
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Giải nén các layers từ file JAR đã build
RUN java -Djarmode=layertools -jar target/*.jar extract

# ==========================================
# GIAI ĐOẠN 2: Môi trường chạy thực tế (Runtime Stage)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Tạo tài khoản và nhóm người dùng phi đặc quyền (non-root)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Sao chép các layer của Spring Boot
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# Phân quyền thư mục làm việc cho non-root user
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Tối ưu hóa JVM trong môi trường container
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
