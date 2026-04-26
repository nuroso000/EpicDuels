package dev.epicduels.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Tournament {

    public static class Match {
        public final UUID matchId = UUID.randomUUID();
        public final UUID p1;
        public final UUID p2;
        public UUID duelInstanceId;
        public UUID winner;
        public boolean started;
        public boolean finished;

        public Match(UUID p1, UUID p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        public boolean involves(UUID id) {
            return p1.equals(id) || p2.equals(id);
        }
    }

    private final UUID id;
    private final UUID partyId;
    private final String kitName;
    private final List<UUID> initialParticipants;
    private final Set<UUID> eliminated = new LinkedHashSet<>();
    private List<UUID> currentRoundWinners = new ArrayList<>();
    private final List<Match> activeMatches = new ArrayList<>();
    private final List<Match> pendingMatches = new ArrayList<>();
    private int roundNumber;
    private boolean finished;
    private UUID champion;

    public Tournament(UUID partyId, List<UUID> participants, String kitName) {
        this.id = UUID.randomUUID();
        this.partyId = partyId;
        this.kitName = kitName;
        this.initialParticipants = new ArrayList<>(participants);
        this.roundNumber = 0;
    }

    public UUID getId() { return id; }
    public UUID getPartyId() { return partyId; }
    public String getKitName() { return kitName; }
    public List<UUID> getInitialParticipants() { return initialParticipants; }
    public Set<UUID> getEliminated() { return eliminated; }
    public List<UUID> getCurrentRoundWinners() { return currentRoundWinners; }
    public void setCurrentRoundWinners(List<UUID> list) { this.currentRoundWinners = list; }
    public List<Match> getActiveMatches() { return activeMatches; }
    public List<Match> getPendingMatches() { return pendingMatches; }
    public int getRoundNumber() { return roundNumber; }
    public void incrementRound() { roundNumber++; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean f) { this.finished = f; }
    public UUID getChampion() { return champion; }
    public void setChampion(UUID c) { this.champion = c; }
}
