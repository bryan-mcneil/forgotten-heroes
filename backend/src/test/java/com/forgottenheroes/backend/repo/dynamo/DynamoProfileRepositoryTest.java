package com.forgottenheroes.backend.repo.dynamo;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.backend.repo.Profile;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The first test against a real DynamoDB: Testcontainers starts the box, the test uses it, then it is gone. */
@Testcontainers
class DynamoProfileRepositoryTest {

    @Container                                                      // 1. started before the tests, removed after
    static final GenericContainer<?> DYNAMO = DynamoLocal.container();

    static DynamoProfileRepository repository;

    @BeforeAll
    static void createTableAndRepository() throws Exception {
        DynamoLocal dynamo = DynamoLocal.connect(DYNAMO);           // 2. the app's own config, the same table.json
        repository = new DynamoProfileRepository(dynamo.enhanced(), dynamo.tableName());
    }

    @Test
    void savedProfileComesBackWhole() {
        Profile profile = Profile.create("bryan", Instant.parse("2026-10-16T12:00:00Z"));
        repository.save(profile);
        assertThat(repository.find("bryan")).contains(profile);         // 3. a record compares every field
    }

    @Test
    void unknownUserIsEmpty() {
        assertThat(repository.find("nobody")).isEmpty();
    }
}
