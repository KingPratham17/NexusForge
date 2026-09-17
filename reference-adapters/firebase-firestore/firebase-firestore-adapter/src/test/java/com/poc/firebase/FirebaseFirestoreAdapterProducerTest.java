package com.poc.firebase;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FirebaseFirestoreAdapterProducerTest
        extends CamelTestSupport {

    @Test
    public void testProducerCreatesFirestoreDocument() throws Exception {

        String json =
                "{"
                        + "\"name\":\"Pratham\","
                        + "\"age\":22,"
                        + "\"role\":\"developer\","
                        + "\"source\":\"SAP Custom Adapter\""
                        + "}";

        FirebaseFirestoreAdapterComponentEndpoint endpoint =
                new FirebaseFirestoreAdapterComponentEndpoint();

        endpoint.getFirebaseProjectId();
        endpoint.setCollection("users");
        endpoint.setDocumentId("adapter-producer-test-001");
        endpoint.setOperation("CREATE");
        endpoint.setCredentialAlias("firebase-service-account");

        TestFirestoreDocumentService testService =
                new TestFirestoreDocumentService();

        FirebaseFirestoreAdapterComponentProducer producer =
                new FirebaseFirestoreAdapterComponentProducer(endpoint) {

                    @Override
                    protected FirestoreDocumentService createFirestoreService(
                            String projectId,
                            String credentialAlias) {

                        assertEquals("cpi-custom-adapter-poc", projectId);
                        assertEquals("firebase-service-account", credentialAlias);
                        return testService;
                    }
                };

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody(json);

        producer.process(exchange);

        String response =
                exchange.getIn().getBody(String.class);

        assertTrue(response.contains("SUCCESS"));
        assertEquals("users", testService.collection);
        assertEquals("adapter-producer-test-001", testService.documentId);
        assertEquals("Pratham", testService.data.get("name"));
        assertEquals(22, testService.data.get("age"));
    }

    private static class TestFirestoreDocumentService
            implements FirestoreDocumentService {

        private String collection;
        private String documentId;
        private Map<String, Object> data;

        @Override
        public void createDocument(
                String collection,
                String documentId,
                Map<String, Object> data) {

            this.collection = collection;
            this.documentId = documentId;
            this.data = data;
        }
    }
}
