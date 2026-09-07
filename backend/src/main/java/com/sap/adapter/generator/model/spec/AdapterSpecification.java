package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdapterSpecification {
    private AdapterMeta adapter = new AdapterMeta();
    private TargetMeta target = new TargetMeta();
    private AuthenticationMeta authentication = new AuthenticationMeta();
    private MessageMeta message = new MessageMeta();
    private ConnectionMeta connection = new ConnectionMeta();
    private List<OperationConfig> operations = new ArrayList<>();
    private RuntimeMeta runtime = new RuntimeMeta();

    public AdapterSpecification() {}

    public AdapterMeta getAdapter() { return adapter; }
    public void setAdapter(AdapterMeta adapter) { this.adapter = adapter; }

    public TargetMeta getTarget() { return target; }
    public void setTarget(TargetMeta target) { this.target = target; }

    public AuthenticationMeta getAuthentication() { return authentication; }
    public void setAuthentication(AuthenticationMeta authentication) { this.authentication = authentication; }

    public MessageMeta getMessage() { return message; }
    public void setMessage(MessageMeta message) { this.message = message; }

    public ConnectionMeta getConnection() { return connection; }
    public void setConnection(ConnectionMeta connection) { this.connection = connection; }

    public List<OperationConfig> getOperations() { return operations; }
    public void setOperations(List<OperationConfig> operations) { this.operations = operations; }

    public RuntimeMeta getRuntime() { return runtime; }
    public void setRuntime(RuntimeMeta runtime) { this.runtime = runtime; }
}
