package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.RunRepository;
import com.forgottenheroes.backend.repo.VersionConflictException;
import com.forgottenheroes.engine.model.RunState;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Runs in the RUN# folders of each USER# drawer: one GetItem to find, one conditional PutItem to
 * save. A run is always looked up with its owner, so the key alone keeps one player out of
 * another's runs. The condition on the write is the real guard of architecture §4.3: the ledger
 * checks the version stamp and replaces the folder as one step, so two overlapping saves leave
 * exactly one winner whatever two Lambdas thought they saw.
 */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamo")
public class DynamoRunRepository implements RunRepository {

    /** No folder under this key yet, or its version is still the one the caller loaded. */
    static final String UNCHANGED = "attribute_not_exists(PK) OR #v = :expected";

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
    public void save(String userId, RunState state, long expectedVersion) {
        Expression unchanged = Expression.builder()
            .expression(UNCHANGED)                                                     // 1. the sentence
            .putExpressionName("#v", "version")                                        // 2. #v stands for the attribute
            .putExpressionValue(":expected", AttributeValue.fromN(Long.toString(expectedVersion)))   // 3. :expected for the number
            .build();
        try {
            table.putItem(PutItemEnhancedRequest.builder(RunItem.class)
                .item(RunItem.from(userId, state, Instant.now()))
                .conditionExpression(unchanged)                                        // 4. checked inside the write
                .build());
        } catch (ConditionalCheckFailedException e) {                                  // 5. someone else wrote first
            long current = find(userId, state.runId()).map(RunState::version).orElse(NEW_RUN);
            throw new VersionConflictException(state.runId(), expectedVersion, current);
        }
    }
}
