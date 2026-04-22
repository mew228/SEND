CREATE TABLE IF NOT EXISTS us_financial_statements (
  id                                   BIGSERIAL PRIMARY KEY,
  source_dataset                       TEXT    NOT NULL,
  ticker                               TEXT    NOT NULL,
  currency                             TEXT,
  fiscal_year                          INTEGER,
  fiscal_period                        TEXT,
  report_date                          DATE,
  publish_date                         DATE,
  restated_date                        DATE,
  revenue                              DOUBLE PRECISION,
  cost_of_revenue                      DOUBLE PRECISION,
  sga                                  DOUBLE PRECISION,
  research_and_development             DOUBLE PRECISION,
  income_da                            DOUBLE PRECISION,
  interest_expense_net                 DOUBLE PRECISION,
  abnormal_gains                       DOUBLE PRECISION,
  income_tax_net                       DOUBLE PRECISION,
  extraordinary_gains_losses           DOUBLE PRECISION,
  net_income                           DOUBLE PRECISION,
  shares_basic                         DOUBLE PRECISION,
  shares_diluted                       DOUBLE PRECISION,
  cash_and_st_investments              DOUBLE PRECISION,
  accounts_notes_receivables           DOUBLE PRECISION,
  inventories                          DOUBLE PRECISION,
  total_current_assets                 DOUBLE PRECISION,
  ppe_net                              DOUBLE PRECISION,
  lt_investments_receivables           DOUBLE PRECISION,
  other_lt_assets                      DOUBLE PRECISION,
  total_noncurrent_assets              DOUBLE PRECISION,
  total_assets                         DOUBLE PRECISION,
  payables_accruals                    DOUBLE PRECISION,
  st_debt                              DOUBLE PRECISION,
  total_current_liabilities            DOUBLE PRECISION,
  lt_debt                              DOUBLE PRECISION,
  total_noncurrent_liabilities         DOUBLE PRECISION,
  total_liabilities                    DOUBLE PRECISION,
  share_capital_apic                   DOUBLE PRECISION,
  treasury_stock                       DOUBLE PRECISION,
  retained_earnings                    DOUBLE PRECISION,
  total_equity                         DOUBLE PRECISION,
  total_liabilities_equity             DOUBLE PRECISION,
  cf_net_income_starting_line          DOUBLE PRECISION,
  cf_da                                DOUBLE PRECISION,
  change_in_fixed_assets_intangibles   DOUBLE PRECISION,
  change_in_working_capital            DOUBLE PRECISION,
  change_in_accounts_receivable        DOUBLE PRECISION,
  change_in_inventories                DOUBLE PRECISION,
  change_in_accounts_payable           DOUBLE PRECISION,
  change_in_other                      DOUBLE PRECISION,
  net_cash_operating_activities        DOUBLE PRECISION,
  change_fixed_assets_intangibles      DOUBLE PRECISION,
  net_change_lti                       DOUBLE PRECISION,
  net_cash_acquisitions_divestitures   DOUBLE PRECISION,
  net_cash_investing_activities        DOUBLE PRECISION,
  dividends_paid                       DOUBLE PRECISION,
  repayment_of_debt                    DOUBLE PRECISION,
  repurchase_of_equity                 DOUBLE PRECISION,
  net_cash_financing_activities        DOUBLE PRECISION,
  net_change_cash                      DOUBLE PRECISION,
  CONSTRAINT us_financial_statements_source_dataset_chk CHECK (
    source_dataset IN ('us-income-quarterly', 'us-balance-quarterly', 'us-cashflow-quarterly')
  )
);

CREATE INDEX IF NOT EXISTS us_financial_statements_source_ticker_idx
  ON us_financial_statements (source_dataset, ticker);

CREATE INDEX IF NOT EXISTS us_financial_statements_ticker_report_date_idx
  ON us_financial_statements (ticker, report_date DESC);

CREATE INDEX IF NOT EXISTS us_financial_statements_publish_date_idx
  ON us_financial_statements (publish_date);

CREATE UNIQUE INDEX IF NOT EXISTS us_financial_statements_dataset_grain_uidx
  ON us_financial_statements (source_dataset, ticker, fiscal_year, fiscal_period, report_date);
