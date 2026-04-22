type connection_config =
  {
    host : string;
    port : int;
    database : string;
    user : string;
    password : string;
  }

type company =
  {
    ticker : string;
    company_name : string option;
    industry_id : int option;
    isin : string option;
    fiscal_year_end_month : int option;
    number_employees : int option;
    business_summary : string option;
    market : string option;
    cik : int option;
    main_currency : string option;
  }

type financial_statement =
  {
    source_dataset : string;
    ticker : string;
    currency : string option;
    fiscal_year : int option;
    fiscal_period : string option;
    report_date : string option;
    publish_date : string option;
    restated_date : string option;
    revenue : float option;
    cost_of_revenue : float option;
    sga : float option;
    research_and_development : float option;
    income_da : float option;
    interest_expense_net : float option;
    abnormal_gains : float option;
    income_tax_net : float option;
    extraordinary_gains_losses : float option;
    net_income : float option;
    shares_basic : float option;
    shares_diluted : float option;
    cash_and_st_investments : float option;
    accounts_notes_receivables : float option;
    inventories : float option;
    total_current_assets : float option;
    ppe_net : float option;
    lt_investments_receivables : float option;
    other_lt_assets : float option;
    total_noncurrent_assets : float option;
    total_assets : float option;
    payables_accruals : float option;
    st_debt : float option;
    total_current_liabilities : float option;
    lt_debt : float option;
    total_noncurrent_liabilities : float option;
    total_liabilities : float option;
    share_capital_apic : float option;
    treasury_stock : float option;
    retained_earnings : float option;
    total_equity : float option;
    total_liabilities_equity : float option;
    cf_net_income_starting_line : float option;
    cf_da : float option;
    change_in_fixed_assets_intangibles : float option;
    change_in_working_capital : float option;
    change_in_accounts_receivable : float option;
    change_in_inventories : float option;
    change_in_accounts_payable : float option;
    change_in_other : float option;
    net_cash_operating_activities : float option;
    change_fixed_assets_intangibles : float option;
    net_change_lti : float option;
    net_cash_acquisitions_divestitures : float option;
    net_cash_investing_activities : float option;
    dividends_paid : float option;
    repayment_of_debt : float option;
    repurchase_of_equity : float option;
    net_cash_financing_activities : float option;
    net_change_cash : float option;
  }

type share_price =
  {
    ticker : string;
    price_date : string;
    open_price : float option;
    high_price : float option;
    low_price : float option;
    close_price : float option;
    adj_close : float option;
    volume : int option;
    dividend : float option;
    shares_outstanding : int option;
  }

type t

val connect : connection_config -> t
val connect_from_env : unit -> t
val close : t -> unit

val list_companies : ?limit:int -> t -> company list
val find_company : t -> ticker:string -> company option

val list_financial_statements
  :  ?source_dataset:string
  -> ?limit:int
  -> t
  -> ticker:string
  -> financial_statement list

val list_financial_statements_on_date
  :  ?source_dataset:string
  -> t
  -> ticker:string
  -> report_date:string
  -> financial_statement list

val list_share_prices
  :  ?date_from:string
  -> ?date_to:string
  -> ?limit:int
  -> t
  -> ticker:string
  -> share_price list

val find_share_price_on_date : t -> ticker:string -> price_date:string -> share_price option
