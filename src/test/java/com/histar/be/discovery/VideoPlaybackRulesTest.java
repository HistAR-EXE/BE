package com.histar.be.discovery;
import com.histar.be.discovery.explore.video.VideoPlaybackRules;
import com.histar.be.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VideoPlaybackRulesTest {
 @Test void sequentialProgressIsBoundedByServerClockAndHeartbeatWindow() {
  assertEquals(10,VideoPlaybackRules.advance(5,10,5,100));
  assertThrows(BusinessRuleException.class,()->VideoPlaybackRules.advance(5,40,5,100));
  assertThrows(BusinessRuleException.class,()->VideoPlaybackRules.advance(5,25,60,100));
  assertEquals(10,VideoPlaybackRules.advance(10,2,5,100));
 }
 @Test void rejectsNonFiniteAndInvalidPositions() {
  for(double position:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,101})
   assertThrows(BusinessRuleException.class,()->VideoPlaybackRules.advance(0,position,5,100));
 }
 @Test void quizRequiresNinetyPercent() {
  assertFalse(VideoPlaybackRules.ready(89.9,100));
  assertTrue(VideoPlaybackRules.ready(90,100));
 }
}
