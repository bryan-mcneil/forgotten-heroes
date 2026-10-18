package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.LeaderboardEntry;
import java.time.Instant;
import java.time.YearMonth;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * One finished run as a folder in its season's drawer (architecture §5). The sort key is the rank:
 * {@code 99 - crowns} first, so the best run has the smallest key and a plain Query reads the top 50
 * in order, then the turns, then the player. Rows expire at the end of the following season.
 */
@DynamoDbBean
public class LeaderboardItem {

    private String pk;
    private String sk;
    private String userId;
    private String displayName;
    private int crowns;
    private int turns;
    private Instant finishedAt;
    private long expiresAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")                                // 1. LB#<season>: one drawer per month
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")                                // 2. <99-crowns>#<turns>#<userId>: the rank itself
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    /** Copied from the profile when the run ended; a later rename does not reach this row. */
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getCrowns() { return crowns; }
    public void setCrowns(int crowns) { this.crowns = crowns; }

    public int getTurns() { return turns; }
    public void setTurns(int turns) { this.turns = turns; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    static LeaderboardItem from(LeaderboardEntry entry) {
        LeaderboardItem item = new LeaderboardItem();
        item.setPk(Keys.leaderboard(entry.season()));
        item.setSk(Keys.leaderboardSort(entry.crowns(), entry.turns(), entry.userId()));
        item.setUserId(entry.userId());
        item.setDisplayName(entry.displayName());
        item.setCrowns(entry.crowns());
        item.setTurns(entry.turns());
        item.setFinishedAt(entry.finishedAt());
        item.setExpiresAt(Ttl.endOfNextSeason(YearMonth.parse(entry.season())));   // 3. gone with the season after
        return item;
    }

    LeaderboardEntry toEntry() {
        String season = pk.substring("LB#".length());
        return new LeaderboardEntry(season, userId, displayName, crowns, turns, finishedAt);
    }
}
