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

type t = { connection : Postgresql.connection }

let env_or ~default name =
  match Sys.getenv_opt name with Some value -> value | None -> default

let int_env_or ~default name =
  match Sys.getenv_opt name with Some value -> int_of_string value | None -> default

let connection_string config =
  Printf.sprintf
    "host=%s port=%d dbname=%s user=%s password=%s"
    config.host
    config.port
    config.database
    config.user
    config.password

let connect config = { connection = new Postgresql.connection ~conninfo:(connection_string config) () }

let connect_from_env () =
  connect
    {
      host = env_or ~default:"localhost" "PGHOST";
      port = int_env_or ~default:5432 "PGPORT";
      database = env_or ~default:"send" "PGDATABASE";
      user = env_or ~default:"send_user" "PGUSER";
      password = env_or ~default:"change_me" "PGPASSWORD";
    }

let close reader = reader.connection#finish

let is_null result row column = result#getisnull row column
let get_string_opt result row column = if is_null result row column then None else Some (result#getvalue row column)
let get_string result row column = result#getvalue row column
let get_int_opt result row column = Option.map int_of_string (get_string_opt result row column)
let get_float_opt result row column = Option.map float_of_string (get_string_opt result row column)

let get_limit_clause = function
  | None -> ""
  | Some limit -> Printf.sprintf " LIMIT %d" limit

let get_company_of_row result row =
  {
    ticker = get_string result row 0;
    company_name = get_string_opt result row 1;
    industry_id = get_int_opt result row 2;
    isin = get_string_opt result row 3;
    fiscal_year_end_month = get_int_opt result row 4;
    number_employees = get_int_opt result row 5;
    business_summary = get_string_opt result row 6;
    market = get_string_opt result row 7;
    cik = get_int_opt result row 8;
    main_currency = get_string_opt result row 9;
  }

let get_financial_statement_of_row result row =
  {
    source_dataset = get_string result row 0;
    ticker = get_string result row 1;
    currency = get_string_opt result row 2;
    fiscal_year = get_int_opt result row 3;
    fiscal_period = get_string_opt result row 4;
    report_date = get_string_opt result row 5;
    publish_date = get_string_opt result row 6;
    restated_date = get_string_opt result row 7;
    revenue = get_float_opt result row 8;
    cost_of_revenue = get_float_opt result row 9;
    sga = get_float_opt result row 10;
    research_and_development = get_float_opt result row 11;
    income_da = get_float_opt result row 12;
    interest_expense_net = get_float_opt result row 13;
    abnormal_gains = get_float_opt result row 14;
    income_tax_net = get_float_opt result row 15;
    extraordinary_gains_losses = get_float_opt result row 16;
    net_income = get_float_opt result row 17;
    shares_basic = get_float_opt result row 18;
    shares_diluted = get_float_opt result row 19;
    cash_and_st_investments = get_float_opt result row 20;
    accounts_notes_receivables = get_float_opt result row 21;
    inventories = get_float_opt result row 22;
    total_current_assets = get_float_opt result row 23;
    ppe_net = get_float_opt result row 24;
    lt_investments_receivables = get_float_opt result row 25;
    other_lt_assets = get_float_opt result row 26;
    total_noncurrent_assets = get_float_opt result row 27;
    total_assets = get_float_opt result row 28;
    payables_accruals = get_float_opt result row 29;
    st_debt = get_float_opt result row 30;
    total_current_liabilities = get_float_opt result row 31;
    lt_debt = get_float_opt result row 32;
    total_noncurrent_liabilities = get_float_opt result row 33;
    total_liabilities = get_float_opt result row 34;
    share_capital_apic = get_float_opt result row 35;
    treasury_stock = get_float_opt result row 36;
    retained_earnings = get_float_opt result row 37;
    total_equity = get_float_opt result row 38;
    total_liabilities_equity = get_float_opt result row 39;
    cf_net_income_starting_line = get_float_opt result row 40;
    cf_da = get_float_opt result row 41;
    change_in_fixed_assets_intangibles = get_float_opt result row 42;
    change_in_working_capital = get_float_opt result row 43;
    change_in_accounts_receivable = get_float_opt result row 44;
    change_in_inventories = get_float_opt result row 45;
    change_in_accounts_payable = get_float_opt result row 46;
    change_in_other = get_float_opt result row 47;
    net_cash_operating_activities = get_float_opt result row 48;
    change_fixed_assets_intangibles = get_float_opt result row 49;
    net_change_lti = get_float_opt result row 50;
    net_cash_acquisitions_divestitures = get_float_opt result row 51;
    net_cash_investing_activities = get_float_opt result row 52;
    dividends_paid = get_float_opt result row 53;
    repayment_of_debt = get_float_opt result row 54;
    repurchase_of_equity = get_float_opt result row 55;
    net_cash_financing_activities = get_float_opt result row 56;
    net_change_cash = get_float_opt result row 57;
  }

let get_share_price_of_row result row =
  {
    ticker = get_string result row 0;
    price_date = get_string result row 1;
    open_price = get_float_opt result row 2;
    high_price = get_float_opt result row 3;
    low_price = get_float_opt result row 4;
    close_price = get_float_opt result row 5;
    adj_close = get_float_opt result row 6;
    volume = get_int_opt result row 7;
    dividend = get_float_opt result row 8;
    shares_outstanding = get_int_opt result row 9;
  }

let rows_to_list result decode =
  let row_count = result#ntuples in
  let rec loop index acc =
    if index < 0 then acc else loop (index - 1) (decode result index :: acc)
  in
  loop (row_count - 1) []

let list_companies ?limit reader =
  let sql =
    "SELECT ticker, company_name, industry_id, isin, fiscal_year_end_month, "
    ^ "number_employees, business_summary, market, cik, main_currency "
    ^ "FROM us_companies ORDER BY ticker"
    ^ get_limit_clause limit
  in
  reader.connection#exec sql |> fun result -> rows_to_list result get_company_of_row

let find_company reader ~ticker =
  let escaped_ticker = reader.connection#escape_string ticker in
  let sql =
    Printf.sprintf
      "SELECT ticker, company_name, industry_id, isin, fiscal_year_end_month, \
       number_employees, business_summary, market, cik, main_currency \
       FROM us_companies WHERE ticker = '%s' LIMIT 1"
      escaped_ticker
  in
  match rows_to_list (reader.connection#exec sql) get_company_of_row with
  | [] -> None
  | company :: _ -> Some company

let list_financial_statements ?source_dataset ?limit reader ~ticker =
  let escaped_ticker = reader.connection#escape_string ticker in
  let source_dataset_filter =
    match source_dataset with
    | None -> ""
    | Some value ->
        Printf.sprintf " AND source_dataset = '%s'" (reader.connection#escape_string value)
  in
  let sql =
    "SELECT source_dataset, ticker, currency, fiscal_year, fiscal_period, report_date::text, "
    ^ "publish_date::text, restated_date::text, revenue, cost_of_revenue, sga, "
    ^ "research_and_development, income_da, interest_expense_net, abnormal_gains, "
    ^ "income_tax_net, extraordinary_gains_losses, net_income, shares_basic, shares_diluted, "
    ^ "cash_and_st_investments, accounts_notes_receivables, inventories, total_current_assets, "
    ^ "ppe_net, lt_investments_receivables, other_lt_assets, total_noncurrent_assets, "
    ^ "total_assets, payables_accruals, st_debt, total_current_liabilities, lt_debt, "
    ^ "total_noncurrent_liabilities, total_liabilities, share_capital_apic, treasury_stock, "
    ^ "retained_earnings, total_equity, total_liabilities_equity, cf_net_income_starting_line, "
    ^ "cf_da, change_in_fixed_assets_intangibles, change_in_working_capital, "
    ^ "change_in_accounts_receivable, change_in_inventories, change_in_accounts_payable, "
    ^ "change_in_other, net_cash_operating_activities, change_fixed_assets_intangibles, "
    ^ "net_change_lti, net_cash_acquisitions_divestitures, net_cash_investing_activities, "
    ^ "dividends_paid, repayment_of_debt, repurchase_of_equity, net_cash_financing_activities, "
    ^ "net_change_cash FROM us_financial_statements WHERE ticker = '"
    ^ escaped_ticker
    ^ "'"
    ^ source_dataset_filter
    ^ " ORDER BY report_date DESC NULLS LAST, fiscal_year DESC NULLS LAST"
    ^ get_limit_clause limit
  in
  reader.connection#exec sql |> fun result -> rows_to_list result get_financial_statement_of_row

let list_financial_statements_on_date ?source_dataset reader ~ticker ~report_date =
  let escaped_ticker = reader.connection#escape_string ticker in
  let escaped_report_date = reader.connection#escape_string report_date in
  let source_dataset_filter =
    match source_dataset with
    | None -> ""
    | Some value ->
        Printf.sprintf " AND source_dataset = '%s'" (reader.connection#escape_string value)
  in
  let sql =
    "SELECT source_dataset, ticker, currency, fiscal_year, fiscal_period, report_date::text, "
    ^ "publish_date::text, restated_date::text, revenue, cost_of_revenue, sga, "
    ^ "research_and_development, income_da, interest_expense_net, abnormal_gains, "
    ^ "income_tax_net, extraordinary_gains_losses, net_income, shares_basic, shares_diluted, "
    ^ "cash_and_st_investments, accounts_notes_receivables, inventories, total_current_assets, "
    ^ "ppe_net, lt_investments_receivables, other_lt_assets, total_noncurrent_assets, "
    ^ "total_assets, payables_accruals, st_debt, total_current_liabilities, lt_debt, "
    ^ "total_noncurrent_liabilities, total_liabilities, share_capital_apic, treasury_stock, "
    ^ "retained_earnings, total_equity, total_liabilities_equity, cf_net_income_starting_line, "
    ^ "cf_da, change_in_fixed_assets_intangibles, change_in_working_capital, "
    ^ "change_in_accounts_receivable, change_in_inventories, change_in_accounts_payable, "
    ^ "change_in_other, net_cash_operating_activities, change_fixed_assets_intangibles, "
    ^ "net_change_lti, net_cash_acquisitions_divestitures, net_cash_investing_activities, "
    ^ "dividends_paid, repayment_of_debt, repurchase_of_equity, net_cash_financing_activities, "
    ^ "net_change_cash FROM us_financial_statements WHERE ticker = '"
    ^ escaped_ticker
    ^ "' AND report_date = '"
    ^ escaped_report_date
    ^ "'"
    ^ source_dataset_filter
    ^ " ORDER BY source_dataset"
  in
  reader.connection#exec sql |> fun result -> rows_to_list result get_financial_statement_of_row

let list_share_prices ?date_from ?date_to ?limit reader ~ticker =
  let escaped_ticker = reader.connection#escape_string ticker in
  let date_from_filter =
    match date_from with
    | None -> ""
    | Some value ->
        Printf.sprintf " AND price_date >= '%s'" (reader.connection#escape_string value)
  in
  let date_to_filter =
    match date_to with
    | None -> ""
    | Some value ->
        Printf.sprintf " AND price_date <= '%s'" (reader.connection#escape_string value)
  in
  let sql =
    "SELECT ticker, price_date::text, open, high, low, close, adj_close, volume, dividend, "
    ^ "shares_outstanding FROM us_shareprices_daily WHERE ticker = '"
    ^ escaped_ticker
    ^ "'"
    ^ date_from_filter
    ^ date_to_filter
    ^ " ORDER BY price_date DESC"
    ^ get_limit_clause limit
  in
  reader.connection#exec sql |> fun result -> rows_to_list result get_share_price_of_row

let find_share_price_on_date reader ~ticker ~price_date =
  let escaped_ticker = reader.connection#escape_string ticker in
  let escaped_price_date = reader.connection#escape_string price_date in
  let sql =
    "SELECT ticker, price_date::text, open, high, low, close, adj_close, volume, dividend, "
    ^ "shares_outstanding FROM us_shareprices_daily WHERE ticker = '"
    ^ escaped_ticker
    ^ "' AND price_date = '"
    ^ escaped_price_date
    ^ "' LIMIT 1"
  in
  match rows_to_list (reader.connection#exec sql) get_share_price_of_row with
  | [] -> None
  | share_price :: _ -> Some share_price
