package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.Profile;
import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** The PROFILE folder as the translator sees it: a getter and a setter per attribute. */
@DynamoDbBean
public class ProfileItem {

    private String pk;
    private String sk;
    private String displayName;
    private Instant createdAt;
    private int bestCrowns;
    private int runsPlayed;
    private String activeRunId;

    @DynamoDbPartitionKey                                   // 1. the drawer label
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey                                        // 2. the place in the drawer
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getBestCrowns() { return bestCrowns; }
    public void setBestCrowns(int bestCrowns) { this.bestCrowns = bestCrowns; }

    public int getRunsPlayed() { return runsPlayed; }
    public void setRunsPlayed(int runsPlayed) { this.runsPlayed = runsPlayed; }

    public String getActiveRunId() { return activeRunId; }
    public void setActiveRunId(String activeRunId) { this.activeRunId = activeRunId; }

    /** Profile (the backend's word) to item (the ledger's word). */
    static ProfileItem from(Profile profile) {
        ProfileItem item = new ProfileItem();
        item.setPk(Keys.user(profile.userId())); // 1. labels come from Keys
        item.setSk(Keys.PROFILE);
        item.setDisplayName(profile.displayName());
        item.setCreatedAt(profile.createdAt());
        item.setBestCrowns(profile.bestCrowns());
        item.setRunsPlayed(profile.runsPlayed());
        item.setActiveRunId(profile.activeRunId());
        return item;
    }

    /** Item back to profile; the user id is read out of the drawer label. */
    Profile toProfile() {
        String userId = pk.substring("USER#".length()); // 2. "USER#bryan" -> "bryan"
        return new Profile(userId, displayName, createdAt, bestCrowns, runsPlayed, activeRunId);
    }
}
