package com.histar.be.ar;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class ArCardMailControllerTest {
    @Test void authenticatedStatusAndAcceptedRequestAreNotCached() throws Exception {
        var service=mock(ArCardMailService.class);var user=mock(CurrentUserAccessor.class);var id=UUID.randomUUID();var key=UUID.randomUUID();
        when(user.getUserId()).thenReturn(Optional.of(id));
        when(service.status(id)).thenReturn(new ArCardMailService.Status(false,true,"owner@example.invalid",0,3,"MAIL_DISABLED"));
        when(service.send(id,"hat",key)).thenReturn(new ArCardMailService.Accepted("ACCEPTED","hat",60,2));
        var mvc=MockMvcBuilders.standaloneSetup(new ArCardMailController(service,user)).build();
        mvc.perform(get("/api/ar/cards/email/status")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","private, no-store")).andExpect(jsonPath("$.data.unavailableReason").value("MAIL_DISABLED"));
        mvc.perform(post("/api/ar/cards/email").contentType("application/json").content("{\"requestId\":\""+key+"\",\"artifactId\":\"hat\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.artifactId").value("hat"));
        verify(service).send(id,"hat",key);
    }
    @Test void guestClientChosenRecipientAndMissingArtifactAreRejected() throws Exception {
        var service=mock(ArCardMailService.class);var user=mock(CurrentUserAccessor.class);
        var mvc=MockMvcBuilders.standaloneSetup(new ArCardMailController(service,user)).build();
        mvc.perform(get("/api/ar/cards/email/status")).andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control","private, no-store"));
        when(user.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        for(var content:List.of("{\"requestId\":\""+UUID.randomUUID()+"\",\"artifactId\":\"hat\",\"to\":\"other@example.invalid\"}","{\"requestId\":\"invalid\",\"artifactId\":\"hat\"}","{\"requestId\":\""+UUID.randomUUID()+"\"}","{\"requestId\":\""+UUID.randomUUID()+"\",\"artifactId\":42}"))
            mvc.perform(post("/api/ar/cards/email").contentType("application/json").content(content)).andExpect(status().isUnprocessableContent());
        verifyNoInteractions(service);
    }
}
