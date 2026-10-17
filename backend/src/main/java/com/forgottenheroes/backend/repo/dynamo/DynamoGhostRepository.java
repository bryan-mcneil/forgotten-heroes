package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.Ghost;
import com.forgottenheroes.backend.repo.GhostRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * Ghosts in the GHOST#T&lt;turn&gt; drawers: one PutItem to save, one Query to read a whole drawer.
 * The first repository that reads more than one folder at a time.
 */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamo")
public class DynamoGhostRepository implements GhostRepository {

    private final DynamoDbTable<GhostItem> table;

    public DynamoGhostRepository(DynamoDbEnhancedClient client, @Value("${app.dynamodb.table}") String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(GhostItem.class));
    }

    @Override
    public void save(Ghost ghost) {
        table.putItem(GhostItem.from(ghost));
    }

    /** Every ghost of that turn, highest crowns first and the newest first among equal crowns. */
    @Override
    public List<Ghost> findByTurn(int turn) {
        QueryConditional wholeDrawer = QueryConditional.keyEqualTo(          // 1. PK = GHOST#T<turn>, any SK
            Key.builder().partitionValue(Keys.ghostTurn(turn)).build());
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(wholeDrawer)
            .scanIndexForward(false)                                           // 2. sort key descending
            .build();
        return table.query(request).items().stream().map(GhostItem::toGhost).toList();   // 3. page by page
    }
}
