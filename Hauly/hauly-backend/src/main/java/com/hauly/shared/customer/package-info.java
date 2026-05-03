/**
 * Customer bounded context (shared kernel — used by intake and marketplace).
 * Aggregate root: Customer.
 *
 * Auto-matching strategy: when an INTAKE order references a customer,
 * we try line_id → phone → create new GUEST. Email/password only for REGISTERED
 * customers (Phase 2 marketplace signup).
 */
package com.hauly.shared.customer;
