package com.forgottenheroes.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.backend.repo.VersionConflictException;
import com.forgottenheroes.backend.repo.dynamo.DynamoBattleLogRepository;
import com.forgottenheroes.backend.repo.dynamo.DynamoGhostRepository;
import com.forgottenheroes.backend.repo.dynamo.DynamoLocal;
import com.forgottenheroes.backend.repo.dynamo.DynamoProfileRepository;
import com.forgottenheroes.backend.repo.dynamo.DynamoRunRepository;
import com.forgottenheroes.backend.web.dto.ActionRequest;
import com.forgottenheroes.backend.web.dto.ActionResponse;
import com.forgottenheroes.backend.web.dto.RunStateDto;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.session.GameSession;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The race of architecture §4.3, made to happen on purpose: the real service on the real DynamoDB
 * repositories, two threads released at the same instant with the same version, twenty times. Exactly
 * one wins, the other gets a conflict, and the ledger moves one step. Which thread wins is not fixed,
 * and sometimes the loser is caught by the service's quick check instead of the ledger's condition;
 * the outcome is the same either way.
 */
@Testcontainers
class RunServiceConcurrencyTest {

    private static final GameSession SESSION = new GameSession(ContentLoader.load());

    @Container
    static final GenericContainer<?> DYNAMO = DynamoLocal.container();

    static RunService service;
    static ExecutorService pool;

    @BeforeAll
    static void buildTheServiceOnDynamo() throws Exception {
        DynamoLocal dynamo = DynamoLocal.connect(DYNAMO);
        DynamoProfileRepository profiles = new DynamoProfileRepository(dynamo.enhanced(), dynamo.tableName());
        DynamoGhostRepository ghosts = new DynamoGhostRepository(dynamo.enhanced(), dynamo.tableName());
        service = new RunService(SESSION,
            new DynamoRunRepository(dynamo.enhanced(), dynamo.tableName()), ghosts,
            new DynamoBattleLogRepository(dynamo.enhanced(), dynamo.tableName()),
            profiles, new ProfileService(profiles), new GhostMatchmaking(ghosts), () -> 42L);
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterAll
    static void stopThreads() {
        pool.shutdownNow();
    }

    @RepeatedTest(20)
    void twoActionsWithTheSameVersionLeaveExactlyOneWinner(RepetitionInfo repetition) throws Exception {
        String user = "racer-" + repetition.getCurrentRepetition();
        RunStateDto run = service.start(user);
        String runId = run.runId();
        long version = run.version();
        CountDownLatch go = new CountDownLatch(1);

        Future<Object> buy = pool.submit(() -> { go.await(); return attempt(user, runId, new ActionRequest.Buy(0, 0, version)); });
        Future<Object> roll = pool.submit(() -> { go.await(); return attempt(user, runId, new ActionRequest.Roll(version)); });
        go.countDown();                                                            // 1. both start now
        List<Object> outcomes = List.of(buy.get(), roll.get());

        assertThat(outcomes).filteredOn(o -> o instanceof VersionConflictException).hasSize(1);   // 2. one loser
        assertThat(outcomes).filteredOn(o -> o instanceof ActionResponse).hasSize(1);             // 3. one winner
        ActionResponse winner = (ActionResponse) outcomes.stream().filter(o -> o instanceof ActionResponse).findFirst().orElseThrow();
        RunStateDto stored = service.get(user, runId);
        assertThat(stored.version()).isEqualTo(version + 1);                                      // 4. the stamp moved once
        assertThat(stored).isEqualTo(winner.state());                                             // 5. and only the winner's work is there
    }

    private static Object attempt(String user, String runId, ActionRequest request) {
        try {
            return service.apply(user, runId, request);
        } catch (VersionConflictException conflict) {
            return conflict;
        }
    }
}
