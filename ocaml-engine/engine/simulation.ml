module String_set = Stdlib.Set.Make (String)
module String_map = Stdlib.Map.Make (String)

type position = {
  ticker : string;
  quantity : float;
  average_cost : float;
}

type trade_event = {
  node_id : string;
  action : string;
  ticker : string;
  size_mode : string;
  requested_amount : float;
  filled_shares : float;
  fill_price : float;
  cash_before : float;
  cash_after : float;
  realized_pnl : float;
}

type balance_snapshot = {
  cash : float;
  market_value : float;
  equity : float;
  realized_pnl : float;
  unrealized_pnl : float;
}

type market_cache = {
  prices : Market_data_reader.share_price String_map.t String_map.t;
  companies : Market_data_reader.company String_map.t;
  fundamentals : Market_data_reader.financial_statement list String_map.t;
  financials : Market_data_reader.financial_statement list String_map.t String_map.t;
  calendar : string list;
}

type simulation_state = {
  initial_cash : float;
  mutable current_date : string;
  mutable cash_balance : float;
  mutable positions_by_ticker : position String_map.t;
  mutable realized_pnl : float;
  mutable warnings : string list;
  mutable previous_day_outputs : Node.value Node.Port_ref_map.t;
  mutable trade_count : int;
}

type day_state = {
  mutable warnings : string list;
  mutable errors : string list;
  mutable trades : trade_event list;
}

let hidden_constant_node_types =
  [ "const_number"; "const_bool"; "const_string" ]

let is_hidden_constant_node node_type =
  List.mem node_type hidden_constant_node_types

let value_to_json = function
  | Node.Number_value value ->
      if Float.is_integer value then `Int (int_of_float value) else `Float value
  | Node.Bool_value value -> `Bool value
  | Node.String_value value -> `String value

let float_to_json value =
  if Float.is_integer value then `Int (int_of_float value) else `Float value

let option_to_json value_to_json = function
  | Some value -> value_to_json value
  | None -> `Null

let string_of_node_id node =
  Node_id.to_string node.Node.id

let record_warning state day node message =
  let warning =
    state.current_date ^ " [" ^ string_of_node_id node ^ "] " ^ message
  in
  day.warnings <- day.warnings @ [ warning ];
  state.warnings <- state.warnings @ [ warning ]

let parse_date value =
  try Scanf.sscanf value "%d-%d-%d" (fun year month day -> (year, month, day))
  with _ -> failwith ("Invalid ISO date: " ^ value)

let add_days value offset =
  let year, month, day = parse_date value in
  let time, _ =
    Unix.mktime
      {
        Unix.tm_sec = 0;
        tm_min = 0;
        tm_hour = 12;
        tm_mday = day;
        tm_mon = month - 1;
        tm_year = year - 1900;
        tm_wday = 0;
        tm_yday = 0;
        tm_isdst = false;
      }
  in
  let shifted = Unix.gmtime (time +. (float_of_int offset *. 86_400.0)) in
  Printf.sprintf
    "%04d-%02d-%02d"
    (shifted.tm_year + 1900)
    (shifted.tm_mon + 1)
    shifted.tm_mday

let price_field_value field share_price =
  match String.lowercase_ascii field with
  | "open" -> share_price.Market_data_reader.open_price
  | "high" -> share_price.high_price
  | "low" -> share_price.low_price
  | "close" -> share_price.close_price
  | "adj_close"
  | "adjusted_close" -> share_price.adj_close
  | "dividend" -> share_price.dividend
  | "volume" -> Option.map float_of_int share_price.volume
  | "shares_outstanding" -> Option.map float_of_int share_price.shares_outstanding
  | _ -> None

let company_value field company =
  match String.lowercase_ascii field with
  | "company_name" -> Option.map (fun value -> Node.String_value value) company.Market_data_reader.company_name
  | "industry_id" -> Option.map (fun value -> Node.normalize_number (float_of_int value)) company.industry_id
  | "isin" -> Option.map (fun value -> Node.String_value value) company.isin
  | "fiscal_year_end_month" ->
      Option.map (fun value -> Node.normalize_number (float_of_int value)) company.fiscal_year_end_month
  | "number_employees" ->
      Option.map (fun value -> Node.normalize_number (float_of_int value)) company.number_employees
  | "business_summary" -> Option.map (fun value -> Node.String_value value) company.business_summary
  | "market" -> Option.map (fun value -> Node.String_value value) company.market
  | "cik" -> Option.map (fun value -> Node.normalize_number (float_of_int value)) company.cik
  | "main_currency" -> Option.map (fun value -> Node.String_value value) company.main_currency
  | _ -> None

let financial_statement_value field statement =
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
  | "change_fixed_assets_intangibles" -> Option.map Node.normalize_number statement.change_fixed_assets_intangibles
  | "net_change_lti" -> Option.map Node.normalize_number statement.net_change_lti
  | "net_cash_acquisitions_divestitures" -> Option.map Node.normalize_number statement.net_cash_acquisitions_divestitures
  | "net_cash_investing_activities" -> Option.map Node.normalize_number statement.net_cash_investing_activities
  | "dividends_paid" -> Option.map Node.normalize_number statement.dividends_paid
  | "repayment_of_debt" -> Option.map Node.normalize_number statement.repayment_of_debt
  | "repurchase_of_equity" -> Option.map Node.normalize_number statement.repurchase_of_equity
  | "net_cash_financing_activities" -> Option.map Node.normalize_number statement.net_cash_financing_activities
  | "net_change_cash" -> Option.map Node.normalize_number statement.net_change_cash
  | _ -> None

let fundamentals_value field statement =
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

let find_statement_as_of_date statements target_date =
  List.find_opt (report_date_is_on_or_before ~target_date) statements

let find_fundamentals_as_of_date statements field target_date =
  statements
  |> List.find_map (fun statement ->
         if report_date_is_on_or_before ~target_date statement then
           fundamentals_value field statement
         else
           None)

let collect_string_field node field_name =
  match Node.find_data_field node field_name with
  | Some { value = Node.String_value value; _ } when not (String.equal value "") -> Some value
  | _ -> None

let collect_number_field node field_name =
  match Node.find_data_field node field_name with
  | Some { value = Node.Number_value value; _ } -> Some value
  | _ -> None

let preload_market_cache graph ~start_date ~end_date =
  let tickers, financial_sources =
    Graph.nodes graph
    |> List.fold_left
         (fun (tickers, sources) (node : Node.t) ->
           match collect_string_field node "ticker" with
           | None -> (tickers, sources)
           | Some ticker ->
               let tickers = String_set.add ticker tickers in
               let sources =
                 if String.equal node.node_type "fetch_financial_statements" then
                   match collect_string_field node "sourceDataset" with
                   | Some source_dataset ->
                       let existing =
                         match String_map.find_opt ticker sources with
                         | Some values -> values
                         | None -> String_set.empty
                       in
                       String_map.add ticker (String_set.add source_dataset existing) sources
                   | None -> sources
                 else
                   sources
               in
               (tickers, sources))
         (String_set.empty, String_map.empty)
  in
  if String_set.is_empty tickers then
    Error [ "Simulation requires at least one node-configured ticker to preload market data." ]
  else
    let reader = Market_data_reader.connect_from_env () in
    Fun.protect
      ~finally:(fun () -> Market_data_reader.close reader)
      (fun () ->
        let prices, companies, fundamentals, financials, calendar =
          String_set.elements tickers
          |> List.fold_left
               (fun (prices, companies, fundamentals, financials, calendar) ticker ->
                 let price_rows =
                   Market_data_reader.list_share_prices
                     ~date_from:start_date
                     ~date_to:end_date
                     reader
                     ~ticker
                 in
                 let prices_by_date =
                   List.fold_left
                     (fun current share_price ->
                       String_map.add share_price.Market_data_reader.price_date share_price current)
                     String_map.empty
                     price_rows
                 in
                 let calendar =
                   List.fold_left
                     (fun current share_price -> String_set.add share_price.Market_data_reader.price_date current)
                     calendar
                     price_rows
                 in
                 let companies =
                   match Market_data_reader.find_company reader ~ticker with
                   | Some company -> String_map.add ticker company companies
                   | None -> companies
                 in
                 let fundamentals =
                   String_map.add
                     ticker
                     (Market_data_reader.list_financial_statements reader ~ticker)
                     fundamentals
                 in
                 let financial_rows =
                   match String_map.find_opt ticker financial_sources with
                   | None -> String_map.empty
                   | Some source_datasets ->
                       source_datasets
                       |> String_set.elements
                       |> List.fold_left
                            (fun current source_dataset ->
                              let statements =
                                Market_data_reader.list_financial_statements
                                  ~source_dataset
                                  reader
                                  ~ticker
                              in
                              String_map.add source_dataset statements current)
                            String_map.empty
                 in
                 ( String_map.add ticker prices_by_date prices,
                   companies,
                   fundamentals,
                   String_map.add ticker financial_rows financials,
                   calendar ))
               (String_map.empty, String_map.empty, String_map.empty, String_map.empty, String_set.empty)
        in
        let calendar = String_set.elements calendar |> List.sort String.compare in
        if calendar = [] then
          Error [ "No share price data was found for the requested tickers and date range." ]
        else
          Ok { prices; companies; fundamentals; financials; calendar })

let lookup_price_row cache ~ticker ~date =
  match String_map.find_opt ticker cache.prices with
  | None -> None
  | Some prices_by_date -> String_map.find_opt date prices_by_date

let lookup_price_value cache ~ticker ~field ~date =
  match lookup_price_row cache ~ticker ~date with
  | Some share_price -> price_field_value field share_price
  | None -> None

let lookup_company_value cache ~ticker ~field =
  match String_map.find_opt ticker cache.companies with
  | Some company -> company_value field company
  | None -> None

let lookup_fundamentals_value cache ~ticker ~field ~report_date =
  match String_map.find_opt ticker cache.fundamentals with
  | Some statements -> find_fundamentals_as_of_date statements field report_date
  | None -> None

let lookup_financial_statement_value cache ~ticker ~source_dataset ~field ~report_date =
  match String_map.find_opt ticker cache.financials with
  | None -> None
  | Some statements_by_source -> (
      match String_map.find_opt source_dataset statements_by_source with
      | Some statements -> (
          match find_statement_as_of_date statements report_date with
          | Some statement -> financial_statement_value field statement
          | None -> None)
      | None -> None)

let effective_date_of_context state (context : Executor.run_context) ~explicit_field_name =
  let binding_mode =
    match Node.find_data_field context.node "dateBindingMode" with
    | Some { value = Node.String_value value; _ } -> String.lowercase_ascii value
    | _ -> "explicit_date"
  in
  match binding_mode with
  | "simulation_day" -> Ok state.current_date
  | "simulation_day_offset" ->
      let day_offset =
        match collect_number_field context.node "dayOffset" with
        | Some value -> int_of_float value
        | None -> 0
      in
      Ok (add_days state.current_date day_offset)
  | "explicit_date"
  | _ -> Executor.expect_string_field context explicit_field_name

let compute_requested_shares size_mode requested_amount fill_price =
  match String.lowercase_ascii size_mode with
  | "shares" -> requested_amount
  | "dollars" -> requested_amount /. fill_price
  | _ -> requested_amount

let execute_trade state cache day ~node ~action ~ticker ~size_mode ~requested_amount ~price_field =
  let cash_before = state.cash_balance in
  let action = String.lowercase_ascii action in
  match lookup_price_value cache ~ticker ~field:price_field ~date:state.current_date with
  | None ->
      record_warning state day node ("No `" ^ price_field ^ "` price is available for ticker `" ^ ticker ^ "`.");
      Ok
        {
          Executor.executed = false;
          filled_shares = 0.0;
          fill_price = None;
          cash_before;
          cash_after = cash_before;
          realized_pnl = 0.0;
        }
  | Some fill_price when fill_price <= 0.0 ->
      record_warning state day node ("Non-positive trade price for ticker `" ^ ticker ^ "`.");
      Ok
        {
          Executor.executed = false;
          filled_shares = 0.0;
          fill_price = Some fill_price;
          cash_before;
          cash_after = cash_before;
          realized_pnl = 0.0;
        }
  | Some fill_price ->
      let requested_shares = compute_requested_shares size_mode requested_amount fill_price in
      if requested_shares <= 0.0 then (
        record_warning state day node "Trade size must be greater than zero.";
        Ok
          {
            Executor.executed = false;
            filled_shares = 0.0;
            fill_price = Some fill_price;
            cash_before;
            cash_after = cash_before;
            realized_pnl = 0.0;
          })
      else
        match action with
        | "buy" ->
            let trade_cost = requested_shares *. fill_price in
            if trade_cost > cash_before +. 1e-9 then (
              record_warning state day node ("Insufficient cash to buy `" ^ ticker ^ "`.");
              Ok
                {
                  Executor.executed = false;
                  filled_shares = 0.0;
                  fill_price = Some fill_price;
                  cash_before;
                  cash_after = cash_before;
                  realized_pnl = 0.0;
                })
            else
              let existing_position =
                match String_map.find_opt ticker state.positions_by_ticker with
                | Some position -> position
                | None -> { ticker; quantity = 0.0; average_cost = 0.0 }
              in
              let updated_quantity = existing_position.quantity +. requested_shares in
              let updated_average_cost =
                ((existing_position.quantity *. existing_position.average_cost) +. trade_cost) /. updated_quantity
              in
              state.cash_balance <- cash_before -. trade_cost;
              state.positions_by_ticker <-
                String_map.add
                  ticker
                  { ticker; quantity = updated_quantity; average_cost = updated_average_cost }
                  state.positions_by_ticker;
              state.trade_count <- state.trade_count + 1;
              day.trades <-
                day.trades
                @ [
                    {
                      node_id = string_of_node_id node;
                      action;
                      ticker;
                      size_mode;
                      requested_amount;
                      filled_shares = requested_shares;
                      fill_price;
                      cash_before;
                      cash_after = state.cash_balance;
                      realized_pnl = 0.0;
                    };
                  ];
              Ok
                {
                  Executor.executed = true;
                  filled_shares = requested_shares;
                  fill_price = Some fill_price;
                  cash_before;
                  cash_after = state.cash_balance;
                  realized_pnl = 0.0;
                }
        | "sell" -> (
            match String_map.find_opt ticker state.positions_by_ticker with
            | None ->
                record_warning state day node ("No open position is available to sell for `" ^ ticker ^ "`.");
                Ok
                  {
                    Executor.executed = false;
                    filled_shares = 0.0;
                    fill_price = Some fill_price;
                    cash_before;
                    cash_after = cash_before;
                    realized_pnl = 0.0;
                  }
            | Some position ->
                if requested_shares > position.quantity +. 1e-9 then (
                  record_warning state day node ("Insufficient shares to sell ticker `" ^ ticker ^ "`.");
                  Ok
                    {
                      Executor.executed = false;
                      filled_shares = 0.0;
                      fill_price = Some fill_price;
                      cash_before;
                      cash_after = cash_before;
                      realized_pnl = 0.0;
                    })
                else
                  let proceeds = requested_shares *. fill_price in
                  let realized_pnl = requested_shares *. (fill_price -. position.average_cost) in
                  let remaining_quantity = position.quantity -. requested_shares in
                  state.cash_balance <- cash_before +. proceeds;
                  state.realized_pnl <- state.realized_pnl +. realized_pnl;
                  state.positions_by_ticker <-
                    if remaining_quantity <= 1e-9 then
                      String_map.remove ticker state.positions_by_ticker
                    else
                      String_map.add
                        ticker
                        { ticker; quantity = remaining_quantity; average_cost = position.average_cost }
                        state.positions_by_ticker;
                  state.trade_count <- state.trade_count + 1;
                  day.trades <-
                    day.trades
                    @ [
                        {
                          node_id = string_of_node_id node;
                          action;
                          ticker;
                          size_mode;
                          requested_amount;
                          filled_shares = requested_shares;
                          fill_price;
                          cash_before;
                          cash_after = state.cash_balance;
                          realized_pnl;
                        };
                      ];
                  Ok
                    {
                      Executor.executed = true;
                      filled_shares = requested_shares;
                      fill_price = Some fill_price;
                      cash_before;
                      cash_after = state.cash_balance;
                      realized_pnl;
                    })
        | _ -> Error (Executor.Message ("Unsupported trade action `" ^ action ^ "`."))

let collect_output_results graph =
  Graph.nodes graph
  |> List.filter_map (fun (node : Node.t) ->
         if is_hidden_constant_node node.node_type then
           None
         else
           match Graph.find_node_spec graph node.node_type with
           | None -> None
           | Some spec ->
               let outputs =
                 spec.output_ports
                 |> List.filter_map (fun (port : Node.port_spec) ->
                        let port_ref = Node.make_port_ref ~node_id:node.id ~port_index:port.index in
                        match Graph.port_value graph port_ref with
                        | None -> None
                        | Some value -> Some (port.name, value_to_json value))
               in
               if outputs = [] then None else Some (Node_id.to_string node.id, `Assoc outputs))
  |> fun entries -> `Assoc entries

let collect_output_snapshot graph =
  Graph.nodes graph
  |> List.fold_left
       (fun current (node : Node.t) ->
         if is_hidden_constant_node node.node_type then
           current
         else
           match Graph.find_node_spec graph node.node_type with
           | None -> current
           | Some spec ->
               List.fold_left
                 (fun current (port : Node.port_spec) ->
                   let port_ref = Node.make_port_ref ~node_id:node.id ~port_index:port.index in
                   match Graph.port_value graph port_ref with
                   | Some value -> Node.Port_ref_map.add port_ref value current
                   | None -> current)
                 current
                 spec.output_ports)
       Node.Port_ref_map.empty

let collect_node_changes graph ordered_nodes previous_day_outputs =
  let current_snapshot = collect_output_snapshot graph in
  let node_changes =
    ordered_nodes
    |> List.filter_map (fun (node : Node.t) ->
           if is_hidden_constant_node node.node_type then
             None
           else
             match Graph.find_node_spec graph node.node_type with
             | None -> None
             | Some spec ->
                 let changed_outputs =
                   spec.output_ports
                   |> List.filter_map (fun (port : Node.port_spec) ->
                          let port_ref = Node.make_port_ref ~node_id:node.id ~port_index:port.index in
                          match Node.Port_ref_map.find_opt port_ref current_snapshot with
                          | None -> None
                          | Some value -> (
                              match Node.Port_ref_map.find_opt port_ref previous_day_outputs with
                              | Some previous_value when Node.equal_value previous_value value -> None
                              | _ -> Some (port.name, value_to_json value)))
                 in
                 if changed_outputs = [] then
                   None
                 else
                   Some
                     (`Assoc
                        [
                          ("nodeId", `String (Node_id.to_string node.id));
                          ("outputs", `Assoc changed_outputs);
                        ]))
  in
  (node_changes, current_snapshot)

let mark_price_for_position cache ticker date =
  match lookup_price_value cache ~ticker ~field:"close" ~date with
  | Some price -> Some price
  | None -> lookup_price_value cache ~ticker ~field:"adj_close" ~date

let balance_snapshot_of_state state cache =
  let market_value, unrealized_pnl =
    state.positions_by_ticker
    |> String_map.bindings
    |> List.fold_left
         (fun (market_value, unrealized_pnl) (_, (position : position)) ->
           match mark_price_for_position cache position.ticker state.current_date with
           | Some market_price ->
               let position_market_value = position.quantity *. market_price in
               let position_unrealized = position.quantity *. (market_price -. position.average_cost) in
               (market_value +. position_market_value, unrealized_pnl +. position_unrealized)
           | None -> (market_value, unrealized_pnl))
         (0.0, 0.0)
  in
  {
    cash = state.cash_balance;
    market_value;
    equity = state.cash_balance +. market_value;
    realized_pnl = state.realized_pnl;
    unrealized_pnl;
  }

let trade_to_json trade =
  `Assoc
    [
      ("nodeId", `String trade.node_id);
      ("action", `String trade.action);
      ("ticker", `String trade.ticker);
      ("sizeMode", `String trade.size_mode);
      ("requestedAmount", float_to_json trade.requested_amount);
      ("filledShares", float_to_json trade.filled_shares);
      ("fillPrice", float_to_json trade.fill_price);
      ("cashBefore", float_to_json trade.cash_before);
      ("cashAfter", float_to_json trade.cash_after);
      ("realizedPnl", float_to_json trade.realized_pnl);
    ]

let balance_snapshot_to_json snapshot =
  `Assoc
    [
      ("cash", float_to_json snapshot.cash);
      ("marketValue", float_to_json snapshot.market_value);
      ("equity", float_to_json snapshot.equity);
      ("realizedPnl", float_to_json snapshot.realized_pnl);
      ("unrealizedPnl", float_to_json snapshot.unrealized_pnl);
    ]

let portfolio_positions_to_json state cache =
  state.positions_by_ticker
  |> String_map.bindings
  |> List.map (fun (_, (position : position)) ->
         let market_price = mark_price_for_position cache position.ticker state.current_date in
         let market_value =
           match market_price with Some price -> position.quantity *. price | None -> 0.0
         in
         let unrealized_pnl =
           match market_price with
           | Some price -> position.quantity *. (price -. position.average_cost)
           | None -> 0.0
         in
         `Assoc
           [
             ("ticker", `String position.ticker);
             ("quantity", float_to_json position.quantity);
             ("averageCost", float_to_json position.average_cost);
             ("marketPrice", option_to_json float_to_json market_price);
             ("marketValue", float_to_json market_value);
             ("unrealizedPnl", float_to_json unrealized_pnl);
           ])

let services_for_day state cache day =
  {
    Executor.current_date = state.current_date;
    resolve_effective_date = effective_date_of_context state;
    lookup_price_value = (fun ~ticker ~field ~date -> lookup_price_value cache ~ticker ~field ~date);
    lookup_company_value = (fun ~ticker ~field -> lookup_company_value cache ~ticker ~field);
    lookup_fundamentals_value =
      (fun ~ticker ~field ~report_date -> lookup_fundamentals_value cache ~ticker ~field ~report_date);
    lookup_financial_statement_value =
      (fun ~ticker ~source_dataset ~field ~report_date ->
        lookup_financial_statement_value cache ~ticker ~source_dataset ~field ~report_date);
    execute_trade =
      (fun ~node ~action ~ticker ~size_mode ~requested_amount ~price_field ->
        execute_trade state cache day ~node ~action ~ticker ~size_mode ~requested_amount ~price_field);
    record_warning = (fun ~node message -> record_warning state day node message);
  }

let day_trace_to_json date day node_changes snapshot =
  `Assoc
    [
      ("date", `String date);
      ("warnings", `List (List.map (fun warning -> `String warning) day.warnings));
      ("errors", `List (List.map (fun error -> `String error) day.errors));
      ("trades", `List (List.map trade_to_json day.trades));
      ("balanceSnapshot", balance_snapshot_to_json snapshot);
      ("nodeChanges", `List node_changes);
    ]

let simulate ~graph ~registry ~start_date ~end_date ~initial_cash ~include_trace =
  match preload_market_cache graph ~start_date ~end_date with
  | Error _ as error -> error
  | Ok market_cache -> (
      match Graph.topological_sort graph with
      | Error errors -> Error (List.map Graph_error.to_string errors)
      | Ok ordered_nodes ->
          let state =
            {
              initial_cash;
              current_date = start_date;
              cash_balance = initial_cash;
              positions_by_ticker = String_map.empty;
              realized_pnl = 0.0;
              warnings = [];
              previous_day_outputs = Node.Port_ref_map.empty;
              trade_count = 0;
            }
          in
          let trace_days = ref [] in
          let final_node_values = ref (`Assoc []) in
          List.iter
            (fun date ->
              state.current_date <- date;
              Graph.clear_port_values graph;
              let day = { warnings = []; errors = []; trades = [] } in
              let simulation = services_for_day state market_cache day in
              let execution_result = Engine.execute ~simulation:(Some simulation) ~graph ~registry in
              begin
                match execution_result with
                | Ok executed_graph ->
                    final_node_values := collect_output_results executed_graph
                | Error errors ->
                    day.errors <- List.map Engine_error.to_string errors;
                    final_node_values := collect_output_results graph
              end;
              let node_changes, current_snapshot =
                collect_node_changes graph ordered_nodes state.previous_day_outputs
              in
              state.previous_day_outputs <- current_snapshot;
              let snapshot = balance_snapshot_of_state state market_cache in
              if include_trace then
                trace_days := !trace_days @ [ day_trace_to_json date day node_changes snapshot ])
            market_cache.calendar;
          let final_snapshot = balance_snapshot_of_state state market_cache in
          let result =
            `Assoc
              [
                ( "summary",
                  `Assoc
                    [
                      ("startDate", `String start_date);
                      ("endDate", `String end_date);
                      ("executedDays", `Int (List.length market_cache.calendar));
                      ("initialCash", float_to_json state.initial_cash);
                      ("finalCash", float_to_json state.cash_balance);
                      ("marketValue", float_to_json final_snapshot.market_value);
                      ("finalEquity", float_to_json final_snapshot.equity);
                      ("realizedPnl", float_to_json state.realized_pnl);
                      ("unrealizedPnl", float_to_json final_snapshot.unrealized_pnl);
                      ("tradeCount", `Int state.trade_count);
                    ] );
                ( "portfolio",
                  `Assoc
                    [
                      ("cash", float_to_json state.cash_balance);
                      ("positions", `List (portfolio_positions_to_json state market_cache));
                    ] );
                ("finalNodeValues", !final_node_values);
                ("trace", `List !trace_days);
                ("warnings", `List (List.map (fun warning -> `String warning) state.warnings));
              ]
          in
          Ok result)
