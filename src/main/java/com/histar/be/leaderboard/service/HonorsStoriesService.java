package com.histar.be.leaderboard.service;

import com.histar.be.config.GamificationProperties;
import com.histar.be.discovery.explore.ExploreChallengeCatalog;
import com.histar.be.leaderboard.dto.HonorsStoriesResponse;
import com.histar.be.leaderboard.dto.HonorsStoriesResponse.*;
import com.histar.be.leaderboard.repository.HonorsStoriesRepository;
import com.histar.be.leaderboard.service.impl.LeaderboardServiceImpl;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class HonorsStoriesService {
 private final LeaderboardServiceImpl access;
 private final HonorsStoriesRepository evidence;
 private final GamificationProperties gamification;
 private record Route(String id,String title,String description,String prefix,List<String> targets) {}
 private static final List<Route> ROUTES=List.of(
  new Route("depths","Bốn tầng Củ Chi","Hoàn thành câu hỏi ở bốn tầng địa đạo.","depth_quiz:",List.of("surface","layer1","layer2","layer3")),
  new Route("ciphers","Giải mã lịch sử","Giải đúng ba mật mã của Củ Chi.","cipher:",List.of("hoang-cam","two-wells","sandals")),
  new Route("evidence","Hồ sơ chứng cứ","Ghi nhận bốn chứng cứ và hoàn thành kết luận.","clue:",List.of("ev-01","ev-02","ev-03","ev-04","conclusion")),
  new Route("dossiers","Sáu hồ sơ Củ Chi","Hoàn thành câu hỏi của sáu hồ sơ.","dossier:",List.of("architecture","trap-warfare","underground-life","command-center","tunnel-rats","ben-duoc")),
  new Route("videos","Bảy thước phim Củ Chi","Xem ít nhất 90% và trả lời đúng câu hỏi của bảy video.","video:",com.histar.be.discovery.explore.video.ExploreVideoCatalog.IDS));
 private static String key(Route route,String target){return target.equals("conclusion")?ExploreChallengeCatalog.CONCLUSION:route.prefix()+target;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public HonorsStoriesResponse read(String scope,String city,UUID viewer,UUID group) {
  var validated=access.resolveAccess(scope,city,viewer,group);
  var dates=evidence.completionDates(viewer);
  var keys=dates.keySet();
  var paths=ROUTES.stream().map(r->{
   int completed=(int)r.targets().stream().filter(t->keys.contains(key(r,t))).count();
   String next=r.targets().stream().filter(t->!keys.contains(key(r,t))).findFirst().orElse(null);
   return new Path(r.id(),r.title(),r.description(),completed,r.targets().size(),completed*100/r.targets().size(),
    "/explore?honorsPath="+r.id()+(next==null?"":"&target="+next),next==null?"Đã hoàn thành":"Tiếp tục khám phá",next==null);
  }).toList();
  var timeline=new ArrayList<>(evidence.timeline(viewer).stream().map(t->{
   if(t.icon().equals("badge"))return new Timeline(t.id(),t.title(),t.description(),t.occurredAt(),"/profile","military_tech");
   if(!t.icon().equals("discovery"))return t;
   String discoveryKey=t.id().substring("discovery:".length());
   for(var r:ROUTES)for(var target:r.targets())if(key(r,target).equals(discoveryKey))
    return new Timeline(t.id(),r.title()+" · "+label(target),t.description(),t.occurredAt(),"/explore?honorsPath="+r.id()+"&target="+target,icon(r.id()));
   return t;
  }).toList());
  for (var route : ROUTES) {
   if (!route.targets().stream().allMatch(target -> keys.contains(key(route, target)))) continue;
   var completedDates = route.targets().stream().map(target -> dates.get(key(route, target))).toList();
   Instant completedAt = completedDates.stream().anyMatch(Objects::isNull) ? null
    : completedDates.stream().max(Comparator.naturalOrder()).orElse(null);
   timeline.add(new Timeline("path:" + route.id(), "Trọn vẹn dấu ấn · " + route.title(),
    "Bạn đã hoàn thành toàn bộ " + route.targets().size() + " thử thách trong hành trình này.",
    completedAt, "/explore?honorsPath=" + route.id(), "flag"));
  }
  int points=Math.max(0,Optional.ofNullable(validated.profile().getTotalPoints()).orElse(0));
  for(int threshold:gamification.parseLevelThresholds())if(threshold>0&&points>=threshold)
   timeline.add(new Timeline("xp:"+threshold,"Cột mốc "+threshold+" XP","Tổng XP hiện tại xác nhận cột mốc; chưa có ngày đạt được xác thực.",null,"/profile","auto_awesome"));
  timeline.sort(Comparator.comparing(Timeline::occurredAt,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Timeline::id));
  var metrics=evidence.metrics(viewer,validated.city(),validated.orgId(),group);
  var spots=new ArrayList<Spotlight>();
  String[][] categories={{"ciphers","Người giải mã di sản","Số mật mã Củ Chi đã giải đúng trên máy chủ (tối đa 3).","mật mã"},
   {"locations","Người kết nối dấu tích","Số địa điểm tồn tại có ít nhất một khám phá được ghi nhận.","địa điểm"},
   {"challenge-sets","Người chinh phục cột mốc","Số bộ đã hoàn thành: 4 tầng, 3 mật mã, 4 chứng cứ và kết luận, 6 hồ sơ, hoặc 7 video (tối đa 5).","bộ thử thách"},
   {"badges","Người sưu tầm dấu ấn","Số huy hiệu khác nhau đã được trao trong hành trình.","huy hiệu"}};
  for(var category:categories){
   var rows=metrics.stream().filter(m->m.category().equals(category[0])).toList();
   var me=rows.stream().filter(m->m.userId().equals(viewer)).findFirst().orElse(null);
   var leaders=rows.stream().filter(m->m.ordinal()<=3).map(m->new Leader(m.userId(),m.displayName(),m.avatarUrl(),m.count(),m.userId().equals(viewer))).toList();
   spots.add(new Spotlight(category[0],category[1],category[2],category[3],leaders,rows.isEmpty()?0:rows.get(0).achievers(),me==null?0:me.count(),me==null?null:me.rank()));
  }
  return new HonorsStoriesResponse(viewer,timeline.stream().limit(60).toList(),paths,spots,Instant.now());
 }
 private static String icon(String id){return switch(id){case "depths"->"layers";case "ciphers"->"key";case "evidence"->"verified";case "dossiers"->"auto_stories";case "videos"->"play_circle";default->"explore";};}
 private static String label(String target){return switch(target){
  case "surface"->"Mặt đất";case "layer1"->"Tầng 1";case "layer2"->"Tầng 2";case "layer3"->"Tầng 3";
  case "hoang-cam"->"Bếp Hoàng Cầm";case "two-wells"->"Hai giếng";case "sandals"->"Đôi dép";
  case "ev-01"->"Chứng cứ 1";case "ev-02"->"Chứng cứ 2";case "ev-03"->"Chứng cứ 3";case "ev-04"->"Chứng cứ 4";case "conclusion"->"Kết luận";
  case "architecture"->"Kiến trúc";case "trap-warfare"->"Chiến thuật bẫy";case "underground-life"->"Đời sống dưới lòng đất";
  case "tunnel-digging"->"Cách đào địa đạo";case "liberation-gate"->"Cổng làng vùng giải phóng";case "communication-station"->"Trạm thông tin";case "guerrilla-house"->"Nhà du kích";case "women-house"->"Nhà phụ nữ";case "war-artifacts"->"Hiện vật chiến tranh";case "ho-chi-minh-space"->"Không gian văn hóa Hồ Chí Minh";
  case "command-center"->"Sở chỉ huy";case "tunnel-rats"->"Đối đầu trong địa đạo";case "ben-duoc"->"Bến Dược";default->"Khám phá";};}
}

