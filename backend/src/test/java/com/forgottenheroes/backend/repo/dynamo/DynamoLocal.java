package com.forgottenheroes.backend.repo.dynamo;

import java.io.IOException;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

/**
 * What every repository test needs from its DynamoDB box: the phone line, the translator and the
 * table's name. Each test class declares its own {@code @Container} (one fresh box per class) and
 * calls {@link #connect} once, which builds the clients with the app's own {@code DynamoConfig} and
 * creates the table from the same {@code infra/table.json} the real one comes from.
 */
record DynamoLocal(DynamoDbClient client, DynamoDbEnhancedClient enhanced, String tableName) {

    /** The same image and command as docker-compose.yml, on a port Docker picks. */
    static GenericContainer<?> container() {
        return new GenericContainer<>("amazon/dynamodb-local:3.3.2")
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory")
            .withExposedPorts(8000);
    }

    static DynamoLocal connect(GenericContainer<?> box) throws IOException {
        String endpoint = "http://" + box.getHost() + ":" + box.getMappedPort(8000);
        DynamoConfig config = new DynamoConfig();
        DynamoDbClient client = config.dynamoDbClient(endpoint, "us-east-1");
        return new DynamoLocal(client, config.dynamoDbEnhancedClient(client), TableDefinition.create(client));
    }

    /** The item as the ledger holds it, for checks the translator hides: attribute names, the TTL number. */
    Map<String, AttributeValue> rawItem(String pk, String sk) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("PK", AttributeValue.fromS(pk), "SK", AttributeValue.fromS(sk)))
            .build();
        return client.getItem(request).item();
    }
}
