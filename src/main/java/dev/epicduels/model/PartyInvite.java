package dev.epicduels.model;

import java.util.UUID;

public class PartyInvite {

    private final UUID partyId;
    private final UUID sender;
    private final UUID receiver;
    private final long timestamp;

    public PartyInvite(UUID partyId, UUID sender, UUID receiver) {
        this.partyId = partyId;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 30_000;
    }
}
