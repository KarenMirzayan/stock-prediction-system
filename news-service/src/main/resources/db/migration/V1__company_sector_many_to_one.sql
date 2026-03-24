-- Migrate company-sector relationship from many-to-many to many-to-one.
-- Picks the first sector per company from the join table.

ALTER TABLE companies ADD COLUMN IF NOT EXISTS sector_id BIGINT;

-- Migrate: pick one sector per company from the join table
UPDATE companies c
SET sector_id = cs.sector_id
FROM (
    SELECT DISTINCT ON (company_id) company_id, sector_id
    FROM company_sectors
    ORDER BY company_id, sector_id
) cs
WHERE c.id = cs.company_id
  AND c.sector_id IS NULL;

-- Add foreign key constraint
ALTER TABLE companies
    ADD CONSTRAINT fk_company_sector
    FOREIGN KEY (sector_id) REFERENCES economy_sectors(id);

-- Drop the old join table
DROP TABLE IF EXISTS company_sectors;
