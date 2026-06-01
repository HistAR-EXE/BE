# TimeLens — Spring Boot Starter (Ngày 1–3 cho SE1)

> Mục tiêu: SE1 copy-paste chạy thẳng. Java 17, Spring Boot 3.2+, PostgreSQL, JWT.
> **Sửa lại:** data model có **16 bảng** (mình từng nói nhầm 15). Bảng thứ 16 là `campaigns` (độc lập, không FK).

---

## 0. Lưu ý thiết kế

- Entities dùng **UUID FK dạng field** (vd. `private UUID locationId;`) thay vì `@ManyToOne`. Lý do: tránh `LazyInitializationException` + vòng lặp JSON khi serialize — ít footgun, build nhanh hơn cho MVP 4 tuần. Có thể refactor sang `@ManyToOne` sau.
- `profiles` gộp luôn auth (email + password_hash + role) — một bảng user duy nhất cho gọn.
- Schema để Hibernate tự tạo (`ddl-auto: update`) HOẶC chạy file `TimeLens_DB_Schema.sql` rồi để `ddl-auto: validate`. Khuyên: chạy SQL file (có sẵn seed Củ Chi) rồi `validate`.

---

## 1. NGÀY 1 — Tạo project

**Spring Initializr** (start.spring.io) chọn: Gradle, Java 17, Spring Boot 3.2+, dependencies:
`Spring Web`, `Spring Data JPA`, `Spring Security`, `PostgreSQL Driver`, `Lombok`, `Validation`.

**Thêm JWT vào `build.gradle`:**
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-webflux' // cho WebClient (AI chat, ngày 5)
    runtimeOnly   'org.postgresql:postgresql'
    compileOnly   'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly   'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly   'io.jsonwebtoken:jjwt-jackson:0.12.5'
}
```

**`src/main/resources/application.yml`:**
```yaml
spring:
  datasource:
    url: ${DB_URL}            # vd: jdbc:postgresql://...:5432/postgres
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate      # 'update' nếu để Hibernate tự tạo; 'validate' nếu đã chạy schema.sql
    show-sql: true
    properties:
      hibernate.format_sql: true

jwt:
  secret: ${JWT_SECRET}       # PHẢI >= 32 ký tự (256-bit cho HS256)
  expiration: 86400000        # 24h (ms)

gemini:
  api-key: ${GEMINI_API_KEY}  # dùng ngày 5

cors:
  allowed-origins: http://localhost:5173,https://your-frontend.vercel.app
```

Deploy skeleton lên **Railway/Render** (tạo Postgres + set biến môi trường DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET).

---

## 2. NGÀY 2 — Entities + Repositories

> Quy ước: Spring tự map camelCase → snake_case (`passwordHash` → `password_hash`). Đặt các file trong package `com.histar.timelens.entity`.

```java
// Profile.java
@Entity @Table(name = "profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false) private String email;
    private String passwordHash;
    private String provider;   // local / google
    private String role;       // USER / ADMIN
    private String displayName;
    private String avatarUrl;
    private Integer level;
    private Integer totalPoints;
    private String city;
    private Instant createdAt;
}

// Location.java
@Entity @Table(name = "locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Location {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private String name;
    @Column(columnDefinition = "text") private String description;
    private Double latitude;
    private Double longitude;
    private String city;
    @Column(columnDefinition = "text") private String coverImage;
    private Instant createdAt;
}

// Character.java  (tên class CharacterEntity để tránh trùng java.lang.Character)
@Entity @Table(name = "characters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CharacterEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID locationId;
    private String name;
    private String era;
    @Column(columnDefinition = "text") private String personaPrompt;
    @Column(columnDefinition = "text") private String portraitUrl;
}

// PhotoPair.java
@Entity @Table(name = "photo_pairs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhotoPair {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID locationId;
    @Column(columnDefinition = "text") private String historicalImage;
    @Column(columnDefinition = "text") private String currentImage;
    private Integer year;
    private String caption;
    private Integer sortOrder;
}

// Panorama.java
@Entity @Table(name = "panoramas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Panorama {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID locationId;
    @Column(columnDefinition = "text") private String imageUrl;
    private String title;
}

// Hotspot.java
@Entity @Table(name = "hotspots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hotspot {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID panoramaId;
    private Double yaw;
    private Double pitch;
    private String type;
    private String contentRef;
    private String label;
}

// Quest.java
@Entity @Table(name = "quests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID locationId;
    private String title;
    @Column(columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String story;
    private Integer pointsReward;
    private Integer requiredOrder;
}

// UserQuestProgress.java
@Entity @Table(name = "user_quest_progress")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserQuestProgress {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID userId;
    private UUID questId;
    private String status;   // not_started / in_progress / completed
    private Instant startedAt;
    private Instant completedAt;
}

// Badge.java
@Entity @Table(name = "badges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Badge {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private String name;
    @Column(columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String iconUrl;
    private String conditionType;
    private Integer conditionValue;
}

// UserBadge.java
@Entity @Table(name = "user_badges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserBadge {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID userId;
    private UUID badgeId;
    private Instant earnedAt;
}

// Conversation.java
@Entity @Table(name = "conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID userId;
    private UUID characterId;
    private Instant createdAt;
}

// Message.java
@Entity @Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID conversationId;
    private String role;     // user / assistant
    @Column(columnDefinition = "text") private String content;
    private Instant createdAt;
}

// Checkin.java
@Entity @Table(name = "checkins")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Checkin {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID userId;
    private UUID locationId;
    private Double latitude;
    private Double longitude;
    private Instant createdAt;
}

// PhotoFrame.java
@Entity @Table(name = "photo_frames")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhotoFrame {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private String name;
    @Column(columnDefinition = "text") private String imageUrl;
    private String era;
    private Integer sortOrder;
}

// UserCreation.java
@Entity @Table(name = "user_creations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserCreation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID userId;
    private UUID frameId;
    @Column(columnDefinition = "text") private String outputUrl;
    private Instant createdAt;
}

// Campaign.java  ← BẢNG THỨ 16
@Entity @Table(name = "campaigns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Campaign {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private String name;
    private Instant startDate;
    private Instant endDate;
    private Integer bonusPoints;
}
```

**Repositories** (package `com.histar.timelens.repository`) — chỉ cần extend `JpaRepository`:
```java
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByEmail(String email);
}
public interface LocationRepository extends JpaRepository<Location, UUID> {}
public interface CharacterRepository extends JpaRepository<CharacterEntity, UUID> {
    List<CharacterEntity> findByLocationId(UUID locationId);
}
public interface PhotoPairRepository extends JpaRepository<PhotoPair, UUID> {
    List<PhotoPair> findByLocationIdOrderBySortOrder(UUID locationId);
}
public interface QuestRepository extends JpaRepository<Quest, UUID> {
    List<Quest> findByLocationId(UUID locationId);
}
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {}
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdOrderByCreatedAt(UUID conversationId);
}
// Tương tự cho: Panorama, Hotspot, UserQuestProgress, Badge, UserBadge, Checkin, PhotoFrame, UserCreation, Campaign
```

---

## 3. NGÀY 3 — Spring Security + JWT

Package `com.histar.timelens.security` và `...auth`.

```java
// JwtService.java
@Service
public class JwtService {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration}") private long expiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String generateToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key())
            .compact();
    }
    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload().getSubject();
    }
    public boolean isValid(String token) {
        try { Jwts.parser().verifyWith(key()).build().parseSignedClaims(token); return true; }
        catch (Exception e) { return false; }
    }
}
```

```java
// CustomUserDetailsService.java
@Service @RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final ProfileRepository profileRepository;
    @Override
    public UserDetails loadUserByUsername(String email) {
        Profile p = profileRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Not found: " + email));
        return User.builder()
            .username(p.getEmail())
            .password(p.getPasswordHash() == null ? "" : p.getPasswordHash())
            .authorities("ROLE_" + (p.getRole() == null ? "USER" : p.getRole()))
            .build();
    }
}
```

```java
// JwtAuthenticationFilter.java
@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService uds;
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                String email = jwtService.extractEmail(token);
                UserDetails ud = uds.loadUserByUsername(email);
                var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
```

```java
// SecurityConfig.java
@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService uds;
    @Value("${cors.allowed-origins}") private String allowedOrigins;

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
          .csrf(c -> c.disable())
          .cors(Customizer.withDefaults())
          .authorizeHttpRequests(a -> a
              .requestMatchers("/api/auth/**", "/api/locations/**", "/api/characters/**").permitAll()
              .anyRequest().authenticated())
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authenticationProvider(authProvider())
          .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean public AuthenticationProvider authProvider() {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public AuthenticationManager authManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
    @Bean public CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
```

```java
// DTOs (package ...auth) — dùng record cho gọn
public record RegisterRequest(@Email String email, @NotBlank String password, String displayName) {}
public record LoginRequest(@Email String email, @NotBlank String password) {}
public record AuthResponse(String token, UUID userId, String displayName) {}
```

```java
// AuthService.java
@Service @RequiredArgsConstructor
public class AuthService {
    private final ProfileRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authManager;

    public AuthResponse register(RegisterRequest r) {
        if (repo.findByEmail(r.email()).isPresent())
            throw new RuntimeException("Email đã được đăng ký");
        Profile p = Profile.builder()
            .email(r.email())
            .passwordHash(encoder.encode(r.password()))
            .displayName(r.displayName())
            .provider("local").role("USER").level(1).totalPoints(0)
            .createdAt(Instant.now())
            .build();
        repo.save(p);
        return new AuthResponse(jwt.generateToken(p.getEmail()), p.getId(), p.getDisplayName());
    }
    public AuthResponse login(LoginRequest r) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(r.email(), r.password()));
        Profile p = repo.findByEmail(r.email()).orElseThrow();
        return new AuthResponse(jwt.generateToken(p.getEmail()), p.getId(), p.getDisplayName());
    }
}
```

```java
// AuthController.java
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    @PostMapping("/register") public AuthResponse register(@RequestBody @Valid RegisterRequest r) { return auth.register(r); }
    @PostMapping("/login")    public AuthResponse login(@RequestBody @Valid LoginRequest r)    { return auth.login(r); }
}
```

---

## 4. NGÀY 4 — Controllers cơ bản (test API)

```java
@RestController @RequestMapping("/api/locations") @RequiredArgsConstructor
public class LocationController {
    private final LocationRepository repo;
    @GetMapping public List<Location> all() { return repo.findAll(); }
    @GetMapping("/{id}") public Location one(@PathVariable UUID id) { return repo.findById(id).orElseThrow(); }
}

@RestController @RequestMapping("/api/characters") @RequiredArgsConstructor
public class CharacterController {
    private final CharacterRepository repo;
    @GetMapping("/by-location/{locationId}")
    public List<CharacterEntity> byLocation(@PathVariable UUID locationId) {
        return repo.findByLocationId(locationId);
    }
}
```

---

## 5. Test nhanh (curl)

```bash
# Đăng ký
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@fpt.edu.vn","password":"123456","displayName":"Quan"}'

# Đăng nhập → lấy token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@fpt.edu.vn","password":"123456"}'

# Gọi API có bảo vệ (thay <TOKEN>)
curl http://localhost:8080/api/locations \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Gotchas (đọc kỹ kẻo mất thời gian)

- `jwt.secret` phải **≥ 32 ký tự**, nếu không jjwt báo lỗi WeakKey với HS256.
- Class entity của `characters` đặt tên **`CharacterEntity`** để không trùng `java.lang.Character`.
- `@Table(name="...")` dùng số nhiều khớp tên bảng SQL.
- Nếu chạy `TimeLens_DB_Schema.sql` rồi thì để `ddl-auto: validate`; nếu để Hibernate tạo bảng thì `update` (nhưng khi đó seed data phải insert riêng).
- CORS: nhớ đổi `cors.allowed-origins` sang domain frontend thật khi deploy.
- Đừng commit `.env` / secret lên Git.
