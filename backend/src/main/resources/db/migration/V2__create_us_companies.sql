CREATE TABLE IF NOT EXISTS us_companies (
  ticker                  TEXT PRIMARY KEY,
  company_name            TEXT,
  industry_id             BIGINT,
  isin                    TEXT,
  fiscal_year_end_month   INTEGER,
  number_employees        BIGINT,
  business_summary        TEXT,
  market                  TEXT,
  cik                     BIGINT,
  main_currency           TEXT
);

CREATE INDEX IF NOT EXISTS us_companies_market_idx
  ON us_companies (market);

CREATE INDEX IF NOT EXISTS us_companies_industry_id_idx
  ON us_companies (industry_id);

CREATE INDEX IF NOT EXISTS us_companies_cik_idx
  ON us_companies (cik);
