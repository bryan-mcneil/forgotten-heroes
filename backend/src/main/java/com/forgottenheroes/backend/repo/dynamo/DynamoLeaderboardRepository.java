package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.LeaderboardEntry;
import com.forgottenheroes.backend.repo.LeaderboardRepository;
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
 * Results in the LB#&lt;season&gt; drawers: one PutItem per finished run, one Query for the top 50.
 * No sorting happens here: the sort key was designed so that the drawer's natural order is the ranking.
 */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamo")
public class DynamoLeaderboardRepository implements LeaderboardRepository {

    private final DynamoDbTable<LeaderboardItem> table;

    public DynamoLeaderboardRepository(DynamoDbEnhancedClient client, @Value("${app.dynamodb.table}") String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(LeaderboardItem.class));
    }

    @Override
    public void save(LeaderboardEntry entry) {
        table.putItem(LeaderboardItem.from(entry));          // same player, same score: same key, the row is replaced
    }

    @Override
    public List<LeaderboardEntry> top(String season, int limit) {
        QueryConditional wholeDrawer = QueryConditional.keyEqualTo(          // 1. PK = LB#<season>, any SK
            Key.builder().partitionValue(Keys.leaderboard(season)).build());
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(wholeDrawer)                                     // 2. forwards: smallest key = best run
            .limit(limit)                                                      // 3. the ledger reads at most this many
            .build();
        Page<LeaderboardItem> firstPage = table.query(request).iterator().next();   // 4. one page, never a second
        return firstPage.items().stream().map(LeaderboardItem::toEntry).toList();
    }
}
