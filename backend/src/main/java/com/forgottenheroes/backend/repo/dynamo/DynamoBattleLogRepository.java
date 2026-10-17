package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.BattleLog;
import com.forgottenheroes.backend.repo.BattleLogRepository;
import java.nio.charset.StandardCharsets;
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
 * Battle logs in the BATTLE# folders of each RUN# drawer. DynamoDB refuses any item over 400 KB; this
 * repository refuses at 300 KB so the limit is hit in a test, not in production. Step 36 adds gzip.
 */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamo")
public class DynamoBattleLogRepository implements BattleLogRepository {

    /** Our own guard, under DynamoDB's hard 400 KB. */
    static final int MAX_BYTES = 300 * 1024;

    private final DynamoDbTable<BattleLogItem> table;

    public DynamoBattleLogRepository(DynamoDbEnhancedClient client, @Value("${app.dynamodb.table}") String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(BattleLogItem.class));
    }

    @Override
    public void save(BattleLog log) {
        BattleLogItem item = BattleLogItem.from(log, Instant.now());
        int bytes = item.getEvents().getBytes(StandardCharsets.UTF_8).length
            + item.getOpponentBand().getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_BYTES) {
            throw new IllegalStateException("battle log of run " + log.runId() + " turn " + log.turn() + " is " + bytes
                + " bytes, over the " + MAX_BYTES / 1024 + " KB guard (DynamoDB stops at 400 KB; Step 36 adds gzip)");
        }
        table.putItem(item);
    }

    @Override
    public Optional<BattleLog> find(String runId, int turn) {
        Key key = Key.builder().partitionValue(Keys.run(runId)).sortValue(Keys.battle(turn)).build();
        return Optional.ofNullable(table.getItem(key)).map(BattleLogItem::toLog);
    }
}
