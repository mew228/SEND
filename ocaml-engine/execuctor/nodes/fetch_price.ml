let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let number_of_field field share_price =
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

let run context =
  let* ticker = Executor.expect_string_field context "ticker" in
  let* field = Executor.expect_string_field context "field" in
  match context.Executor.simulation with
  | Some simulation -> begin
      let* date = Executor.resolve_effective_date context ~explicit_field_name:"date" in
      match simulation.lookup_price_value ~ticker ~field ~date with
      | Some value -> Executor.single_output 0 (Node.normalize_number value)
      | None ->
          Error (Executor.Message ("No share price found for ticker `" ^ ticker ^ "` on `" ^ date ^ "`." ))
    end
  | None ->
      let* date = Executor.expect_string_field context "date" in
      let reader = Market_data_reader.connect_from_env () in
      Fun.protect
        ~finally:(fun () -> Market_data_reader.close reader)
        (fun () ->
          match Market_data_reader.find_share_price_on_date reader ~ticker ~price_date:date with
          | None -> Error (Executor.Message ("No share price found for ticker `" ^ ticker ^ "` on `" ^ date ^ "`."))
          | Some share_price -> (
              match number_of_field field share_price with
              | Some value -> Executor.single_output 0 (Node.normalize_number value)
              | None ->
                  Error
                    (Executor.Message
                       ("Unsupported or missing price field `"
                       ^ field
                       ^ "` for ticker `"
                       ^ ticker
                       ^ "` on `"
                       ^ date
                       ^ "`."))))

let executor = { Executor.key = "fetch_price"; run }
