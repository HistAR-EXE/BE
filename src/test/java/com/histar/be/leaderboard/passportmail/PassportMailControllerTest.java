package com.histar.be.leaderboard.passportmail;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class PassportMailControllerTest {
 @Test void statusUsesAuthenticatedIdAndIsNeverCached() throws Exception {
  var service=mock(PassportMailService.class);var accessor=mock(CurrentUserAccessor.class);var id=UUID.randomUUID();
  when(accessor.getUserId()).thenReturn(Optional.of(id));
  when(service.status(id)).thenReturn(new PassportMailService.Status(true,true,"owner@example.com",0,3));
  MockMvcBuilders.standaloneSetup(new PassportMailController(service,accessor)).build()
   .perform(get("/api/leaderboard/passport/email/status")).andExpect(status().isOk())
   .andExpect(header().string("Cache-Control","private, no-store"))
   .andExpect(jsonPath("$.data.email").value("owner@example.com"));
 }
 @Test void unauthorizedHasStructuredFailureAndNoCache() throws Exception {
  var accessor=mock(CurrentUserAccessor.class);when(accessor.getUserId()).thenReturn(Optional.empty());
  MockMvcBuilders.standaloneSetup(new PassportMailController(mock(PassportMailService.class),accessor)).build()
   .perform(get("/api/leaderboard/passport/email/status")).andExpect(status().isUnauthorized())
   .andExpect(header().string("Cache-Control","private, no-store"))
   .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
 }
 @Test void refusesClientRecipientAndMalformedId() throws Exception {
  var accessor=mock(CurrentUserAccessor.class);when(accessor.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
  var service=mock(PassportMailService.class);var mvc=MockMvcBuilders.standaloneSetup(new PassportMailController(service,accessor)).build();
  var file=new org.springframework.mock.web.MockMultipartFile("image","x.png","image/png",new byte[]{1});
  mvc.perform(multipart("/api/leaderboard/passport/email").file(file).param("requestId",UUID.randomUUID().toString()).param("to","victim@example.com"))
   .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  mvc.perform(multipart("/api/leaderboard/passport/email").file(file).param("requestId","not-a-uuid"))
   .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_REQUEST_ID"));
  verifyNoInteractions(service);
 }}

