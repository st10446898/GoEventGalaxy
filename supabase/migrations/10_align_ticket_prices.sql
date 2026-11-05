-- Migration 10: Align ticket prices with event starting_price_cents
-- Table name: ticket_types, price column: price_cents.
-- Events table column: starting_price_cents.

UPDATE ticket_types tt
SET price_cents = e.starting_price_cents
FROM events e
WHERE tt.event_id = e.id AND tt.price_cents != e.starting_price_cents;
