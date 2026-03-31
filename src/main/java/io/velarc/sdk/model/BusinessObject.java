package io.velarc.sdk.model;

/**
 * Represents a business object reference.
 *
 * @param type the business object type
 * @param id   the business object identifier
 */
public record BusinessObject(String type, String id) {

    public static BusinessObject of(String type, String id) {
        return new BusinessObject(type, id);
    }
}
