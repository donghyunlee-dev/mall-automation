# Product Status Automation Spec

## Overview

This document defines the business logic for automatically switching a product's status between **"Out of Stock (품절)"** and **"On Sale (판매)"** based on inventory conditions.

The automation must evaluate conditions per product and apply the correct status transition without manual intervention.

---

## Scope

Two operational scopes are defined:

| Scope | Description |
|---|---|
| `General (범용)` | Applies to all general products |
| `Exclusive (전용, 에쓰푸드몰)` | Applies only to products sold exclusively on S-Food Mall |

---

## Prerequisites (Base Conditions)

Both scopes share the same prerequisites. A product is **eligible for evaluation** only when **all** of the following are satisfied:

### Zone Code
- Must be **numeric only**
- The following zone codes are **excluded** from evaluation:
  - `EXT`, `MSC`, `PRC`, `RCV`, `SHP`, `WOR`, `DMG`, `RTN`, `SHP`

### Warehouse
- Target warehouse must be: **`OA110` (물류창고)**

> ⚠️ If either condition is not met, the product must be **skipped** — no status change should occur.

---

## Status Transition Logic

### 🔴 Trigger: Switch to "Out of Stock (품절)"

| Scope | Condition |
|---|---|
| General | Total available inventory **≤ 20 EA** |
| Exclusive | Total available inventory **≤ 1 EA** |

- When the condition is met → set product status to **`OUT_OF_STOCK`**

---

### 🟢 Trigger: Resume Sale (판매 재개) — Switch back to "On Sale"

Both conditions below must be satisfied simultaneously (`AND` logic):

| Scope | Condition A | Condition B |
|---|---|---|
| General | Remaining ratio **≥ 50** AND lot exists | Total available inventory **≥ 21 EA** |
| Exclusive | Remaining ratio **≥ 50** AND lot exists | Total available inventory **≥ 2 EA** |

- When **both** conditions are met → set product status to **`ON_SALE`**

---

## Decision Flow

```
START
  │
  ├─ Is zone code numeric? ──── NO ──► SKIP (no change)
  │         │
  │        YES
  │         │
  ├─ Is zone code excluded? ─── YES ─► SKIP (no change)
  │         │
  │        NO
  │         │
  ├─ Is warehouse == OA110? ─── NO ──► SKIP (no change)
  │         │
  │        YES
  │         │
  ├─ Determine scope: General or Exclusive
  │
  ├─ [OUT_OF_STOCK CHECK]
  │     General:   available_qty <= 20  → set OUT_OF_STOCK
  │     Exclusive: available_qty <= 1   → set OUT_OF_STOCK
  │
  └─ [ON_SALE CHECK]  (only when currently OUT_OF_STOCK)
        General:
          remaining_ratio >= 50 AND lot_exists = true
          AND available_qty >= 21
          → set ON_SALE

        Exclusive:
          remaining_ratio >= 50 AND lot_exists = true
          AND available_qty >= 2
          → set ON_SALE

END
```

---

## Data Model (Expected Inputs)

```typescript
interface ProductEvaluationInput {
  productId: string;
  scope: "GENERAL" | "EXCLUSIVE";
  zoneCode: string;           // e.g. "12345" or "EXT"
  warehouseCode: string;      // e.g. "OA110"
  availableQty: number;       // total available inventory (EA)
  remainingRatio: number;     // remaining ratio (0~100)
  lotExists: boolean;         // whether a valid lot exists
  currentStatus: "ON_SALE" | "OUT_OF_STOCK";
}
```

---

## Output / Side Effects

```typescript
type StatusTransitionResult =
  | { action: "SET_OUT_OF_STOCK"; productId: string }
  | { action: "SET_ON_SALE"; productId: string }
  | { action: "NO_CHANGE"; productId: string }
  | { action: "SKIPPED"; productId: string; reason: string };
```

---

## Rules Summary Table

| Condition | General | Exclusive |
|---|---|---|
| Out of Stock threshold | available ≤ 20 EA | available ≤ 1 EA |
| Resume Sale: lot condition | remaining ratio ≥ 50 AND lot exists | remaining ratio ≥ 50 AND lot exists |
| Resume Sale: qty condition | available ≥ 21 EA | available ≥ 2 EA |
| Both Resume Sale conditions | AND (must satisfy both) | AND (must satisfy both) |
| Excluded zone codes | EXT MSC PRC RCV SHP WOR DMG RTN | Same as General |
| Valid warehouse | OA110 | OA110 |

---

## Implementation Notes

- Evaluate **Out of Stock → On Sale** transition only when the product's `currentStatus` is `OUT_OF_STOCK`
- Evaluate **On Sale → Out of Stock** transition only when the product's `currentStatus` is `ON_SALE`
- All transitions must be **idempotent** — re-running with the same inputs must not cause duplicate state changes
- Skipped products must be **logged** with a reason for auditability
- Scope (`GENERAL` vs `EXCLUSIVE`) must be determined **before** threshold evaluation
