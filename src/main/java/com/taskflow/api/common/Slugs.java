package com.taskflow.api.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");
    private static final int MAX_LENGTH = 50;

    private Slugs() {
    }

    /**
     * Turns a display name into a URL-safe slug: strips accents, lower-cases, and replaces
     * every run of non-alphanumeric characters with a single dash. Falls back to "workspace"
     * when nothing usable is left (e.g. a name written entirely in a non-Latin script).
     */
    public static String slugify(String input) {
        String stripped = Normalizer.normalize(input, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        String slug = EDGE_DASHES.matcher(NON_SLUG_CHARS.matcher(stripped).replaceAll("-")).replaceAll("");

        if (slug.length() > MAX_LENGTH) {
            slug = EDGE_DASHES.matcher(slug.substring(0, MAX_LENGTH)).replaceAll("");
        }
        return slug.isEmpty() ? "workspace" : slug;
    }
}
