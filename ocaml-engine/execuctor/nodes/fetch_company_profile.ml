let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let value_of_field field company =
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

let run context =
  let* ticker = Executor.expect_string_field context "ticker" in
  let* field = Executor.expect_string_field context "field" in
  match context.Executor.simulation with
  | Some simulation -> (
      match simulation.lookup_company_value ~ticker ~field with
      | Some value -> Executor.single_output 0 value
      | None ->
          Error
            (Executor.Message
               ("Unsupported or missing company profile field `"
               ^ field
               ^ "` for ticker `"
               ^ ticker
               ^ "`.")))
  | None ->
      let reader = Market_data_reader.connect_from_env () in
      Fun.protect
        ~finally:(fun () -> Market_data_reader.close reader)
        (fun () ->
          match Market_data_reader.find_company reader ~ticker with
          | None ->
              Error (Executor.Message ("No company profile found for ticker `" ^ ticker ^ "`."))
          | Some company -> (
              match value_of_field field company with
              | Some value -> Executor.single_output 0 value
              | None ->
                  Error
                    (Executor.Message
                       ("Unsupported or missing company profile field `"
                       ^ field
                       ^ "` for ticker `"
                       ^ ticker
                       ^ "`."))))

let executor = { Executor.key = "fetch_company_profile"; run }
