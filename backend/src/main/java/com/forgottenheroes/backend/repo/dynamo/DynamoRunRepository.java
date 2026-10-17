package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.RunRepository;
import com.forgottenheroes.engine.model.RunState;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Runs in the RUN# folders of each USER# drawer: one GetItem to find, one PutItem to save. A run is
 * always looked up with its owner, so the key alone keeps one player out of another's runs.
 */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamo")
public class DynamoRunRepository implements RunRepository {

    private final DynamoDbTable<RunItem> table;

    public DynamoRunRepository(DynamoDbEnhancedClient client, @Value("${app.dynamodb.table}") String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(RunItem.class));
    }

    @Override
    public Optional<RunState> find(String userId, String runId) {
        Key key = Key.builder().partitionValue(Keys.user(userId)).sortValue(Keys.run(runId)).build();
        return Optional.ofNullable(table.getItem(key)).map(RunItem::toState);
    }

    @Override
    public void save(String userId, RunState state) {
        table.putItem(RunItem.from(userId, state, Instant.now()));
    }
}
