:::lecture { "id": "economics--getting-started--turning-economic-ideas-into-strategy-rules", "slug": "turning-economic-ideas-into-strategy-rules", "path": { "slug": "economics", "title": "Economics", "description": "Beginner-friendly lessons that teach economic ideas through graphs, nodes, and strategy building." }, "category": { "slug": "getting-started", "title": "Getting Started" }, "title": "Turning Economic Ideas Into Strategy Rules", "summary": "Learn how to take a basic economic belief and translate it into a graph with inputs, logic, and actions.", "estimatedMinutes": 14 }

:::sublecture { "id":"a-belief-is-not-a-rule-yet", "title":"A Belief Is Not A Rule Yet" }
# A Belief Is Not A Rule Yet

A lot of strategies start as a sentence.

“If price is above a certain level, buy.”  
“If earnings are strong, allow entry.”  
“If inflation is too high, avoid risk.”  

Those are *good beginnings*, but they are not rules yet.

A rule has to be specific enough that a graph can run it the same way every time. That means you have to turn a sentence into three concrete parts:

- **an input** (what number are we reading?)
- **logic** (what question are we asking about that number?)
- **an action** (what do we do when the answer is true?)

This lesson is about learning that translation.

It is also about noticing the gap between *sounding correct* and *being executable*. A sentence can sound smart, but a graph only understands clear inputs and clear decisions.

One quick note: the examples in this lesson are teaching patterns, not recommending trades. The goal is to practice building rules that are clear, inspectable, and testable.

:::

:::sublecture { "id":"choose-the-input-you-mean", "title":"Choose The Input You Mean" }
# Choose The Input You Mean

The fastest way to make a strategy confusing is to keep the input vague.

When you say “earnings,” do you mean net income, earnings per share (EPS), or something else?

When you say “inflation,” do you mean a specific measure like CPI year-over-year?

When you say “price is high,” do you mean above a fixed threshold, above last week, or above a moving average?

You do not need to solve every economic debate to get started.

You only need to get precise about the input.

A helpful beginner grouping is:

- **Market inputs** (price)
- **Company inputs** (fundamentals like EPS or P/E)
- **Macro inputs** (broad indicators like inflation or unemployment)

A graph can use one of these, or combine them.

But you always start by choosing one number that you can fetch.

:::

:::sublecture { "id":"turn-the-input-into-a-yes-no-signal", "title":"Turn The Input Into A Yes-No Signal" }
# Turn The Input Into A Yes-No Signal

Graphs become powerful when they stop holding numbers and start producing signals.

A signal is a simple answer to a question.

True or false.  
Yes or no.  
Above or below.

That is how you turn an economic idea into something a strategy can act on.

Here is the simplest translation pattern:

1. **Fetch** an indicator value  
2. **Compare** it to a threshold  
3. Use the result as a **signal**  

You already learned this pattern with price.

The important bridge idea is that the exact same structure works for many indicators:

- price vs a level
- EPS vs zero
- P/E vs a maximum
- inflation vs a risk limit

The graph does not care whether the input is “market,” “fundamental,” or “macro.”

It only cares that it receives a value, compares it, and produces a signal.

:::checkpoint
{
  "id": "checkpoint-translate-a-fundamentals-belief",
  "title": "Checkpoint: Translate One Belief Into A Rule",
  "instructions": [
    "Build a simple rule: if EPS is above 0, trigger a buy action.",
    "This is a practice translation. In real strategies, you would usually combine this with other signals."
  ],
  "tasks": [
    {
      "id": "task-add-fetch-fundamentals",
      "label": "Add Fetch Fundamentals",
      "description": "Place a Fetch Fundamentals node on the canvas."
    },
    {
      "id": "task-configure-fundamentals",
      "label": "Set ticker and metric",
      "description": "Set ticker to AAPL and metric to eps_ttm."
    },
    {
      "id": "task-add-threshold",
      "label": "Add threshold",
      "description": "Add a Constant Number node set to 0."
    },
    {
      "id": "task-add-comparison",
      "label": "Add Greater Than",
      "description": "Add a Greater Than node to compare EPS against 0."
    },
    {
      "id": "task-wire-comparison",
      "label": "Wire the rule",
      "description": "Connect Fetch Fundamentals and Constant Number into Greater Than."
    },
    {
      "id": "task-add-action",
      "label": "Trigger an action",
      "description": "Connect Greater Than into Buy."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_fundamentals", "const_number", "gt", "buy"],
    "starterNodes": [],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_exists", "nodeType": "fetch_fundamentals" },
    { "type": "node_field_equals", "nodeType": "fetch_fundamentals", "field": "ticker", "expected": "AAPL" },
    { "type": "node_field_equals", "nodeType": "fetch_fundamentals", "field": "metric", "expected": "eps_ttm" },

    { "type": "node_exists", "nodeType": "const_number" },
    { "type": "node_field_equals", "nodeType": "const_number", "field": "value", "expected": 0 },

    { "type": "node_exists", "nodeType": "gt" },
    { "type": "connection_exists", "sourceType": "fetch_fundamentals", "targetType": "gt" },
    { "type": "connection_exists", "sourceType": "const_number", "targetType": "gt" },

    { "type": "node_exists", "nodeType": "buy" },
    { "type": "connection_exists", "sourceType": "gt", "targetType": "buy" }
  ]
}
:::

:::

:::sublecture { "id":"connect-signals-into-real-strategy-logic", "title":"Connect Signals Into Real Strategy Logic" }
# Connect Signals Into Real Strategy Logic

A single signal can trigger a simple strategy.

But most real strategies use signals in one of two ways:

## Use a signal as a gate

A fundamentals signal is often a gate.

It does not need to be the reason you buy. It can simply decide whether buying is allowed at all.

For example:

- “Only consider buys if EPS is positive.”
- Then use price logic for the actual entry.

In a graph, this usually means the fundamentals signal flows into a branch or guard node that blocks the buy path when the condition is not met.

## Combine signals

If you want to express:

“If one condition is true and another is false…”

You are no longer describing one comparison. You are describing a relationship between signals.

Graph logic nodes let you combine and shape signals, such as:

- AND (both must be true)
- OR (either can be true)
- NOT (invert a signal)

A very common beginner pattern is:

- one signal says “allowed”
- one signal says “entry”
- one action node executes only when both agree

This is the point where a strategy stops being a single rule and starts being a small system.

:::

:::sublecture { "id":"test-the-rule-like-a-system", "title":"Test The Rule Like A System" }
# Test The Rule Like A System

Once you can translate beliefs into graph rules, the next step is learning how to *evaluate* them.

A clean-looking rule is not automatically a good rule. It is simply a rule the graph can execute consistently.

That is why testing matters.

In the sandbox, you can use execution tools like replay to step through a strategy day by day, inspect fetched values, and see what the rule actually produced. This helps you answer questions like:

- what value did the graph fetch on this day?
- what did the comparison output?
- did an action execute?
- what changed as a result?

You do not need advanced economics for this part.

You only need the habit of checking whether the rule you built matches the behavior you *thought* you built.

If it does not match, the fix is often simple:

- clarify the input
- clarify the threshold
- clarify how signals connect to actions

That is the real skill this path is building: taking ideas and turning them into logic you can read and verify.

:::

## Sources

- FINRA (Momentum investing overview and risks): https://www.finra.org/investors/insights/momentum-investing
- FINRA (Evaluating stocks; EPS and P/E explanations; filings context): https://www.finra.org/investors/investing/investment-products/stocks/evaluating-stocks
- Investor.gov (How to read a 10‑K / 10‑Q): https://www.investor.gov/introduction-investing/general-resources/news-alerts/alerts-bulletins/investor-bulletins/how-read
- Investor.gov (What is risk?): https://www.investor.gov/introduction-investing/investing-basics/what-risk
- BLS (CPI definition and overview): https://www.bls.gov/opub/hom/cpi/
- BLS (CPI release schedule): https://www.bls.gov/schedule/news_release/cpi.htm
- BLS (Unemployment rate definition): https://www.bls.gov/cps/definitions.htm
- BLS (Employment Situation release schedule): https://www.bls.gov/schedule/news_release/empsit.htm
- BEA (GDP definition): https://www.bea.gov/data/gdp/gross-domestic-product
- BEA (Release schedule showing GDP advance/second/third estimates): https://www.bea.gov/news/schedule
- Federal Reserve (Policy rate and federal funds rate Q&A): https://www.federalreserve.gov/economy-at-a-glance-policy-rate.htm
- FRED Help (What is FRED; revisions context): https://fredhelp.stlouisfed.org/fred/about/about-fred/what-is-fred/
