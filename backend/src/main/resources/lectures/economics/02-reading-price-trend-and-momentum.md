:::lecture { "id": "economics--getting-started--reading-price-trend-and-momentum", "slug": "reading-price-trend-and-momentum", "path": { "slug": "economics", "title": "Economics", "description": "Beginner-friendly lessons on market ideas, indicators, and how they can be used in strategy graphs." }, "category": { "slug": "getting-started", "title": "Getting Started" }, "title": "Reading Price, Trend, And Momentum", "summary": "Learn how price movement begins to tell a story and how simple graphs can use price and trend-like signals to make decisions.", "estimatedMinutes": 12 }

:::sublecture { "id":"price-is-the-first-market-signal", "title":"Price Is The First Market Signal" }
# Price Is The First Market Signal

The easiest place to begin in markets is with price.

Price is simply what buyers and sellers are agreeing on at a given moment. If a stock is trading at `150`, that number tells you where the market currently values it. On its own, that does not tell you whether the stock is good, bad, expensive, or cheap. It only tells you the current market price.

That may sound simple, but price is still the starting point for many strategies.

In a graph, price can become a direct input. A **Fetch Price Data** node brings that value into the system so the graph has something real to evaluate. Once the value is in the graph, you can compare it to a rule, combine it with other values, or later use it as part of a more advanced signal.

This is why price matters so much in the beginning. Before you learn about more advanced indicators, you need to understand that many of them are built from price itself.

## Why one day of price still matters

Even a single day of price can be useful.

A graph might ask a basic question such as:

**Is the price above 150?**

That is a very simple rule, but it already shows how a market value can turn into a decision. The graph takes in price, compares it to a fixed number, and produces a signal that can later lead to an action.

That is the beginning of economic reasoning inside a graph.

:::checkpoint
{
  "id": "checkpoint-build-price-signal",
  "title": "Checkpoint 1: Build A Price Signal",
  "instructions": [
    "Add one Fetch Price Data node.",
    "Add one Constant Number node.",
    "Set the Constant Number node value to 150.",
    "Add a Greater Than node.",
    "Connect Fetch Price Data into Greater Than.",
    "Connect Constant Number into Greater Than."
  ],
  "tasks": [
    {
      "id": "task-add-price-node",
      "label": "Add Fetch Price Data",
      "description": "Place a Fetch Price Data node on the canvas."
    },
    {
      "id": "task-add-threshold-node",
      "label": "Add Constant Number",
      "description": "Place a Constant Number node on the canvas."
    },
    {
      "id": "task-set-threshold",
      "label": "Set threshold to 150",
      "description": "Update the Constant Number node value to 150."
    },
    {
      "id": "task-add-comparison-node",
      "label": "Add Greater Than",
      "description": "Place a Greater Than node so the graph can compare the fetched price to a rule."
    },
    {
      "id": "task-connect-price",
      "label": "Connect price into Greater Than",
      "description": "Wire Fetch Price Data into Greater Than."
    },
    {
      "id": "task-connect-threshold",
      "label": "Connect threshold into Greater Than",
      "description": "Wire Constant Number into Greater Than."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "const_number", "gt"],
    "starterNodes": [],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_exists", "nodeType": "fetch_price" },
    { "type": "node_exists", "nodeType": "const_number" },
    { "type": "node_field_equals", "nodeType": "const_number", "field": "value", "expected": 150 },
    { "type": "node_exists", "nodeType": "gt" },
    { "type": "connection_exists", "sourceType": "fetch_price", "targetType": "gt" },
    { "type": "connection_exists", "sourceType": "const_number", "targetType": "gt" }
  ]
}
:::

:::sublecture { "id":"multiple-days-begin-to-show-a-trend", "title":"Multiple Days Begin To Show A Trend" }
# Multiple Days Begin To Show A Trend

One day of price tells you where the market is right now.

Multiple days of price begin to tell you where the market has been moving.

That is where trend starts to matter.

A trend is the general direction price has been moving over time. If prices have been moving upward across many days, that may suggest an upward trend. If prices have been moving downward across many days, that may suggest a downward trend.

This is important because many strategies are not based only on one isolated value. They are based on whether price seems to be rising, falling, or holding steady across time.

## Why trend is useful

Trend helps a graph move from a single snapshot to a bigger pattern.

Instead of only asking, “What is the price today?” a graph can begin asking questions like:

- is price staying above an important level?
- is price moving upward over time?
- is the recent direction stronger than the earlier direction?

At the beginner level, you do not need advanced math to understand this idea. You only need to understand that repeated price values can show direction.

Later, this can be represented with moving-average-style nodes or other trend-focused tools. A moving average smooths price data by averaging prices across a chosen period, which can make the overall direction easier to read than raw daily moves alone.

That means even if your graph starts with a simple price comparison, it can later grow into something that reasons about trend more clearly.

:::sublecture { "id":"momentum-measures-how-strong-the-move-is", "title":"Momentum Measures How Strong The Move Is" }
# Momentum Measures How Strong The Move Is

If trend is about direction, momentum is about the strength or speed of that movement.

A stock can be rising slowly, rising quickly, falling slowly, or falling quickly. Momentum is the idea that price changes themselves can carry information about how strong that move is.

This does not mean momentum guarantees what will happen next. It simply gives another way to describe what price has been doing.

## A beginner way to think about momentum

You can think of momentum like this:

- trend asks where price has been moving
- momentum asks how strongly it has been moving

That distinction helps because two stocks might both be going up, but one may be moving much more strongly than the other.

In more advanced chart tools, momentum is often measured by comparing the current price to a price from some earlier period. If the current price is much higher than it was before, momentum may be positive. If it is much lower, momentum may be negative.

For now, the key idea is simpler than the formulas:

price is the raw value, trend is the broader direction, and momentum is the strength of that move.

Once you understand those three ideas, later indicator nodes will make much more sense.

:::sublecture { "id":"how-price-trend-and-momentum-fit-into-graphs", "title":"How Price, Trend, And Momentum Fit Into Graphs" }
# How Price, Trend, And Momentum Fit Into Graphs

The reason these ideas matter in SEND is that they can all be turned into graph logic.

Price can be fetched directly.

Trend can later be represented by nodes that smooth or compare price across time.

Momentum can later be represented by nodes that measure how strongly price has moved over a selected period.

Even before those advanced nodes exist, the reasoning already starts with a simple graph.

A fetched price becomes an input. A constant number becomes a rule. A comparison node asks whether the input is above or below that rule. That true-or-false result becomes a signal.

That may seem basic, but it is already the foundation of trend-based reasoning.

For example, a graph that checks whether price is above a chosen level is beginning to reason about strength. Later, if the graph checks whether price stays above a moving average, it is reasoning about trend. If it checks how far price has moved over a period, it is reasoning about momentum.

So while this lecture begins with price, it is really preparing you for more advanced ways of reading market movement.

## What to remember

Price is the starting value.

Trend is the broader direction shown by many prices over time.

Momentum is the strength of that movement.

In graphs, all three ideas can eventually become inputs, comparisons, and signals that help guide decisions.

:::

## Sources

- U.S. Securities and Exchange Commission, Investor.gov. *Introduction to Investing*. Helpful for grounding the lecture in beginner-friendly investing basics and the idea that market prices are a starting point for investment analysis.
- FINRA. *What Is Momentum Investing?* Used for the beginner-friendly explanation that momentum focuses on securities moving rapidly higher or lower and tries to capitalize on short-term price movement.
- Fidelity Learning Center. *What Are Momentum And Price Change?* Used for the definition that momentum measures the velocity of price changes rather than the price level alone.
- Fidelity Learning Center. *What Is SMA? - Simple Moving Average*. Used for the idea that simple moving averages smooth price data and are often used to help identify trend direction.
