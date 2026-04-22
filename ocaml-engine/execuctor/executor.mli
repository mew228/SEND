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

val find_input : input -> int -> Node.value list option
val expect_values : int -> input -> (Node.value list, run_error) result
val expect_value : int -> input -> (Node.value, run_error) result
val expect_numbers : int -> input -> (float list, run_error) result
val expect_number : int -> input -> (float, run_error) result
val expect_bools : int -> input -> (bool list, run_error) result
val expect_bool : int -> input -> (bool, run_error) result
val expect_strings : int -> input -> (string list, run_error) result
val expect_string : int -> input -> (string, run_error) result
val find_data_field_value : run_context -> string -> (Node.value, run_error) result
val expect_number_field : run_context -> string -> (float, run_error) result
val expect_bool_field : run_context -> string -> (bool, run_error) result
val expect_string_field : run_context -> string -> (string, run_error) result
val require_simulation : run_context -> (simulation_services, run_error) result
val resolve_effective_date : run_context -> explicit_field_name:string -> (string, run_error) result
val single_output : int -> Node.value -> (output, run_error) result
val run_error_to_string : run_error -> string
