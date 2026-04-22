let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let outputs_of_trade_result trade_result =
  let fill_price =
    match trade_result.Executor.fill_price with
    | Some value -> value
    | None -> 0.0
  in
  Ok
    [
      (0, Node.Bool_value trade_result.executed);
      (1, Node.normalize_number trade_result.filled_shares);
      (2, Node.normalize_number fill_price);
      (3, Node.normalize_number trade_result.cash_after);
      (4, Node.normalize_number trade_result.realized_pnl);
    ]

let run context =
  let* simulation = Executor.require_simulation context in
  let* should_buy = Executor.expect_bool 0 context.inputs in
  if not should_buy then
    outputs_of_trade_result
      {
        Executor.executed = false;
        filled_shares = 0.0;
        fill_price = None;
        cash_before = 0.0;
        cash_after = 0.0;
        realized_pnl = 0.0;
      }
  else
    let* ticker = Executor.expect_string_field context "ticker" in
    let* size_mode = Executor.expect_string_field context "sizeMode" in
    let* price_field = Executor.expect_string_field context "priceField" in
    let requested_amount =
      match String.lowercase_ascii size_mode with
      | "dollars" -> Executor.expect_number_field context "dollarAmount"
      | _ -> Executor.expect_number_field context "shareQuantity"
    in
    let* requested_amount = requested_amount in
    let* trade_result =
      simulation.execute_trade
        ~node:context.node
        ~action:"buy"
        ~ticker
        ~size_mode
        ~requested_amount
        ~price_field
    in
    outputs_of_trade_result trade_result

let executor = { Executor.key = "buy"; run }
