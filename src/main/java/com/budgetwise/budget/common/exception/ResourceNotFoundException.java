package com.budgetwise.budget.common.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * Generic exception that works for any resource type (Product, Market, User, etc.)
 *
 * Usage examples:
 *
 * Simple message:
 *   throw new ResourceNotFoundException("Product not found");
 *
 * With resource details:
 *   throw new ResourceNotFoundException("Product", "id", productId);
 *
 * In repository methods:
 *   return repository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates exception with a custom message.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates exception with resource details.
     * Generates message like: "Product not found with id: 123"
     *
     * @param resourceName Type of resource (e.g., "Product", "Market")
     * @param fieldName Field used for lookup (e.g., "id", "productName")
     * @param fieldValue Value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }
}