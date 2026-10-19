package com.histar.be.leaderboard.passportmail;

import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PassportMailService {
    public record Status(boolean available,boolean verified,String email,long cooldownSeconds,int remainingToday) {}
    public record Accepted(String status,long cooldownSeconds,int remainingToday) {}
    private record Quota(long cooldownSeconds,int remainingToday,long retryAfterSeconds) {}
    private record Reservation(UUID id,String recipient,Accepted duplicate) {}
    private final ProfileRepository profiles;
    private final PassportMailRepository attempts;
    private final HistarEmailService mail;
    private final HistarMailProperties global;
    private final TestHookProperties testHooks;
    private final HistarPassportMailProperties settings;
    private final TransactionTemplate transaction;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private Clock clock=Clock.systemUTC();
    public PassportMailService(ProfileRepository profiles,PassportMailRepository attempts,HistarEmailService mail,
            HistarMailProperties global,TestHookProperties testHooks,HistarPassportMailProperties settings,PlatformTransactionManager manager) {
        this.profiles=profiles;this.attempts=attempts;this.mail=mail;this.global=global;this.testHooks=testHooks;this.settings=settings;
        transaction=new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    void setClock(Clock clock){this.clock=Objects.requireNonNull(clock);}
    private boolean available(){return settings.isEnabled() && global.isEnabled() && !testHooks.isEnabled() && validFrom();}
    private boolean validFrom() {
        String from=settings.getFrom();
        if(from==null || from.isBlank() || from.length()>320 || from.chars().anyMatch(c->c<32 || c==127))return false;
        try {var addresses=jakarta.mail.internet.InternetAddress.parse(from,true);if(addresses.length!=1 || addresses[0].isGroup())return false;addresses[0].validate();return true;}
        catch(jakarta.mail.internet.AddressException ex){return false;}
    }
    public Status status(UUID userId) {
        var profile=profile(userId,false);
        var q=quota(userId,clock.instant());
        return new Status(available(),Boolean.TRUE.equals(profile.getEmailVerified()),profile.getEmail(),q.cooldownSeconds(),q.remainingToday());
    }
    public Accepted send(UUID userId,MultipartFile image,UUID requestId) {
        if(userId==null) throw unauthorized();
        if(!available()) throw new PassportMailException(503,"MAIL_UNAVAILABLE","Tính năng gửi hộ chiếu qua email hiện không khả dụng.");
        if(requestId==null) throw new PassportMailException(422,"INVALID_REQUEST_ID","Yêu cầu gửi cần mã định danh hợp lệ.");
        // Authorize before spending time decoding. Recipient is rechecked/captured under the reservation lock.
        verified(profile(userId,false));
        byte[] png=new PassportPngNormalizer().normalize(image);
        String hash=sha256(png);
        Reservation reservation=transaction.execute(tx -> {
            Profile profile=profile(userId,true);
            // OpenEntityManagerInView/authentication may have cached an earlier profile. Refresh under lock.
            if(entityManager!=null) entityManager.refresh(profile,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            verified(profile);
            Instant now=clock.instant();
            var previous=attempts.findByUserIdAndRequestId(userId,requestId);
            Quota q=quota(userId,now);
            if(previous.isPresent()) {
                var attempt=previous.get();
                if(!hash.equals(attempt.getImageSha256())) throw new PassportMailException(409,"REQUEST_ID_REUSED","Mã yêu cầu này đã được dùng cho một ảnh khác.");
                if("ACCEPTED".equals(attempt.getState())) return new Reservation(null,null,new Accepted("ACCEPTED",q.cooldownSeconds(),q.remainingToday()));
                if("CREATED".equals(attempt.getState())) throw new PassportMailException(409,"REQUEST_IN_PROGRESS","Yêu cầu này đang được xử lý và sẽ không được gửi lại.");
                throw unknown();
            }
            if(q.cooldownSeconds()>0 || q.remainingToday()==0) throw new PassportMailException(429,"MAIL_RATE_LIMITED","Vui lòng chờ trước khi gửi thêm hộ chiếu qua email.",q.retryAfterSeconds());
            var attempt=PassportMailAttempt.created(UUID.randomUUID(),userId,requestId,hash,now);
            attempts.saveAndFlush(attempt);
            return new Reservation(attempt.getId(),profile.getEmail(),null);
        });
        if(reservation.duplicate()!=null) return reservation.duplicate();
        // Reservation is committed before transport starts. Never hold the profile lock during network IO.
        try {
            mail.sendAttachment(settings.getFrom(),reservation.recipient(),"Hộ chiếu di sản TimeLens của bạn",
                    "<p>Ảnh hộ chiếu di sản TimeLens của bạn được đính kèm trong thư này.</p><p>Đây là ảnh lưu niệm do bạn tạo, không phải chứng nhận thành tích.</p><p>Thời điểm gửi (UTC): "+clock.instant()+".</p><p>Chúc bạn có thêm nhiều hành trình khám phá!</p>","TimeLens-passport.png",png);
        } catch(RuntimeException ex) {
            try { finish(reservation.id(),"UNKNOWN"); } catch(RuntimeException ignored) { /* Remains CREATED: still never resend. */ }
            throw unknown();
        }
        try { finish(reservation.id(),"ACCEPTED"); } catch(RuntimeException ignored) { throw unknown(); }
        var q=quota(userId,clock.instant());
        return new Accepted("ACCEPTED",q.cooldownSeconds(),q.remainingToday());
    }
    private void finish(UUID id,String state) {
        transaction.executeWithoutResult(tx -> attempts.findById(id).ifPresent(attempt -> {
            attempt.finish(state);attempts.saveAndFlush(attempt);
        }));
    }
    private Profile profile(UUID userId,boolean lock) {
        if(userId==null) throw unauthorized();
        return (lock?profiles.findByIdForUpdate(userId):profiles.findById(userId)).orElseThrow(this::unauthorized);
    }
    private void verified(Profile profile) {
        if(!Boolean.TRUE.equals(profile.getEmailVerified()) || profile.getEmail()==null || profile.getEmail().isBlank())
            throw new PassportMailException(403,"EMAIL_NOT_VERIFIED","Vui lòng xác minh email tài khoản trước khi gửi hộ chiếu.");
    }
    private Quota quota(UUID userId,Instant now) {
        var recent=attempts.findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(userId,now.minus(Duration.ofHours(24)));
        long cooldown=recent.isEmpty()?0:secondsUntil(now,recent.get(recent.size()-1).getCreatedAt().plusSeconds(60));
        int remaining=Math.max(0,3-recent.size());
        long daily=remaining==0?secondsUntil(now,recent.get(0).getCreatedAt().plus(Duration.ofHours(24))):0;
        return new Quota(cooldown,remaining,Math.max(cooldown,daily));
    }
    private long secondsUntil(Instant now,Instant end){long millis=Duration.between(now,end).toMillis();return millis<=0?0:(millis+999)/1000;}
    private PassportMailException unauthorized(){return new PassportMailException(401,"AUTH_REQUIRED","Vui lòng đăng nhập để gửi hộ chiếu qua email.");}
    private PassportMailException unknown(){return new PassportMailException(502,"MAIL_DELIVERY_UNKNOWN","Dịch vụ email chưa xác nhận tiếp nhận. Yêu cầu này sẽ không được gửi lại. Hãy kiểm tra hộp thư và Spam trước khi tạo yêu cầu mới.");}
    private String sha256(byte[] image) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 unavailable",ex);}
    }
}


