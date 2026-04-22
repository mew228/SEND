let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let number_of_field field statement =
  match String.lowercase_ascii field with
  | "revenue" -> statement.Market_data_reader.revenue
  | "net_income" -> statement.net_income
  | "shares_basic" -> statement.shares_basic
  | "shares_diluted" -> statement.shares_diluted
  | "cash_and_st_investments" -> statement.cash_and_st_investments
  | "total_current_assets" -> statement.total_current_assets
  | "total_assets" -> statement.total_assets
  | "total_current_liabilities" -> statement.total_current_liabilities
  | "total_liabilities" -> statement.total_liabilities
  | "total_equity" -> statement.total_equity
  | "net_cash_operating_activities" -> statement.net_cash_operating_activities
  | "net_cash_investing_activities" -> statement.net_cash_investing_activities
  | "net_cash_financing_activities" -> statement.net_cash_financing_activities
  | "net_change_cash" -> statement.net_change_cash
  | _ -> None

let report_date_is_on_or_before ~target_date statement =
  match statement.Market_data_reader.report_date with
  | Some report_date -> String.compare report_date target_date <= 0
  | None -> false

let rec find_value_as_of_date field target_date = function
  | [] -> None
  | statement :: remaining -> (
      if report_date_is_on_or_before ~target_date statement then
        match number_of_field field statement with
        | Some _ as value -> value
        | None -> find_value_as_of_date field target_date remaining
      else
        find_value_as_of_date field target_date remaining)

let run context =
  let* ticker = Executor.expect_string_field context "ticker" in
  let* field = Executor.expect_string_field context "field" in
  match context.Executor.simulation with
  | Some simulation -> begin
      let* report_date = Executor.resolve_effective_date context ~explicit_field_name:"reportDate" in
      match simulation.lookup_fundamentals_value ~ticker ~field ~report_date with
      | Some value -> Executor.single_output 0 (Node.normalize_number value)
      | None ->
          Error
            (Executor.Message
               ("No fundamentals value found on or before `"
               ^ report_date
               ^ "` for field `"
               ^ field
               ^ "`, ticker `"
               ^ ticker
               ^ "`." ))
    end
  | None ->
      let* report_date = Executor.expect_string_field context "reportDate" in
      let reader = Market_data_reader.connect_from_env () in
      Fun.protect
        ~finally:(fun () -> Market_data_reader.close reader)
        (fun () ->
          let statements = Market_data_reader.list_financial_statements reader ~ticker in
          match find_value_as_of_date field report_date statements with
          | Some value -> Executor.single_output 0 (Node.normalize_number value)
          | None ->
              Error
                (Executor.Message
                   ("No fundamentals value found on or before `"
                   ^ report_date
                   ^ "` for field `"
                   ^ field
                   ^ "`, ticker `"
                   ^ ticker
                   ^ "`.")))

let executor = { Executor.key = "fetch_fundamentals"; run }
