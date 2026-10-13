package be.npl.shelfwise.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Created by the tests that use MockMvc. Calls the REST API like the frontend does: log in once per account, then send the bearer token. */
@RequiredArgsConstructor
public class ApiClient {

    private final MockMvc mvc;
    private final ObjectMapper json;

    public String login(String username) throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", "shelfwise"))))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    public MockHttpServletRequestBuilder as(String username, MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + login(username));
    }

    public MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    public JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    public MvcResult perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn();
    }
}
