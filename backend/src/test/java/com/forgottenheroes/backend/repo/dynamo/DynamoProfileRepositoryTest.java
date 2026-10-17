package com.forgottenheroes.backend.repo.dynamo;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.backend.repo.Profile;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** The first test against a real DynamoDB: Testcontainers starts the box, the test uses it, then it is gone. */
@Testcontainers
class DynamoProfileRepositoryTest {

    @Container // 1. started before the tests, removed after
    static final GenericContainer<?> DYNAMO = new GenericContainer<>("amazon/dynamodb-local:3.3.2")
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory")
            .withExposedPorts(8000);

    static DynamoProfileRepository repository;

    // setup and tests come next
    @BeforeAll
    static void createTableAndRepository() throws Exception {
        // 2. Docker picks a free port on your PC; the container object knows which
        String endpoint = "http://" + DYNAMO.getHost() + ":" + DYNAMO.getMappedPort(8000);
        DynamoConfig config = new DynamoConfig();
        DynamoDbClient client = config.dynamoDbClient(endpoint, "us-east-1");   // 3. the app's own code
        String tableName = TableDefinition.create(client);
        repository = new DynamoProfileRepository(config.dynamoDbEnhancedClient(client), tableName);
    }

    @Test
    void savedProfileComesBackWhole() {
        Profile profile = Profile.create("bryan", Instant.parse("2026-10-16T12:00:00Z"));
        repository.save(profile);
        assertThat(repository.find("bryan")).contains(profile);         // 4. a record compares every field
    }

    @Test
    void unknownUserIsEmpty() {
        assertThat(repository.find("nobody")).isEmpty();
    }
}
