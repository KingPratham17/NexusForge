package com.sap.adapter.generator.registry;

import com.sap.adapter.generator.model.spec.ConnectionParameter;
import com.sap.adapter.generator.model.spec.OperationConfig;
import com.sap.adapter.generator.model.tech.TechnologyDefinition;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TechnologyRegistry {

    private final Map<String, TechnologyDefinition> catalog = new LinkedHashMap<>();

    public TechnologyRegistry() {
        initRest();
        initSftp();
        initMongoDb();
        initKafka();
    }

    private void initRest() {
        TechnologyDefinition rest = new TechnologyDefinition(
                "rest-http",
                "REST / HTTP Endpoint",
                "http-api",
                "rest-custom",
                "Generic REST HTTP integration adapter supporting JSON/XML REST web services."
        );
        rest.setSupportedDirections(List.of("receiver", "sender"));
        rest.setAuthenticationModels(List.of("secure-parameter", "oauth2", "basic", "none"));

        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("baseUrl", "Base URL", "string", true, "https://api.example.com", "Target REST endpoint base URL"));
        params.add(new ConnectionParameter("credentialAlias", "Secure Parameter Alias", "secure-alias", false, "REST_CREDS", "Credential alias for API authentication"));
        params.add(new ConnectionParameter("resourcePath", "Resource Path", "string", true, "/v1/resource", "API path endpoint"));
        rest.setConnectionParameters(params);

        List<OperationConfig> ops = new ArrayList<>();
        ops.add(new OperationConfig("POST", true, "Send HTTP POST request"));
        ops.add(new OperationConfig("GET", false, "Send HTTP GET request"));
        ops.add(new OperationConfig("PUT", false, "Send HTTP PUT request"));
        rest.setDefaultOperations(ops);

        catalog.put(rest.getId(), rest);
    }

    private void initSftp() {
        TechnologyDefinition sftp = new TechnologyDefinition(
                "sftp-transfer",
                "SFTP File Storage",
                "file-transfer",
                "sftp-custom",
                "Secure File Transfer Protocol adapter for file payload operations."
        );
        sftp.setSupportedDirections(List.of("receiver", "sender"));
        sftp.setAuthenticationModels(List.of("user-credential", "ssh-key"));

        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("host", "SFTP Host", "string", true, "sftp.example.com", "SFTP Server hostname"));
        params.add(new ConnectionParameter("port", "Port", "integer", true, "22", "SFTP Port"));
        params.add(new ConnectionParameter("remoteDir", "Remote Directory", "string", true, "/upload", "Destination folder path"));
        sftp.setConnectionParameters(params);

        List<OperationConfig> ops = new ArrayList<>();
        ops.add(new OperationConfig("UPLOAD", true, "Upload file to SFTP remote folder"));
        ops.add(new OperationConfig("DOWNLOAD", false, "Download file from SFTP folder"));
        sftp.setDefaultOperations(ops);

        catalog.put(sftp.getId(), sftp);
    }

    private void initMongoDb() {
        TechnologyDefinition mongo = new TechnologyDefinition(
                "mongodb",
                "MongoDB Document Store",
                "nosql-database",
                "mongo-custom",
                "MongoDB document database integration adapter supporting collection document inserts and queries."
        );
        mongo.setSupportedDirections(List.of("receiver", "sender"));
        mongo.setAuthenticationModels(List.of("secure-parameter", "user-credential"));

        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("connectionString", "Connection URI", "string", true, "mongodb://cluster.example.com", "MongoDB connection URI endpoint"));
        params.add(new ConnectionParameter("databaseName", "Database Name", "string", true, "integration_db", "Target MongoDB database name"));
        params.add(new ConnectionParameter("collectionName", "Collection", "string", true, "orders", "Target MongoDB collection name"));
        params.add(new ConnectionParameter("credentialAlias", "Secure Parameter Alias", "secure-alias", true, "MONGO_CREDS", "Secure store alias holding database credentials"));
        mongo.setConnectionParameters(params);

        List<OperationConfig> ops = new ArrayList<>();
        ops.add(new OperationConfig("INSERT", true, "Insert document payload into MongoDB collection"));
        ops.add(new OperationConfig("FIND", false, "Query document from MongoDB collection"));
        mongo.setDefaultOperations(ops);

        catalog.put(mongo.getId(), mongo);
    }

    private void initKafka() {
        TechnologyDefinition kafka = new TechnologyDefinition(
                "kafka-stream",
                "Apache Kafka Stream",
                "event-broker",
                "kafka-custom",
                "Apache Kafka distributed event streaming adapter for publishing and consuming message topics."
        );
        kafka.setSupportedDirections(List.of("receiver", "sender"));
        kafka.setAuthenticationModels(List.of("sasl-ssl", "tls"));

        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("bootstrapServers", "Bootstrap Servers", "string", true, "kafka.example.com:9092", "Comma-separated list of Kafka broker hosts"));
        params.add(new ConnectionParameter("topic", "Topic Name", "string", true, "orders-events", "Kafka target topic name"));
        params.add(new ConnectionParameter("credentialAlias", "Secure Parameter Alias", "secure-alias", true, "KAFKA_SASL_CREDS", "Secure store alias holding SASL credentials"));
        kafka.setConnectionParameters(params);

        List<OperationConfig> ops = new ArrayList<>();
        ops.add(new OperationConfig("PRODUCE", true, "Publish message event payload to Kafka topic"));
        ops.add(new OperationConfig("CONSUME", false, "Subscribe and consume message events from Kafka topic"));
        kafka.setDefaultOperations(ops);

        catalog.put(kafka.getId(), kafka);
    }

    public Collection<TechnologyDefinition> getAllTechnologies() {
        return catalog.values();
    }

    public Optional<TechnologyDefinition> getById(String id) {
        return Optional.ofNullable(catalog.get(id));
    }
}
