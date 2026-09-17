package com.poc.firebase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FirebaseFirestoreAdapterComponentProducer extends DefaultProducer {

    private static final transient Logger LOG =
            LoggerFactory.getLogger(
                    FirebaseFirestoreAdapterComponentProducer.class);

    private final FirebaseFirestoreAdapterComponentEndpoint endpoint;

    public FirebaseFirestoreAdapterComponentProducer(
            FirebaseFirestoreAdapterComponentEndpoint endpoint) {

        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {

        LOG.info("=== Firebase Firestore Adapter Producer ===");

        // ---------------------------------------------------------
        // 1. Read adapter configuration
        // ---------------------------------------------------------

        String firebaseProjectId =
                endpoint.getFirebaseProjectId();

        String collection =
                endpoint.getCollection();

        String documentId =
                endpoint.getDocumentId();

        String operation =
                endpoint.getOperation();

        String credentialAlias =
                endpoint.getCredentialAlias();

        LOG.info("Firebase Project ID : {}", firebaseProjectId);
        LOG.info("Collection           : {}", collection);
        LOG.info("Document ID          : {}", documentId);
        LOG.info("Operation            : {}", operation);
        LOG.info("Credential Alias     : {}", credentialAlias);


        // ---------------------------------------------------------
        // 2. Validate adapter configuration
        // ---------------------------------------------------------

        if (firebaseProjectId == null
                || firebaseProjectId.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Firebase Project ID must be configured");
        }

        if (collection == null
                || collection.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Firestore Collection must be configured");
        }

        if (documentId == null
                || documentId.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Firestore Document ID must be configured");
        }

        if (credentialAlias == null
                || credentialAlias.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Firebase Secure Parameter alias must be configured");
        }

        if (operation == null
                || operation.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Firestore Operation must be configured");
        }


        // ---------------------------------------------------------
        // 3. Read incoming Camel message body
        // ---------------------------------------------------------

        String body =
                exchange.getIn().getBody(String.class);

        if (body == null || body.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Firebase Firestore adapter received "
                            + "an empty message body");
        }

        LOG.info("Incoming message received");


        // ---------------------------------------------------------
        // 4. Convert JSON payload into a Map
        // ---------------------------------------------------------

        ObjectMapper objectMapper =
                new ObjectMapper();

        Map<String, Object> data =
                objectMapper.readValue(
                        body,
                        new TypeReference<Map<String, Object>>() {
                        });


        // ---------------------------------------------------------
        // 5. Create Firebase service using SAP Secure Store
        // ---------------------------------------------------------

        FirestoreDocumentService firebaseService =
                createFirestoreService(
                        firebaseProjectId,
                        credentialAlias
                );


        // ---------------------------------------------------------
        // 6. Execute Firestore operation
        // ---------------------------------------------------------

        if ("CREATE".equalsIgnoreCase(operation)
                || "WRITE".equalsIgnoreCase(operation)) {

            firebaseService.createDocument(
                    collection,
                    documentId,
                    data
            );

            String response =
                    "SUCCESS: Firestore document created at "
                            + collection
                            + "/"
                            + documentId;

            LOG.info(response);

            // -----------------------------------------------------
            // 7. Return response to Camel
            // -----------------------------------------------------

            exchange.getIn().setBody(response);

        } else {

            throw new IllegalArgumentException(
                    "Unsupported Firestore operation: "
                            + operation);
        }
    }


    /**
     * Creates the Firestore service.
     *
     * Kept as a separate method so that the service can
     * be replaced/mocked during unit testing.
     */
    protected FirestoreDocumentService createFirestoreService(
            String firebaseProjectId,
            String credentialAlias) throws Exception {

        return new FirebaseFirestoreService(
                firebaseProjectId,
                credentialAlias
        );
    }
}