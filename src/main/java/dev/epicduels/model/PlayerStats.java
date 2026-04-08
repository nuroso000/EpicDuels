package dev.epicduels.model;

public class PlayerStats {

    private int wins;
    private int losses;

    public PlayerStats(int wins, int losses) {
        this.wins = wins;
        this.losses = losses;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void addWin() {
        this.wins++;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void addLoss() {
        this.losses++;
    }

    public int getTotalGames() {
        return wins + losses;
    }

    public double getWinRate() {
        if (getTotalGames() == 0) return 0.0;
        return (double) wins / getTotalGames() * 100.0;
    }
}
