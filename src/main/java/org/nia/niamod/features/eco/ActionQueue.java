package org.nia.niamod.features.eco;

import org.nia.niamod.models.eco.*;

import java.util.ArrayDeque;
import java.util.Locale;

public final class ActionQueue {
    private final ArrayDeque<GameChange> queue = new ArrayDeque<>();

    public synchronized void enqueue(GameChange change) {
        if (change != null) {
            queue.addLast(change);
        }
    }

    public void drainNext(GameActions gameActions) {
        GameChange next;
        synchronized (this) {
            next = queue.pollFirst();
        }
        if (next != null && gameActions != null) {
            gameActions.apply(next);
        }
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean hasQueuedStat(String territoryKey, TerritoryUpgrade upgrade) {
        if (upgrade == null) {
            return false;
        }
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> change instanceof StatChange stat
                && normalize(stat.territoryName()).equals(key)
                && stat.upgrade() == upgrade);
    }

    public synchronized boolean hasQueuedTax(String territoryKey) {
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> (change instanceof TaxChange tax
                && normalize(tax.territoryName()).equals(key))
                || change instanceof GlobalTaxChange);
    }

    public synchronized boolean hasQueuedBorders(String territoryKey) {
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> (change instanceof BordersChange borders
                && normalize(borders.territoryName()).equals(key))
                || change instanceof GlobalBordersChange);
    }

    public synchronized boolean hasQueuedRoute(String territoryKey, TerritoryRoute route) {
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> (change instanceof RouteChange(
                String territoryName, TerritoryRoute route1
        )
                && normalize(territoryName).equals(key)
                && (route == null || route1 == route))
                || change instanceof GlobalRouteChange);
    }

    public synchronized boolean hasQueuedHeadquarters() {
        return queue.stream().anyMatch(change -> change instanceof HeadquartersChange);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

}
