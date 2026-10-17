package com.forgottenheroes.backend.repo.dynamo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Creates the table from infra/table.json, the same file the by-hand script sends to the CLI. */
final class TableDefinition {

    private TableDefinition() {
    }

    /** Reads the file, sends CreateTable, returns the table's name. */
    static String create(DynamoDbClient client) throws IOException {
         // 1. Maven runs the backend's tests from backend/, so the repo root is one folder up
        JsonNode json = JsonMapper.builder().build().readTree(Files.readString(Path.of("../infra/table.json")));

        // 2. each JSON object becomes the SDK object with the same fields
        List<AttributeDefinition> attributes = new ArrayList<>();
        for (JsonNode a : json.get("AttributeDefinitions")) {
            attributes.add(AttributeDefinition.builder()
                    .attributeName(a.get("AttributeName").asString())
                    .attributeType(a.get("AttributeType").asString())
                    .build());
        }

        List<KeySchemaElement> keys = new ArrayList<>();
        for (JsonNode k : json.get("KeySchema")) {
            keys.add(KeySchemaElement.builder()
                    .attributeName(k.get("AttributeName").asString())
                    .keyType(k.get("KeyType").asString())
                    .build());
        }

        // 3. the same request the CLI sent in chunk 2
        String tableName = json.get("TableName").asString();
        client.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(attributes)
                .keySchema(keys)
                .billingMode(json.get("BillingMode").asString())
                .build());
        return tableName;
    }
}
