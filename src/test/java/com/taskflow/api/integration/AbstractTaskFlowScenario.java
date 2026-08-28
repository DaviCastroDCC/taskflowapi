package com.taskflow.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The end-to-end scenario, database-agnostic. Concrete subclasses pick the datastore:
 * {@link TaskFlowH2Test} runs it in-memory (always), {@link TaskFlowPostgresTest} runs it on a
 * real PostgreSQL via Testcontainers (when Docker is available).
 */
abstract class AbstractTaskFlowScenario {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Test
    void endToEndProjectLifecycle() throws Exception {
        String ownerToken = register("owner@acme.test", "Olivia Owner");
        String devToken = register("dev@acme.test", "Dev Dana");

        JsonNode me = body(exchange(get("/api/v1/me"), ownerToken, null), 200);
        assertThat(me.get("email").asText()).isEqualTo("owner@acme.test");

        JsonNode workspace = body(exchange(post("/api/v1/workspaces"), ownerToken, Map.of("name", "Acme Rockets")), 201);
        String workspaceId = workspace.get("id").asText();
        assertThat(workspace.get("callerRole").asText()).isEqualTo("OWNER");
        assertThat(workspace.get("slug").asText()).isEqualTo("acme-rockets");

        exchange(
                post("/api/v1/workspaces/" + workspaceId + "/members"),
                ownerToken,
                Map.of("email", "dev@acme.test", "role", "MEMBER"),
                201);

        JsonNode project = body(
                exchange(
                        post("/api/v1/workspaces/" + workspaceId + "/projects"),
                        ownerToken,
                        Map.of("name", "Launch Vehicle", "key", "LV")),
                201);
        String projectId = project.get("id").asText();

        JsonNode task = body(
                exchange(
                        post("/api/v1/projects/" + projectId + "/tasks"),
                        devToken,
                        Map.of("title", "Design the fuel system", "priority", "HIGH")),
                201);
        String taskId = task.get("id").asText();
        assertThat(task.get("code").asText()).isEqualTo("LV-1");
        assertThat(task.get("status").asText()).isEqualTo("BACKLOG");
        assertThat(task.get("reporter").get("fullName").asText()).isEqualTo("Dev Dana");

        JsonNode list = body(exchange(get("/api/v1/projects/" + projectId + "/tasks"), devToken, null), 200);
        assertThat(list.get("totalElements").asInt()).isEqualTo(1);
        assertThat(list.get("content").get(0).get("code").asText()).isEqualTo("LV-1");

        JsonNode moved = body(
                exchange(patch("/api/v1/tasks/" + taskId + "/status"), devToken, Map.of("status", "TODO")), 200);
        assertThat(moved.get("status").asText()).isEqualTo("TODO");

        exchange(patch("/api/v1/tasks/" + taskId + "/status"), devToken, Map.of("status", "DONE"), 422);

        exchange(
                post("/api/v1/tasks/" + taskId + "/comments"),
                devToken,
                Map.of("body", "Started on the schematics."),
                201);

        JsonNode comments = body(exchange(get("/api/v1/tasks/" + taskId + "/comments"), devToken, null), 200);
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).get("author").get("fullName").asText()).isEqualTo("Dev Dana");
    }

    @Test
    void protectedEndpointsRejectAnonymousCallers() throws Exception {
        mvc.perform(get("/api/v1/workspaces")).andExpect(status().isUnauthorized());
    }

    @Test
    void aStrangerCannotSeeAnotherUsersWorkspace() throws Exception {
        String ownerToken = register("solo@acme.test", "Solo");
        String outsiderToken = register("outsider@acme.test", "Outsider");

        String workspaceId = body(exchange(post("/api/v1/workspaces"), ownerToken, Map.of("name", "Private")), 201)
                .get("id")
                .asText();

        exchange(get("/api/v1/workspaces/" + workspaceId), outsiderToken, null, 404);
    }

    @Test
    void invalidTransitionIsReportedAsProblemDetail() throws Exception {
        String token = register("pm@acme.test", "Product Manager");
        String workspaceId = body(exchange(post("/api/v1/workspaces"), token, Map.of("name", "PM Space")), 201)
                .get("id")
                .asText();
        String projectId = body(
                        exchange(
                                post("/api/v1/workspaces/" + workspaceId + "/projects"),
                                token,
                                Map.of("name", "Alpha", "key", "AL")),
                        201)
                .get("id")
                .asText();
        String taskId = body(
                        exchange(post("/api/v1/projects/" + projectId + "/tasks"), token, Map.of("title", "Ship it")),
                        201)
                .get("id")
                .asText();

        JsonNode problem =
                body(exchange(patch("/api/v1/tasks/" + taskId + "/status"), token, Map.of("status", "IN_REVIEW")), 422);
        assertThat(problem.get("type").asText()).endsWith("/invalid-transition");
    }

    private String register(String email, String fullName) throws Exception {
        JsonNode response = body(
                exchange(
                        post("/api/v1/auth/register"),
                        null,
                        Map.of("email", email, "password", "password123", "fullName", fullName)),
                201);
        return response.get("accessToken").asText();
    }

    private MvcResult exchange(MockHttpServletRequestBuilder request, String token, Object requestBody) throws Exception {
        request.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (requestBody != null) {
            request.content(json.writeValueAsString(requestBody));
        }
        return mvc.perform(request).andReturn();
    }

    private void exchange(MockHttpServletRequestBuilder request, String token, Object requestBody, int expectedStatus)
            throws Exception {
        assertStatus(exchange(request, token, requestBody).getResponse().getStatus(), expectedStatus);
    }

    private JsonNode body(MvcResult result, int expectedStatus) throws Exception {
        assertStatus(result.getResponse().getStatus(), expectedStatus);
        return json.readTree(result.getResponse().getContentAsString());
    }

    private static void assertStatus(int actual, int expected) {
        assertThat(actual).as("HTTP status").isEqualTo(expected);
    }
}
