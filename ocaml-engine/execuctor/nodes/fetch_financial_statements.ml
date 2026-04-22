let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let value_of_field field statement =
  match String.lowercase_ascii field with
  | "currency" -> Option.map (fun value -> Node.String_value value) statement.Market_data_reader.currency
  | "fiscal_year" -> Option.map (fun value -> Node.normalize_number (float_of_int value)) statement.fiscal_year
  | "fiscal_period" -> Option.map (fun value -> Node.String_value value) statement.fiscal_period
  | "report_date" -> Option.map (fun value -> Node.String_value value) statement.report_date
  | "publish_date" -> Option.map (fun value -> Node.String_value value) statement.publish_date
  | "restated_date" -> Option.map (fun value -> Node.String_value value) statement.restated_date
  | "revenue" -> Option.map Node.normalize_number statement.revenue
  | "cost_of_revenue" -> Option.map Node.normalize_number statement.cost_of_revenue
  | "sga" -> Option.map Node.normalize_number statement.sga
  | "research_and_development" -> Option.map Node.normalize_number statement.research_and_development
  | "income_da" -> Option.map Node.normalize_number statement.income_da
  | "interest_expense_net" -> Option.map Node.normalize_number statement.interest_expense_net
  | "abnormal_gains" -> Option.map Node.normalize_number statement.abnormal_gains
  | "income_tax_net" -> Option.map Node.normalize_number statement.income_tax_net
  | "extraordinary_gains_losses" -> Option.map Node.normalize_number statement.extraordinary_gains_losses
  | "net_income" -> Option.map Node.normalize_number statement.net_income
  | "shares_basic" -> Option.map Node.normalize_number statement.shares_basic
  | "shares_diluted" -> Option.map Node.normalize_number statement.shares_diluted
  | "cash_and_st_investments" -> Option.map Node.normalize_number statement.cash_and_st_investments
  | "accounts_notes_receivables" -> Option.map Node.normalize_number statement.accounts_notes_receivables
  | "inventories" -> Option.map Node.normalize_number statement.inventories
  | "total_current_assets" -> Option.map Node.normalize_number statement.total_current_assets
  | "ppe_net" -> Option.map Node.normalize_number statement.ppe_net
  | "lt_investments_receivables" -> Option.map Node.normalize_number statement.lt_investments_receivables
  | "other_lt_assets" -> Option.map Node.normalize_number statement.other_lt_assets
  | "total_noncurrent_assets" -> Option.map Node.normalize_number statement.total_noncurrent_assets
  | "total_assets" -> Option.map Node.normalize_number statement.total_assets
  | "payables_accruals" -> Option.map Node.normalize_number statement.payables_accruals
  | "st_debt" -> Option.map Node.normalize_number statement.st_debt
  | "total_current_liabilities" -> Option.map Node.normalize_number statement.total_current_liabilities
  | "lt_debt" -> Option.map Node.normalize_number statement.lt_debt
  | "total_noncurrent_liabilities" -> Option.map Node.normalize_number statement.total_noncurrent_liabilities
  | "total_liabilities" -> Option.map Node.normalize_number statement.total_liabilities
  | "share_capital_apic" -> Option.map Node.normalize_number statement.share_capital_apic
  | "treasury_stock" -> Option.map Node.normalize_number statement.treasury_stock
  | "retained_earnings" -> Option.map Node.normalize_number statement.retained_earnings
  | "total_equity" -> Option.map Node.normalize_number statement.total_equity
  | "total_liabilities_equity" -> Option.map Node.normalize_number statement.total_liabilities_equity
  | "cf_net_income_starting_line" -> Option.map Node.normalize_number statement.cf_net_income_starting_line
  | "cf_da" -> Option.map Node.normalize_number statement.cf_da
  | "change_in_fixed_assets_intangibles" ->
      Option.map Node.normalize_number statement.change_in_fixed_assets_intangibles
  | "change_in_working_capital" -> Option.map Node.normalize_number statement.change_in_working_capital
  | "change_in_accounts_receivable" -> Option.map Node.normalize_number statement.change_in_accounts_receivable
  | "change_in_inventories" -> Option.map Node.normalize_number statement.change_in_inventories
  | "change_in_accounts_payable" -> Option.map Node.normalize_number statement.change_in_accounts_payable
  | "change_in_other" -> Option.map Node.normalize_number statement.change_in_other
  | "net_cash_operating_activities" -> Option.map Node.normalize_number statement.net_cash_operating_activities
  | "change_fixed_assets_intangibles" ->
      Option.map Node.normalize_number statement.change_fixed_assets_intangibles
  | "net_change_lti" -> Option.map Node.normalize_number statement.net_change_lti
  | "net_cash_acquisitions_divestitures" ->
      Option.map Node.normalize_number statement.net_cash_acquisitions_divestitures
  | "net_cash_investing_activities" -> Option.map Node.normalize_number statement.net_cash_investing_activities
  | "dividends_paid" -> Option.map Node.normalize_number statement.dividends_paid
  | "repayment_of_debt" -> Option.map Node.normalize_number statement.repayment_of_debt
  | "repurchase_of_equity" -> Option.map Node.normalize_number statement.repurchase_of_equity
  | "net_cash_financing_activities" -> Option.map Node.normalize_number statement.net_cash_financing_activities
  | "net_change_cash" -> Option.map Node.normalize_number statement.net_change_cash
  | _ -> None

let report_date_is_on_or_before ~target_date statement =
  match statement.Market_data_reader.report_date with
  | Some report_date -> String.compare report_date target_date <= 0
  | None -> false

let rec find_statement_as_of_date target_date = function
  | [] -> None
  | statement :: remaining ->
      if report_date_is_on_or_before ~target_date statement then Some statement
      else find_statement_as_of_date target_date remaining

let unsupported_field_message ~field ~ticker ~source_dataset ~report_date =
  "Unsupported or missing financial statement field `"
  ^ field
  ^ "` for ticker `"
  ^ ticker
  ^ "`, source dataset `"
  ^ source_dataset
  ^ "`, and as-of date `"
  ^ report_date
  ^ "`."

let missing_statement_message ~ticker ~source_dataset ~report_date =
  "No financial statement found on or before `"
  ^ report_date
  ^ "` for ticker `"
  ^ ticker
  ^ "`, source dataset `"
  ^ source_dataset
  ^ "`."

let run context =
  let* ticker = Executor.expect_string_field context "ticker" in
  let* source_dataset = Executor.expect_string_field context "sourceDataset" in
  let* field = Executor.expect_string_field context "field" in
  let unsupported_field_error report_date =
    Error
      (Executor.Message
         (unsupported_field_message ~field ~ticker ~source_dataset ~report_date))
  in
  match context.Executor.simulation with
  | Some simulation -> begin
      let* report_date = Executor.resolve_effective_date context ~explicit_field_name:"reportDate" in
      match simulation.lookup_financial_statement_value ~ticker ~source_dataset ~field ~report_date with
      | Some value -> Executor.single_output 0 value
      | None -> unsupported_field_error report_date
    end
  | None ->
      let* report_date = Executor.expect_string_field context "reportDate" in
      let reader = Market_data_reader.connect_from_env () in
      Fun.protect
        ~finally:(fun () -> Market_data_reader.close reader)
        (fun () ->
          let statements =
            Market_data_reader.list_financial_statements ~source_dataset reader ~ticker
          in
          match find_statement_as_of_date report_date statements with
          | None ->
              Error
                (Executor.Message
                   (missing_statement_message ~ticker ~source_dataset ~report_date))
          | Some statement ->
              match value_of_field field statement with
              | Some value -> Executor.single_output 0 value
              | None -> unsupported_field_error report_date)

let executor = { Executor.key = "fetch_financial_statements"; run }
