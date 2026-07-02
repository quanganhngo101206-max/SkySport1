package com.example.skysport1.util;

/**
 * Utility class for generating URL-friendly slugs from Vietnamese text.
 * Handles diacritical marks and special characters.
 */
public class SlugUtil {

    /**
     * Generate a slug from a given name.
     * If name is null or blank, uses the provided ID as fallback.
     *
     * @param name the name to convert to slug
     * @param id   the fallback ID if name is null
     * @return a URL-friendly slug
     */
    public static String generateSlug(String name, String id) {
        if (name == null || name.isBlank()) {
            return id != null ? id.toLowerCase() : "unknown";
        }

        return name.toLowerCase()
                // Replace Vietnamese accented characters
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ýÿ]", "y")
                .replaceAll("[đ]", "d")
                // Remove non-alphanumeric characters except spaces and hyphens
                .replaceAll("[^a-z0-9\\s-]", "")
                // Replace multiple spaces with single hyphen
                .replaceAll("\\s+", "-")
                // Replace multiple hyphens with single hyphen
                .replaceAll("-+", "-")
                // Trim whitespace and leading/trailing hyphens
                .trim()
                .replaceAll("^-+|-+$", "");
    }

    /**
     * Generate a unique slug by appending a suffix if needed.
     * Useful when the original slug might be collision with another product.
     *
     * @param baseSlug the base slug to start with
     * @param suffix   the numeric suffix (e.g., 1 => "-1")
     * @return a slug with suffix appended
     */
    public static String generateUniqueSlug(String baseSlug, int suffix) {
        if (baseSlug == null || baseSlug.isBlank()) {
            return "-" + suffix;
        }
        return baseSlug + "-" + suffix;
    }

    /**
     * Check if two slugs are semantically equivalent
     * (useful for comparing original slug with potentially updated slug).
     *
     * @param slug1 first slug
     * @param slug2 second slug
     * @return true if slugs are equal (case-insensitive)
     */
    public static boolean slugEquals(String slug1, String slug2) {
        if (slug1 == null && slug2 == null) return true;
        if (slug1 == null || slug2 == null) return false;
        return slug1.trim().equalsIgnoreCase(slug2.trim());
    }
}


