package dev.dailyemerald.perks.patreon;

/** Anything the Patreon API said no to. */
public class PatreonException extends Exception {
    public PatreonException(String message) {
        super(message);
    }

    public PatreonException(String message, Throwable cause) {
        super(message, cause);
    }
}
