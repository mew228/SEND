let assert_true condition message =
  if not condition then failwith message

let node_id value = Node_id.of_string value

let spec
    ?(inputs = [])
    ?(outputs = [])
    ?(data_fields = [])
    ~node_type
    ~executor_key
    ~display_name
    ()
  =
  Node.make_spec
    ~node_type
    ~set:"primitive"
    ~display_name
    ~description:display_name
    ~input_ports:inputs
    ~output_ports:outputs
    ~data_fields
    ~executor_key

let port index name value_kind =
  Node.make_port_spec ~index ~name ~arity:Node.one ~value_kind

let input index value = (index, [ value ])

let data_field_spec ?(required = false) ?default_value name value_kind =
  Node.make_data_field_spec ~name ~value_kind ~required ~default_value

let context ?(data_fields = []) ~node_type ~node_spec ~inputs () =
  {
    Executor.node = Node.make ~id:(node_id (node_type ^ "-node")) ~node_type ~data_fields ();
    node_spec;
    inputs;
    simulation = None;
  }

let expect_output expected = function
  | Ok output when output = expected -> ()
  | Ok _ -> failwith "unexpected executor output"
  | Error error -> failwith (Executor.run_error_to_string error)

let expect_error predicate = function
  | Error error when predicate error -> ()
  | Error error -> failwith ("unexpected executor error: " ^ Executor.run_error_to_string error)
  | Ok _ -> failwith "expected executor error"

let add_spec =
  spec
    ~node_type:"add"
    ~executor_key:"add"
    ~display_name:"Add"
    ~inputs:[ port 0 "a" Node.number_kind; port 1 "b" Node.number_kind ]
    ~outputs:[ port 0 "sum" Node.number_kind ]
    ()

let average_spec =
  spec
    ~node_type:"average"
    ~executor_key:"average"
    ~display_name:"Average"
    ~inputs:[ Node.make_port_spec ~index:0 ~name:"values" ~arity:Node.many ~value_kind:Node.number_kind ]
    ~outputs:[ port 0 "average" Node.number_kind ]
    ()

let subtract_spec =
  spec
    ~node_type:"subtract"
    ~executor_key:"subtract"
    ~display_name:"Subtract"
    ~inputs:[ port 0 "a" Node.number_kind; port 1 "b" Node.number_kind ]
    ~outputs:[ port 0 "difference" Node.number_kind ]
    ()

let multiply_spec =
  spec
    ~node_type:"multiply"
    ~executor_key:"multiply"
    ~display_name:"Multiply"
    ~inputs:[ port 0 "a" Node.number_kind; port 1 "b" Node.number_kind ]
    ~outputs:[ port 0 "product" Node.number_kind ]
    ()

let divide_spec =
  spec
    ~node_type:"divide"
    ~executor_key:"divide"
    ~display_name:"Divide"
    ~inputs:[ port 0 "numerator" Node.number_kind; port 1 "denominator" Node.number_kind ]
    ~outputs:[ port 0 "quotient" Node.number_kind ]
    ()

let negate_spec =
  spec
    ~node_type:"negate"
    ~executor_key:"negate"
    ~display_name:"Negate"
    ~inputs:[ port 0 "value" Node.number_kind ]
    ~outputs:[ port 0 "result" Node.number_kind ]
    ()

let and_spec =
  spec
    ~node_type:"and"
    ~executor_key:"and"
    ~display_name:"And"
    ~inputs:[ port 0 "left" Node.bool_kind; port 1 "right" Node.bool_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let or_spec =
  spec
    ~node_type:"or"
    ~executor_key:"or"
    ~display_name:"Or"
    ~inputs:[ port 0 "left" Node.bool_kind; port 1 "right" Node.bool_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let not_spec =
  spec
    ~node_type:"not"
    ~executor_key:"not"
    ~display_name:"Not"
    ~inputs:[ port 0 "value" Node.bool_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let gt_spec =
  spec
    ~node_type:"gt"
    ~executor_key:"gt"
    ~display_name:"Gt"
    ~inputs:[ port 0 "left" Node.number_kind; port 1 "right" Node.number_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let gte_spec =
  spec
    ~node_type:"gte"
    ~executor_key:"gte"
    ~display_name:"Gte"
    ~inputs:[ port 0 "left" Node.number_kind; port 1 "right" Node.number_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let lt_spec =
  spec
    ~node_type:"lt"
    ~executor_key:"lt"
    ~display_name:"Lt"
    ~inputs:[ port 0 "left" Node.number_kind; port 1 "right" Node.number_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let lte_spec =
  spec
    ~node_type:"lte"
    ~executor_key:"lte"
    ~display_name:"Lte"
    ~inputs:[ port 0 "left" Node.number_kind; port 1 "right" Node.number_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let const_number_spec =
  spec
    ~node_type:"const_number"
    ~executor_key:"const_number"
    ~display_name:"Const Number"
    ~outputs:[ port 0 "value" Node.number_kind ]
    ~data_fields:[ data_field_spec ~required:true ~default_value:(Node.Number_value 0.0) "value" Node.number_kind ]
    ()

let const_bool_spec =
  spec
    ~node_type:"const_bool"
    ~executor_key:"const_bool"
    ~display_name:"Const Bool"
    ~outputs:[ port 0 "value" Node.bool_kind ]
    ~data_fields:[ data_field_spec ~required:true ~default_value:(Node.Bool_value false) "value" Node.bool_kind ]
    ()

let const_string_spec =
  spec
    ~node_type:"const_string"
    ~executor_key:"const_string"
    ~display_name:"Const String"
    ~outputs:[ port 0 "value" Node.string_kind ]
    ~data_fields:[ data_field_spec ~required:true ~default_value:(Node.String_value "") "value" Node.string_kind ]
    ()

let eq_spec =
  spec
    ~node_type:"eq"
    ~executor_key:"eq"
    ~display_name:"Eq"
    ~inputs:[ port 0 "left" Node.any_kind; port 1 "right" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let neq_spec =
  spec
    ~node_type:"neq"
    ~executor_key:"neq"
    ~display_name:"Neq"
    ~inputs:[ port 0 "left" Node.any_kind; port 1 "right" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let if_spec =
  spec
    ~node_type:"if"
    ~executor_key:"if"
    ~display_name:"If"
    ~inputs:[ port 0 "condition" Node.bool_kind; port 1 "when_true" Node.any_kind; port 2 "when_false" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.any_kind ]
    ()

let to_bool_spec =
  spec
    ~node_type:"to_bool"
    ~executor_key:"to_bool"
    ~display_name:"To Bool"
    ~inputs:[ port 0 "value" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.bool_kind ]
    ()

let to_number_spec =
  spec
    ~node_type:"to_number"
    ~executor_key:"to_number"
    ~display_name:"To Number"
    ~inputs:[ port 0 "value" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.number_kind ]
    ()

let to_string_spec =
  spec
    ~node_type:"to_string"
    ~executor_key:"to_string"
    ~display_name:"To String"
    ~inputs:[ port 0 "value" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.string_kind ]
    ()

let test_arithmetic_executors () =
  expect_output
    [ (0, Node.Number_value 4.0) ]
    (Add.executor.run
       (context
          ~node_type:"add"
          ~node_spec:add_spec
          ~inputs:[ input 0 (Node.Number_value 1.5); input 1 (Node.Number_value 2.5) ]
          ()));
  expect_output
    [ (0, Node.Number_value 3.0) ]
    (Subtract.executor.run
       (context
          ~node_type:"subtract"
          ~node_spec:subtract_spec
          ~inputs:[ input 0 (Node.Number_value 5.0); input 1 (Node.Number_value 2.0) ]
          ()));
  expect_output
    [ (0, Node.Number_value 12.0) ]
    (Multiply.executor.run
       (context
          ~node_type:"multiply"
          ~node_spec:multiply_spec
          ~inputs:[ input 0 (Node.Number_value 3.0); input 1 (Node.Number_value 4.0) ]
          ()));
  expect_output
    [ (0, Node.Number_value 2.5) ]
    (Divide.executor.run
       (context
          ~node_type:"divide"
          ~node_spec:divide_spec
          ~inputs:[ input 0 (Node.Number_value 5.0); input 1 (Node.Number_value 2.0) ]
          ()));
  expect_output
    [ (0, Node.Number_value (-3.0)) ]
    (Negate.executor.run
       (context
          ~node_type:"negate"
          ~node_spec:negate_spec
          ~inputs:[ input 0 (Node.Number_value 3.0) ]
          ()))

let test_boolean_and_comparison_executors () =
  expect_output
    [ (0, Node.Bool_value false) ]
    (And_node.executor.run
       (context
          ~node_type:"and"
          ~node_spec:and_spec
          ~inputs:[ input 0 (Node.Bool_value true); input 1 (Node.Bool_value false) ]
          ()));
  expect_output
    [ (0, Node.Bool_value true) ]
    (Or_node.executor.run
       (context
          ~node_type:"or"
          ~node_spec:or_spec
          ~inputs:[ input 0 (Node.Bool_value true); input 1 (Node.Bool_value false) ]
          ()));
  expect_output
    [ (0, Node.Bool_value false) ]
    (Not_node.executor.run
       (context
          ~node_type:"not"
          ~node_spec:not_spec
          ~inputs:[ input 0 (Node.Bool_value true) ]
          ()));
  expect_output
    [ (0, Node.Bool_value true) ]
    (Gt.executor.run
       (context
          ~node_type:"gt"
          ~node_spec:gt_spec
          ~inputs:[ input 0 (Node.Number_value 3.0); input 1 (Node.Number_value 2.0) ]
          ()));
  expect_output
    [ (0, Node.Bool_value true) ]
    (Gte.executor.run
       (context
          ~node_type:"gte"
          ~node_spec:gte_spec
          ~inputs:[ input 0 (Node.Number_value 3.0); input 1 (Node.Number_value 3.0) ]
          ()));
  expect_output
    [ (0, Node.Bool_value true) ]
    (Lt.executor.run
       (context
          ~node_type:"lt"
          ~node_spec:lt_spec
          ~inputs:[ input 0 (Node.Number_value 2.0); input 1 (Node.Number_value 3.0) ]
          ()));
  expect_output
    [ (0, Node.Bool_value true) ]
    (Lte.executor.run
       (context
          ~node_type:"lte"
          ~node_spec:lte_spec
          ~inputs:[ input 0 (Node.Number_value 3.0); input 1 (Node.Number_value 3.0) ]
          ()))

let test_constant_executors () =
  expect_output
    [ (0, Node.Number_value 9.0) ]
    (Const_number.executor.run
       (context
          ~node_type:"const_number"
          ~node_spec:const_number_spec
          ~inputs:[]
          ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 9.0) ]
          ()));
  expect_output
    [ (0, Node.Bool_value false) ]
    (Const_bool.executor.run
       (context
          ~node_type:"const_bool"
          ~node_spec:const_bool_spec
          ~inputs:[]
          ()));
  expect_output
    [ (0, Node.String_value "hello") ]
    (Const_string.executor.run
       (context
          ~node_type:"const_string"
          ~node_spec:const_string_spec
          ~inputs:[]
          ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.String_value "hello") ]
          ()))

let test_polymorphic_and_conversion_executors () =
  expect_output
    [ (0, Node.Bool_value true) ]
    (Eq.executor.run
       (context
          ~node_type:"eq"
          ~node_spec:eq_spec
          ~inputs:[ input 0 (Node.String_value "x"); input 1 (Node.String_value "x") ]
          ()));
  expect_output
    [ (0, Node.Bool_value true) ]
    (Neq.executor.run
       (context
          ~node_type:"neq"
          ~node_spec:neq_spec
          ~inputs:[ input 0 (Node.String_value "x"); input 1 (Node.Number_value 1.0) ]
          ()));
  expect_output
    [ (0, Node.String_value "chosen") ]
    (If_node.executor.run
       (context
          ~node_type:"if"
          ~node_spec:if_spec
          ~inputs:[ input 0 (Node.Bool_value true); input 1 (Node.String_value "chosen"); input 2 (Node.String_value "other") ]
          ()));
  expect_output
    [ (0, Node.Bool_value false) ]
    (To_bool.executor.run
       (context
          ~node_type:"to_bool"
          ~node_spec:to_bool_spec
          ~inputs:[ input 0 (Node.Number_value 0.0) ]
          ()));
  expect_output
    [ (0, Node.Number_value 12.5) ]
    (To_number.executor.run
       (context
          ~node_type:"to_number"
          ~node_spec:to_number_spec
          ~inputs:[ input 0 (Node.String_value "12.5") ]
          ()));
  expect_output
    [ (0, Node.String_value "true") ]
    (To_string.executor.run
       (context
          ~node_type:"to_string"
          ~node_spec:to_string_spec
          ~inputs:[ input 0 (Node.Bool_value true) ]
          ()))

let test_average_executor_and_multi_helpers () =
  expect_output
    [ (0, Node.Number_value 4.0) ]
    (Average.executor.run
       (context
          ~node_type:"average"
          ~node_spec:average_spec
          ~inputs:[ (0, [ Node.Number_value 2.0; Node.Number_value 4.0; Node.Number_value 6.0 ]) ]
          ()));
  match Executor.expect_numbers 0 [ (0, [ Node.Number_value 1.0; Node.Number_value 2.5 ]) ] with
  | Ok [ 1.0; 2.5 ] -> ()
  | _ -> failwith "expected multi-number helper to return all values"

let test_executor_failures () =
  expect_error
    (function
      | Executor.Invalid_input_type _ -> true
      | _ -> false)
    (Add.executor.run
       (context
          ~node_type:"add"
          ~node_spec:add_spec
          ~inputs:[ input 0 (Node.Bool_value true); input 1 (Node.Number_value 2.0) ]
          ()));
  expect_error
    (function
      | Executor.Missing_input 1 -> true
      | _ -> false)
    (Add.executor.run
       (context
          ~node_type:"add"
          ~node_spec:add_spec
          ~inputs:[ input 0 (Node.Number_value 2.0) ]
          ()));
  expect_error
    (function
      | Executor.Invalid_input_count { index = 0; expected_count = 1; actual_count = 2 } -> true
      | _ -> false)
    (Add.executor.run
       (context
          ~node_type:"add"
          ~node_spec:add_spec
          ~inputs:[ (0, [ Node.Number_value 2.0; Node.Number_value 3.0 ]); input 1 (Node.Number_value 4.0) ]
          ()));
  expect_error
    (function
      | Executor.Message "Division by zero" -> true
      | _ -> false)
    (Divide.executor.run
       (context
          ~node_type:"divide"
          ~node_spec:divide_spec
          ~inputs:[ input 0 (Node.Number_value 5.0); input 1 (Node.Number_value 0.0) ]
          ()));
  expect_error
    (function
      | Executor.Message message -> String.starts_with ~prefix:"Cannot convert string to number:" message
      | _ -> false)
    (To_number.executor.run
       (context
          ~node_type:"to_number"
          ~node_spec:to_number_spec
          ~inputs:[ input 0 (Node.String_value "bad") ]
          ()))

let test_primitive_registry_lookup () =
  assert_true (List.length Primitive_registry.all = 28) "expected full primitive executor set";
  assert_true (Executor_registry.find "add" Primitive_registry.registry <> None) "expected add lookup";
  assert_true (Executor_registry.find "average" Primitive_registry.registry <> None) "expected average lookup";
  assert_true (Executor_registry.find "subtract" Primitive_registry.registry <> None) "expected subtract lookup";
  assert_true (Executor_registry.find "to_string" Primitive_registry.registry <> None) "expected to_string lookup";
  assert_true (Executor_registry.find "buy" Primitive_registry.registry <> None) "expected buy lookup";
  assert_true (Executor_registry.find "sell" Primitive_registry.registry <> None) "expected sell lookup"

let run_all () =
  test_arithmetic_executors ();
  test_boolean_and_comparison_executors ();
  test_constant_executors ();
  test_polymorphic_and_conversion_executors ();
  test_average_executor_and_multi_helpers ();
  test_executor_failures ();
  test_primitive_registry_lookup ()
