package com.experian.aperture.datastudio.sdk.step.examples.testframework;

import com.experian.aperture.datastudio.sdk.testframework.StepTestBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class StepComponentTest {
    private static final String AUTH_KEY_PROPERTY = "EmailValidation.authKey";
    private static final String AUTH_KEY_VALUE = "key";
    private static final String COLOR_ID_COLUMN = "Color Id";
    private static final Map<String, ColorData> MOCK_COLORS = createColors();
    private static ClientAndServer mockServer;
    private static String csvInput = "out/test/resources/InputData.csv";

    @BeforeClass
    public static void setUp() throws URISyntaxException {
        mockServer = startClientAndServer(1080);
        csvInput = new File(StepComponentTest.class.getResource("/InputData.csv").toURI())
                .getAbsolutePath();
    }

    /**
     * Validates the step execution returns expected column and values against a mock server
     */
    @Test
    public void stepShouldExecuteSuccessfully() throws JsonProcessingException {
        this.setupMockServer("4"); // setup mock server for resource on rows 1 in csv.
        final int colorNameColumnIndex = 3;
        final int additionalAttributeColumnIndex = 4;

        StepTestBuilder.fromCustomStep(new RestServiceSampleStep())
                .withCsvInput(csvInput)
                .withServerProperty(AUTH_KEY_PROPERTY, AUTH_KEY_VALUE)
                .withStepPropertyValue(0, COLOR_ID_COLUMN)
                .withStepPropertyValue(1, RestServiceSampleStep.ATTRIBUTE_YEAR)
                .withConstantValue(RestServiceSampleStep.COLOR_SERVICE_URL_KEY, "http://localhost:1080/")
                .isInteractive(true)
                .build()
                .execute()
                .assertColumnValueAt(1, colorNameColumnIndex, "aqua sky")
                .assertColumnValueAt(1, additionalAttributeColumnIndex, "2003")
                .waitForAssertion();
    }

    @AfterClass
    public static void tearDown() {
        mockServer.stop(true);
    }

    @SuppressWarnings("squid:S2095") // Should not be close until the test tear down.
    private void setupMockServer(final String requestId) throws JsonProcessingException {
        new MockServerClient("localhost", 1080)
                .when(request()
                        .withMethod("GET")
                        .withPath(String.format("/api/unknown/%s", requestId)))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(toJsonString(new MockResponse(MOCK_COLORS.get(requestId)))));
    }

    private static String toJsonString(final MockResponse response) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(response);
    }

    private static Map<String, ColorData> createColors() {
        final Map<String, ColorData> colors = new HashMap<>();
        colors.put("1", new ColorData("1", "cerulean", "2000", "#98B2D1", "15-4020"));
        colors.put("2", new ColorData("2", "fuchsia rose", "2001", "#C74375", "17-2031"));
        colors.put("3", new ColorData("3", "true red", "2002", "#BF1932", "19-1664"));
        colors.put("4", new ColorData("4", "aqua sky", "2003", "#7BC4C4", "14-4811"));
        colors.put("5", new ColorData("5", "tigerlily", "2004", "#E2583E", "17-1456"));
        return colors;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static final class MockResponse {
        @JsonProperty("data")
        private final ColorData data;

        @JsonCreator
        MockResponse(@JsonProperty("data") final ColorData data) {
            this.data = data;
        }
    }

    @JsonInclude
    private static final class ColorData {
        @JsonProperty("id")
        private final String id;

        @JsonProperty("name")
        private final String name;

        @JsonProperty("year")
        private final String year;

        @JsonProperty("color")
        private final String color;

        @JsonProperty("pantone_value")
        private final String pantoneValue;

        private ColorData(
                final String id, final String name, final String year, final String color, final String pantoneValue) {
            this.id = id;
            this.name = name;
            this.year = year;
            this.color = color;
            this.pantoneValue = pantoneValue;
        }
    }
}
