package dev.epicduels.model;

public enum TeamSize {
    TWO_VS_TWO(2),
    THREE_VS_THREE(3),
    FOUR_VS_FOUR(4);

    private final int playersPerTeam;

    TeamSize(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public int getTotalPlayers() {
        return playersPerTeam * 2;
    }

    public String getLabel() {
        return playersPerTeam + "v" + playersPerTeam;
    }
}
