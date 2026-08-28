package com.taskflow.api.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Runs the full scenario in-memory. Always executes; no Docker required. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class TaskFlowH2Test extends AbstractTaskFlowScenario {
}
