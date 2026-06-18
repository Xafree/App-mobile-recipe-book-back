package fr.gymgod.common.constants;

public final class FeedConstants {

    /** Taille de page par défaut pour le fil public. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Taille de page maximale — plafond imposé côté serveur. */
    public static final int MAX_PAGE_SIZE = 50;

    /** Séparateur utilisé dans le curseur opaque (timestamp|uuid). */
    public static final String CURSOR_SEPARATOR = "|";

    private FeedConstants() {}
}
