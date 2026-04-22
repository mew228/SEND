let all =
  [
    Add.executor;
    Average.executor;
    Subtract.executor;
    Multiply.executor;
    Divide.executor;
    Negate.executor;
    And_node.executor;
    Or_node.executor;
    Not_node.executor;
    Gt.executor;
    Gte.executor;
    Lt.executor;
    Lte.executor;
    Const_number.executor;
    Const_bool.executor;
    Const_string.executor;
    Fetch_company_profile.executor;
    Fetch_financial_statements.executor;
    Fetch_fundamentals.executor;
    Fetch_price.executor;
    Buy.executor;
    Sell.executor;
    Eq.executor;
    Neq.executor;
    If_node.executor;
    To_bool.executor;
    To_number.executor;
    To_string.executor;
  ]

let registry = Executor_registry.of_list all
