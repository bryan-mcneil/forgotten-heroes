package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.Ghost;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.session.BandSnapshot;
import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * A ghost as a folder in the drawer of its turn (architecture §4.4, §5). The sort key carries the
 * crowns, the time and the run, so the drawer answers "similar crowns, newest first" in one Query.
 * Every ghost expires 30 days after it was saved.
 */
@DynamoDbBean
public class GhostItem {

    private String pk;
    private String sk;
    private String ownerUserId;
    private String runId;
    private int turn;
    private int crowns;
    private int hearts;
    private String band;
    private boolean bot;
    private Instant savedAt;
    private long expiresAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")                                // 1. GHOST#T<turn>: one drawer per turn
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")                                // 2. <crowns>#<time>#<runId>
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public int getTurn() { return turn; }
    public void setTurn(int turn) { this.turn = turn; }

    public int getCrowns() { return crowns; }
    public void setCrowns(int crowns) { this.crowns = crowns; }

    public int getHearts() { return hearts; }
    public void setHearts(int hearts) { this.hearts = hearts; }

    /** The band as JSON text: hero ids, stats, levels and perks, nothing else. */
    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public boolean getBot() { return bot; }
    public void setBot(boolean bot) { this.bot = bot; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    static GhostItem from(Ghost ghost) {
        GhostItem item = new GhostItem();
        item.setPk(Keys.ghostTurn(ghost.turn()));
        item.setSk(Keys.ghostSort(ghost.crowns(), ghost.savedAt(), ghost.runId()));
        item.setOwnerUserId(ghost.ownerUserId());
        item.setRunId(ghost.runId());
        item.setTurn(ghost.turn());
        item.setCrowns(ghost.crowns());
        item.setHearts(ghost.hearts());
        item.setBand(EventJson.mapper().writeValueAsString(ghost.band()));   // 3. the engine's own JSON
        item.setBot(ghost.bot());
        item.setSavedAt(ghost.savedAt());
        item.setExpiresAt(Ttl.expiresAt(ghost.savedAt(), Ttl.GHOST));        // 4. gone after 30 days
        return item;
    }

    Ghost toGhost() {
        BandSnapshot snapshot = EventJson.mapper().readValue(band, BandSnapshot.class);
        return new Ghost(ownerUserId, runId, turn, crowns, hearts, snapshot, bot, savedAt);
    }
}
