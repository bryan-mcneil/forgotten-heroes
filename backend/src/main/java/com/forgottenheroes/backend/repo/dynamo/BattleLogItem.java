package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.BattleLog;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.session.BandSnapshot;
import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * One battle as a folder in its run's drawer (architecture §5), the biggest item in the table: the
 * opponent's band and every event of the battle as JSON text, for replay. Gone after 7 days.
 */
@DynamoDbBean
public class BattleLogItem {

    private String pk;
    private String sk;
    private String runId;
    private int turn;
    private String opponentName;
    private String opponentBand;
    private String events;
    private String result;
    private long expiresAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")                                // 1. RUN#<runId>: one drawer per run
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")                                // 2. BATTLE#<turn, three digits>
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public int getTurn() { return turn; }
    public void setTurn(int turn) { this.turn = turn; }

    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }

    public String getOpponentBand() { return opponentBand; }
    public void setOpponentBand(String opponentBand) { this.opponentBand = opponentBand; }

    /** The battle's events as the engine's JSON, the same shape the API sends to the browser. */
    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    static BattleLogItem from(BattleLog log, Instant now) {
        BattleLogItem item = new BattleLogItem();
        item.setPk(Keys.run(log.runId()));
        item.setSk(Keys.battle(log.turn()));
        item.setRunId(log.runId());
        item.setTurn(log.turn());
        item.setOpponentName(log.opponentName());
        item.setOpponentBand(EventJson.mapper().writeValueAsString(log.opponentBand()));
        item.setEvents(EventJson.toJson(log.events()));                      // 3. the typed writer keeps "type"
        item.setResult(log.result().name());
        item.setExpiresAt(Ttl.expiresAt(now, Ttl.BATTLE_LOG));
        return item;
    }

    BattleLog toLog() {
        BandSnapshot band = EventJson.mapper().readValue(opponentBand, BandSnapshot.class);
        return new BattleLog(runId, turn, opponentName, band, EventJson.listFromJson(events), Outcome.valueOf(result));
    }
}
