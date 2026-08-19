-- Preserve a single serial token number across every service and date.
-- The negative interim value avoids uniqueness conflicts while existing data is renumbered.
UPDATE tokens SET token_number = -id;
UPDATE tokens SET token_number = -token_number;
