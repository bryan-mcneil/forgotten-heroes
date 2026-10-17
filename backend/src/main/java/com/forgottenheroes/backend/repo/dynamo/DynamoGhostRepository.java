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
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * Ghosts in the GHOST#T&lt;turn&gt; drawers: one PutItem to save, one Query to read a drawer or a slice
 * of it. The slice is a range of the sort key, which is why that key starts with zero-padded crowns.
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

    /** The slice of the drawer with those crowns, highest first, newest first within; one page of at most {@code limit}. */
    @Override
    public List<Ghost> findCandidates(int turn, int crownsMin, int crownsMax, int limit) {
        Key from = Key.builder().partitionValue(Keys.ghostTurn(turn)).sortValue(Keys.ghostLowerBound(crownsMin)).build();
        Key to = Key.builder().partitionValue(Keys.ghostTurn(turn)).sortValue(Keys.ghostUpperBound(crownsMax)).build();
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.sortBetween(from, to))          // 1. "06#" <= SK <= "09#", as text
            .scanIndexForward(false)                                           // 2. highest crowns first, newest within
            .limit(limit)                                                      // 3. the ledger stops reading at limit
            .build();
        Page<GhostItem> firstPage = table.query(request).iterator().next();    // 4. one page, never a second
        return firstPage.items().stream().map(GhostItem::toGhost).toList();
    }
}
