type input = (int * Node.value list) list
type output = (int * Node.value) list

type trade_result = {
  executed : bool;
  filled_shares : float;
  fill_price : float option;
  cash_before : float;
  cash_after : float;
  realized_pnl : float;
}

type run_error =
  | Missing_input of int
  | Invalid_input_count of {
      index : int;
      expected_count : int;
      actual_count : int;
    }
  | Invalid_input_type of {
      index : int;
      expected : Node.value;
      actual : Node.value;
    }
  | Invalid_output_type of {
      index : int;
      expected : Node.value;
      actual : Node.value;
    }
  | Message of string

type run_context = {
  node : Node.t;
  node_spec : Node.node_spec;
  inputs : input;
  simulation : simulation_services option;
}

and simulation_services = {
  current_date : string;
  resolve_effective_date : run_context -> explicit_field_name:string -> (string, run_error) result;
  lookup_price_value : ticker:string -> field:string -> date:string -> float option;
  lookup_company_value : ticker:string -> field:string -> Node.value option;
  lookup_fundamentals_value : ticker:string -> field:string -> report_date:string -> float option;
  lookup_financial_statement_value :
    ticker:string -> source_dataset:string -> field:string -> report_date:string -> Node.value option;
  execute_trade :
    node:Node.t ->
    action:string ->
    ticker:string ->
    size_mode:string ->
    requested_amount:float ->
    price_field:string ->
    (trade_result, run_error) result;
  record_warning : node:Node.t -> string -> unit;
}

type t = {
  key : string;
  run : run_context -> (output, run_error) result;
}

let find_input (inputs : input) index =
  List.assoc_opt index inputs

let expect_values index inputs =
  match find_input inputs index with
  | Some values -> Ok values
  | None -> Error (Missing_input index)

let expect_value index inputs =
  match expect_values index inputs with
  | Ok [ value ] -> Ok value
  | Ok values ->
      Error
        (Invalid_input_count
           {
             index;
             expected_count = 1;
             actual_count = List.length values;
           })
  | Error _ as error -> error

let expect_typed_values index inputs ~expected_value extract =
  let rec loop acc = function
    | [] -> Ok (List.rev acc)
    | value :: remaining -> (
        match extract value with
        | Some decoded -> loop (decoded :: acc) remaining
        | None ->
            Error
              (Invalid_input_type
                 {
                   index;
                   expected = expected_value;
                   actual = value;
                 }))
  in
  match expect_values index inputs with
  | Ok values -> loop [] values
  | Error _ as error -> error

let expect_numbers index inputs =
  expect_typed_values index inputs ~expected_value:Node.number (function
    | Node.Number_value value -> Some value
    | _ -> None)

let expect_number index inputs =
  match expect_numbers index inputs with
  | Ok [ value ] -> Ok value
  | Ok values ->
      Error
        (Invalid_input_count
           {
             index;
             expected_count = 1;
             actual_count = List.length values;
           })
  | Error _ as error -> error

let expect_bools index inputs =
  expect_typed_values index inputs ~expected_value:Node.bool (function
    | Node.Bool_value value -> Some value
    | _ -> None)

let expect_bool index inputs =
  match expect_bools index inputs with
  | Ok [ value ] -> Ok value
  | Ok values ->
      Error
        (Invalid_input_count
           {
             index;
             expected_count = 1;
             actual_count = List.length values;
           })
  | Error _ as error -> error

let expect_strings index inputs =
  expect_typed_values index inputs ~expected_value:Node.string (function
    | Node.String_value value -> Some value
    | _ -> None)

let expect_string index inputs =
  match expect_strings index inputs with
  | Ok [ value ] -> Ok value
  | Ok values ->
      Error
        (Invalid_input_count
           {
             index;
             expected_count = 1;
             actual_count = List.length values;
           })
  | Error _ as error -> error

let find_data_field_value context field_name =
  match Node.find_data_field context.node field_name with
  | Some field ->
      begin
        match Node.find_data_field_spec context.node_spec field_name with
        | Some spec when Node.kind_matches_value spec.value_kind field.value -> Ok field.value
        | Some spec ->
            Error
              (Message
                 ("Data field `"
                 ^ field_name
                 ^ "` expected "
                 ^ Node.value_kind_label spec.value_kind
                 ^ " but got "
                 ^ Node.value_label field.value))
        | None -> Ok field.value
      end
  | None -> (
      match Node.find_data_field_spec context.node_spec field_name with
      | Some spec -> (
          match spec.default_value with
          | Some value -> Ok value
          | None ->
              if spec.required then
                Error (Message ("Missing required data field `" ^ field_name ^ "`"))
              else
                Error (Message ("Missing data field `" ^ field_name ^ "`")))
      | None -> Error (Message ("Unknown data field `" ^ field_name ^ "`")))

let expect_number_field context field_name =
  match find_data_field_value context field_name with
  | Ok (Node.Number_value value) -> Ok value
  | Ok actual ->
      Error
        (Message
           ("Data field `"
           ^ field_name
           ^ "` expected number but got "
           ^ Node.value_label actual))
  | Error _ as error -> error

let expect_bool_field context field_name =
  match find_data_field_value context field_name with
  | Ok (Node.Bool_value value) -> Ok value
  | Ok actual ->
      Error
        (Message
           ("Data field `"
           ^ field_name
           ^ "` expected bool but got "
           ^ Node.value_label actual))
  | Error _ as error -> error

let expect_string_field context field_name =
  match find_data_field_value context field_name with
  | Ok (Node.String_value value) -> Ok value
  | Ok actual ->
      Error
        (Message
           ("Data field `"
           ^ field_name
           ^ "` expected string but got "
           ^ Node.value_label actual))
  | Error _ as error -> error

let require_simulation context =
  match context.simulation with
  | Some simulation -> Ok simulation
  | None -> Error (Message "This node requires simulation runtime context.")

let resolve_effective_date context ~explicit_field_name =
  match require_simulation context with
  | Ok simulation -> simulation.resolve_effective_date context ~explicit_field_name
  | Error _ as error -> error

let single_output index value =
  Ok [ (index, value) ]

let run_error_to_string = function
  | Missing_input index -> "Missing input at index " ^ string_of_int index
  | Invalid_input_count { index; expected_count; actual_count } ->
      "Invalid input count at index "
      ^ string_of_int index
      ^ ": expected "
      ^ string_of_int expected_count
      ^ " but got "
      ^ string_of_int actual_count
  | Invalid_input_type { index; expected; actual } ->
      "Invalid input type at index "
      ^ string_of_int index
      ^ ": expected "
      ^ Node.value_label expected
      ^ " but got "
      ^ Node.value_label actual
  | Invalid_output_type { index; expected; actual } ->
      "Invalid output type at index "
      ^ string_of_int index
      ^ ": expected "
      ^ Node.value_label expected
      ^ " but got "
      ^ Node.value_label actual
  | Message message -> message
