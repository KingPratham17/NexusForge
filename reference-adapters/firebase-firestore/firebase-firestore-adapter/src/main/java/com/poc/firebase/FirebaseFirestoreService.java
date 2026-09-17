package com.poc.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import com.sap.it.api.ITApiFactory;
import com.sap.it.api.securestore.SecureStoreService;
import com.sap.it.api.securestore.UserCredential;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FirebaseFirestoreService implements FirestoreDocumentService {

    private final Firestore firestore;

    public FirebaseFirestoreService(
            String projectId,
            String credentialAlias) throws Exception {

        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Firebase Project ID cannot be empty");
        }

        if (credentialAlias == null || credentialAlias.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Firebase Secure Parameter alias cannot be empty");
        }

        SecureStoreService secureStoreService =
                ITApiFactory.getService(
                        SecureStoreService.class,
                        null);

        if (secureStoreService == null) {
            throw new IllegalStateException(
                    "SAP SecureStoreService is not available");
        }

        UserCredential userCredential =
                secureStoreService.getUserCredential(credentialAlias);

        if (userCredential == null) {
            throw new IllegalStateException(
                    "No SAP Secure Store credential found for alias: "
                            + credentialAlias);
        }

        char[] password = userCredential.getPassword();

        if (password == null || password.length == 0) {
            throw new IllegalStateException(
                    "Secure Store alias '" + credentialAlias
                            + "' does not contain a value");
        }

        String serviceAccountJson =
                new String(password);

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(
                        new ByteArrayInputStream(
                                serviceAccountJson.getBytes(
                                        StandardCharsets.UTF_8)));

        this.firestore =
                FirestoreOptions.newBuilder()
                        .setCredentials(credentials)
                        .setProjectId(projectId)
                        .build()
                        .getService();
    }

    @Override
    public void createDocument(
            String collection,
            String documentId,
            Map<String, Object> data) throws Exception {

        firestore
                .collection(collection)
                .document(documentId)
                .set(data)
                .get();
    }

    public Firestore getFirestore() {
        return firestore;
    }
}