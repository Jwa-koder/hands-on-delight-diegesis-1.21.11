package com.pockyl.diegesis.api.station;

/** Who owns a station's state. */
public enum Authority {
    /**
     * The client alone: works on any server, even one without the library, but other players do not see what the player
     * does, and nothing can move items between the station and the player's inventory. For visual stations, previews,
     * toys and animations.
     */
    CLIENT,
    /**
     * The server: every action is checked and applied there, items move safely, other players see the station change
     * and watch the objects being carried. Needs the library on the server; without it the block works as it does in
     * vanilla.
     */
    SERVER,
    /**
     * The block's own vanilla menu, opened on the server by the block's normal use and never shown: the station's slots are
     * windows on the menu's slots and every click is the one the vanilla screen would send. Works on any server, with or
     * without the library there. Set by {@code StationType.Builder.menu}; no {@code StationLogic} runs.
     */
    MENU
}
