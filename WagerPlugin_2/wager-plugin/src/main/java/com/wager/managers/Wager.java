package com.wager.managers;

import java.util.UUID;

public class Wager {

    public enum WagerState {
        WAITING,      // Listed in GUI, waiting for opponent
        ACCEPTED,     // Both players confirmed, about to start
        COUNTDOWN,    // Countdown in progress
        IN_PROGRESS,  // Fight is happening
        FINISHED      // Wager is over
    }

    private final UUID id;
    private final UUID creator;
    private final String creatorName;
    private UUID opponent;
    private String opponentName;
    private final double amount;
    private WagerState state;
    private String arenaId;
    private long createdAt;

    public Wager(UUID creator, String creatorName, double amount) {
        this.id = UUID.randomUUID();
        this.creator = creator;
        this.creatorName = creatorName;
        this.amount = amount;
        this.state = WagerState.WAITING;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getId() { return id; }
    public UUID getCreator() { return creator; }
    public String getCreatorName() { return creatorName; }
    public UUID getOpponent() { return opponent; }
    public String getOpponentName() { return opponentName; }
    public double getAmount() { return amount; }
    public WagerState getState() { return state; }
    public String getArenaId() { return arenaId; }
    public long getCreatedAt() { return createdAt; }

    public void setOpponent(UUID opponent, String opponentName) {
        this.opponent = opponent;
        this.opponentName = opponentName;
    }

    public void setState(WagerState state) {
        this.state = state;
    }

    public void setArenaId(String arenaId) {
        this.arenaId = arenaId;
    }

    public boolean isParticipant(UUID playerId) {
        return creator.equals(playerId) || (opponent != null && opponent.equals(playerId));
    }
}
