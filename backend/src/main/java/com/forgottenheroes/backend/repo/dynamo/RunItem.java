package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.engine.json.RunStateJson;
import com.forgottenheroes.engine.model.RunState;
import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * A run as a folder in its owner's drawer (architecture §5): the whole state as one compact JSON text,
 * plus a few numbers copied out of it so the admin page, and later a Query, can read a run without
 * parsing it. {@code expiresAt} is set only once the run is over; 90 days later DynamoDB deletes it.
 */
@DynamoDbBean
public class RunItem {

    private String pk;
    private String sk;
    private String status;
    private int turn;
    private int gold;
    private int hearts;
    private int crowns;
    private long version;
    private Instant updatedAt;
    private String state;
    private Long expiresAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")                                // 1. the owner's drawer, shared with the profile
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")                                // 2. RUN#<runId>
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTurn() { return turn; }
    public void setTurn(int turn) { this.turn = turn; }

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }

    public int getHearts() { return hearts; }
    public void setHearts(int hearts) { this.hearts = hearts; }

    public int getCrowns() { return crowns; }
    public void setCrowns(int crowns) { this.crowns = crowns; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /** The run itself, as {@code RunStateJson} writes it: band, tavern, seed, counters, everything. */
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    /** Null while the run is in progress (kept for ever); seconds since 1970 once it is over. */
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    static RunItem from(String userId, RunState run, Instant now) {
        RunItem item = new RunItem();
        item.setPk(Keys.user(userId));
        item.setSk(Keys.run(run.runId()));
        item.setStatus(run.status().name());               // 3. copies for the admin page, never read back
        item.setTurn(run.turn());
        item.setGold(run.gold());
        item.setHearts(run.hearts());
        item.setCrowns(run.crowns());
        item.setVersion(run.version());
        item.setUpdatedAt(now);
        item.setState(RunStateJson.toJson(run));            // 4. the truth, one text attribute
        item.setExpiresAt(run.status().isOver() ? Ttl.expiresAt(now, Ttl.RUN) : null);
        return item;
    }

    RunState toState() {
        return RunStateJson.fromJson(state);
    }
}
