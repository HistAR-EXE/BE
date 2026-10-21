package com.histar.be.ar;

import com.histar.be.config.*;
import com.histar.be.mail.EmailSender;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;

@Service
public class ArCardMailService {
    public record Status(boolean available, boolean verified, String email, long cooldownSeconds, int remainingToday, String unavailableReason) {}
    public record Accepted(String status, String artifactId, long cooldownSeconds, int remainingToday) {}
    private record Quota(long cooldown, int remaining, long retry) {}
    private record Reservation(UUID id, String email, Accepted previous) {}
    private record Card(String id, String name, String photo) {}
    private final ProfileRepository profiles;
    private final ArCardMailRepository attempts;
    private final EmailSender sender;
    private final HistarMailProperties mail;
    private final TestHookProperties hooks;
    private final HistarAppProperties app;
    private final TransactionTemplate transaction;
    private final List<Card> cards;
    @Value("${histar.ar.card-mail-enabled:${HISTAR_AR_CARD_MAIL_ENABLED:true}}")
    private boolean enabled=true;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private Clock clock=Clock.systemUTC();

    public ArCardMailService(ProfileRepository profiles, ArCardMailRepository attempts, @org.springframework.beans.factory.annotation.Qualifier("emailSender") EmailSender sender,
            HistarMailProperties mail, TestHookProperties hooks, HistarAppProperties app, PlatformTransactionManager manager) {
        this.profiles=profiles;this.attempts=attempts;this.sender=sender;this.mail=mail;this.hooks=hooks;this.app=app;
        transaction=new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try (var reader=new BufferedReader(new InputStreamReader(new ClassPathResource("ar/photo-cards.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            cards=reader.lines().filter(line->!line.isBlank()).map(line->{var fields=line.split("\t");
                if(fields.length!=3 || !fields[0].matches("[a-z0-9-]+") || !fields[2].matches("DSCF[0-9]+\\.JPG")) throw new IllegalStateException("Invalid AR card manifest");
                return new Card(fields[0],fields[1],fields[2]);}).toList();
        } catch(IOException error) { throw new IllegalStateException("AR card manifest unavailable",error); }
        if(cards.size()!=10 || cards.stream().map(Card::id).distinct().count()!=10 || cards.stream().map(Card::photo).distinct().count()!=10) throw new IllegalStateException("Expected ten distinct AR cards");
    }
    private String origin() {
        try {
            var uri=URI.create(app.getFrontendUrl());var host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme()) || host==null || !host.contains(".") || host.matches("[0-9.]+") || host.endsWith(".localhost") || host.endsWith(".local") || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) return null;
            return uri.toString().replaceAll("/$", "");
        } catch(RuntimeException error) { return null; }
    }
    private String unavailableReason() {
        if(!enabled) return "FEATURE_DISABLED";
        if(!mail.isEnabled()) return "MAIL_DISABLED";
        if(hooks.isEnabled()) return "TEST_MODE";
        if(origin()==null) return "PUBLIC_URL_REQUIRED";
        return null;
    }
    private boolean available() { return unavailableReason()==null; }
    private Profile profile(UUID userId, boolean lock) {
        if(userId==null) throw new ArCardMailException(401,"AUTH_REQUIRED","Vui lòng đăng nhập để nhận ảnh AR.");
        return (lock ? profiles.findByIdForUpdate(userId) : profiles.findById(userId)).orElseThrow(()->new ArCardMailException(401,"AUTH_REQUIRED","Không tìm thấy tài khoản."));
    }
    private Quota quota(UUID userId) {
        var now=clock.instant();var rows=attempts.findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(userId,now.minus(Duration.ofHours(24)));
        long cooldown=rows.isEmpty()?0:seconds(now,rows.get(rows.size()-1).getCreatedAt().plusSeconds(60));
        long daily=rows.size()<3?0:seconds(now,rows.get(0).getCreatedAt().plus(Duration.ofHours(24)));
        return new Quota(cooldown,Math.max(0,3-rows.size()),Math.max(cooldown,daily));
    }
    private long seconds(Instant now, Instant end) { return Math.max(0,(Duration.between(now,end).toMillis()+999)/1000); }
    public Status status(UUID userId) {
        var profile=profile(userId,false);var q=quota(userId);
        return new Status(available(),Boolean.TRUE.equals(profile.getEmailVerified()),profile.getEmail(),q.cooldown(),q.remaining(),unavailableReason());
    }
    public Accepted send(UUID userId, String artifactId, UUID requestId) {
        var card=cards.stream().filter(item->Objects.equals(item.id(),artifactId)).findFirst().orElseThrow(()->new ArCardMailException(422,"INVALID_ARTIFACT","Vui lòng chọn một hiện vật AR hợp lệ."));
        if(requestId==null) throw new ArCardMailException(422,"INVALID_REQUEST","Thiếu mã yêu cầu gửi.");
        if(!available()) throw new ArCardMailException(503,"MAIL_UNAVAILABLE","Gửi ảnh AR qua email chưa sẵn sàng. Bạn vẫn có thể tải hoặc in ảnh.");
        var reservation=transaction.execute(tx->{
            var profile=profile(userId,true);
            if(entityManager!=null) entityManager.refresh(profile,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if(!Boolean.TRUE.equals(profile.getEmailVerified()) || profile.getEmail()==null || profile.getEmail().isBlank()) throw new ArCardMailException(403,"EMAIL_NOT_VERIFIED","Vui lòng xác minh email tài khoản.");
            var old=attempts.findByUserIdAndRequestId(userId,requestId);var q=quota(userId);
            if(old.isPresent()) {
                if(!Objects.equals(old.get().getArtifactId(),card.id())) throw new ArCardMailException(409,"REQUEST_ID_REUSED","Mã gửi đã được dùng cho nội dung khác. Vui lòng tạo yêu cầu mới.");
                if("ACCEPTED".equals(old.get().getState())) return new Reservation(null,null,new Accepted("ACCEPTED",card.id(),q.cooldown(),q.remaining()));
                if("CREATED".equals(old.get().getState())) throw new ArCardMailException(409,"REQUEST_IN_PROGRESS","Yêu cầu đang được xử lý; hệ thống không gửi trùng thư.");
                throw unknown();
            }
            if(q.retry()>0 || q.remaining()==0) throw new ArCardMailException(429,"MAIL_RATE_LIMITED","Giới hạn 3 lần trong 24 giờ và cách nhau 60 giây.",q.retry());
            var attempt=attempts.save(ArCardMailAttempt.created(userId,requestId,card.id(),clock.instant()));
            return new Reservation(attempt.getId(),profile.getEmail(),null);
        });
        if(reservation.previous()!=null) return reservation.previous();
        try {
            sender.sendHtmlBounded(reservation.email(),"TimeLens · Ảnh AR " + card.name(),html(card));
            finish(reservation.id(),"ACCEPTED");
        } catch(RuntimeException error) {
            try { finish(reservation.id(),"UNKNOWN"); } catch(RuntimeException ignored) { /* A reserved request still prevents duplicate delivery. */ }
            throw unknown();
        }
        var q=quota(userId);return new Accepted("ACCEPTED",card.id(),q.cooldown(),q.remaining());
    }
    private void finish(UUID id, String state) { transaction.executeWithoutResult(tx->attempts.findById(id).ifPresent(attempt->{attempt.finish(state);attempts.save(attempt);})); }
    private ArCardMailException unknown() { return new ArCardMailException(502,"MAIL_DELIVERY_UNKNOWN","Chưa xác nhận được dịch vụ email tiếp nhận. Hãy kiểm tra Hộp thư đến và Spam; yêu cầu này sẽ không tự gửi lại."); }
    private String html(Card card) {
        var base=HtmlUtils.htmlEscape(origin());
        var name=HtmlUtils.htmlEscape(card.name());
        var photo=base+"/assets/ar-targets/"+card.photo().replace(".JPG",".jpg");
        var camera=base+"/ar-experience?artifact="+card.id()+"&amp;view=camera";
        var target=base+"/ar-experience?artifact="+card.id()+"&amp;view=target";
        return "<!doctype html><html lang=\"vi\"><meta charset=\"utf-8\"><body style=\"font-family:Arial,sans-serif;color:#0f1b33;line-height:1.7\">"
            +"<h1>Ảnh nhận diện AR · "+name+"</h1><p>Bạn đã chọn hiện vật <strong>"+name+"</strong>. Email này chỉ chứa ảnh nhận diện của hiện vật đó.</p>"
            +"<img src=\""+photo+"\" alt=\"Ảnh nhận diện "+name+"\" width=\"640\" style=\"display:block;width:100%;max-width:640px;height:auto\">"
            +"<p>"+card.photo()+" · Ảnh màu thực địa do nhóm chụp tại Củ Chi.</p>"
            +"<p><a href=\""+photo+"\">Mở ảnh màu / Tải ảnh</a> · <a href=\""+target+"\">Mở thẻ lớn / In ảnh</a></p>"
            +"<p>Nếu email chưa hiển thị ảnh, chọn Mở ảnh màu. Giữ đầy đủ khung hình và in màu khi dùng bản in.</p>"
            +"<p><a href=\""+camera+"\">Mở camera AR cho "+name+"</a></p>"
            +"<ol><li>Mở ảnh này trên laptop, màn hình khác hoặc in màu.</li><li>Trên điện thoại mở camera AR bằng liên kết trên, chọn Bật camera AR và cấp quyền.</li><li>Quét toàn bộ ảnh màu này để hiện mô hình "+name+". QR chỉ mở trang; ảnh màu dùng để nhận diện.</li></ol>"
            +"<p>Nếu đang đọc email trên điện thoại, hãy mở ảnh trên một màn hình khác hoặc in ra. Webcam laptop cần ảnh in hoặc ảnh mở trên điện thoại đưa trước camera.</p>"
            +"<p>Bạn nhận thư vì đã yêu cầu ảnh này trên TimeLens. Mô hình minh họa phục vụ khám phá, không xác nhận vật thể cụ thể trong ảnh là nguồn của mô hình 3D.</p></body></html>";
    }
}
