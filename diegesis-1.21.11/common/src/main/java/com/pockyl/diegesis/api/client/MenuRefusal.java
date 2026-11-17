package com.pockyl.diegesis.api.client;

/** Why a menu-backed station ended without the player leaving it. */
public enum MenuRefusal {
    /** The server opened nothing in time (a claim, a protected region, the block was gone). */
    TIMEOUT,
    /** The server opened a menu the station does not take: its vanilla screen opens instead. */
    OTHER_MENU,
    /** The server closed the menu (the block broke, the player was pushed away, a plugin). */
    CLOSED_BY_SERVER
}
