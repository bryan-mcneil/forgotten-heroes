package com.forgottenheroes.backend.repo;

import java.util.List;

/** Ghost bands by turn; Step 30 narrows the query to similar crowns and adds bots. */
public interface GhostRepository {

    void save(Ghost ghost);

    List<Ghost> findByTurn(int turn);
}
