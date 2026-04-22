:::lecture { "id": "economics--getting-started--what-are-economic-indicators", "slug": "what-are-economic-indicators", "path": { "slug": "economics", "title": "Economics", "description": "Beginner-friendly lessons that teach economic ideas through graphs, nodes, and strategy building." }, "category": { "slug": "getting-started", "title": "Getting Started" }, "title": "What Are Economic Indicators?", "summary": "Learn what economic indicators are, why they matter, and how they can become inputs in a strategy graph.", "estimatedMinutes": 12 }

:::sublecture { "id":"what-an-economic-indicator-is", "title":"What An Economic Indicator Is" }
# What An Economic Indicator Is

When people talk about the economy or the market, they are usually talking about information.

That information might describe how expensive things are becoming, whether businesses are growing, whether people are spending more money, or whether a company is earning more than before. Economic indicators are the measurements people use to track those kinds of changes.

In simple terms, an economic indicator is a value that helps describe what is happening in the economy, the market, or a business.

Some indicators are very broad. They describe large parts of the economy, such as inflation, interest rates, unemployment, or economic growth. Other indicators are more focused. They might describe a single company, such as earnings, revenue, or profit. Some indicators are directly tied to the market itself, such as price.

What makes indicators important is that they help turn a vague idea into something measurable. Instead of saying, "the economy feels strong," an indicator gives you a value you can actually inspect and reason about.

## Why indicators matter in a graph system

In SEND, indicators are not just facts to read about. They can become part of the graph.

A fetch node can bring indicator data into the graph. A constant node can hold a target level or threshold. A comparison node can check whether the fetched value is above, below, or equal to that level.

That means an indicator can move from being something you understand conceptually to something your strategy can actually use.

Instead of only saying, "this indicator matters," you begin asking a more useful question:

**How could this indicator affect a decision?**

That is the beginning of economic reasoning inside a graph.

:::sublecture { "id":"common-beginner-indicators", "title":"Common Beginner Indicators" }
# Common Beginner Indicators

At the beginning, it helps to think about indicators in a few simple groups.

## Market indicators

These are values tied closely to market movement itself.

Examples include:

- stock price
- volume
- price change over time

These are often the easiest indicators to understand because they are directly connected to what a stock has been doing.

## Company indicators

These describe the business behind the stock.

Examples include:

- revenue
- earnings
- profit
- P/E ratio

These indicators help answer questions about how a company is performing, not just how its stock price is moving.

## Bigger-economy indicators

These describe larger economic conditions.

Examples include:

- inflation
- interest rates
- unemployment
- economic growth

These indicators matter because markets do not move in isolation. The bigger economy affects companies, investors, and market behavior.

## The main idea to remember

Even though these indicators describe different things, they all serve the same purpose: they give you information that can shape a decision.

In a graph system, that means they can become inputs that later nodes use to produce signals and actions.

:::checkpoint
{
  "id": "checkpoint-identify-indicator-types",
  "title": "Checkpoint 1: Identify Different Kinds Of Indicators",
  "instructions": [
    "Look at the example indicators shown in the lesson.",
    "Notice that some describe markets, some describe companies, and some describe the larger economy.",
    "Keep in mind that all of them can become inputs in a graph."
  ],
  "tasks": [
    {
      "id": "task-recognize-market-indicator",
      "label": "Recognize a market indicator",
      "description": "Notice that values like stock price describe market movement directly."
    },
    {
      "id": "task-recognize-company-indicator",
      "label": "Recognize a company indicator",
      "description": "Notice that values like earnings or revenue describe the business itself."
    },
    {
      "id": "task-recognize-macro-indicator",
      "label": "Recognize a larger-economy indicator",
      "description": "Notice that values like inflation or interest rates describe broad economic conditions."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "const_number", "gt", "buy"],
    "starterNodes": [],
    "starterEdges": []
  },
  "validation": []
}
:::

:::sublecture { "id":"how-an-indicator-becomes-a-signal", "title":"How An Indicator Becomes A Signal" }
# How An Indicator Becomes A Signal

An indicator by itself is just a value.

A strategy begins when the graph asks a question about that value.

For example, if a fetched price is 210, that number alone does not tell the graph what to do. But once the graph compares 210 to a rule like 200, the value starts to mean something.

Now the graph can ask:

**Is the price greater than 200?**

That kind of question turns a raw value into a signal.

This is one of the most important ideas in the economics path. Economic indicators matter because they can be turned into strategy logic.

## The basic graph pattern

A beginner graph often follows this pattern:

1. fetch an indicator  
2. compare it to a constant  
3. use the result as a signal  
4. send that signal toward an action  

That pattern works whether the indicator is stock price, earnings, inflation, or something else.

The indicator may change, but the logic structure stays familiar.

## A first example

Imagine a graph that fetches a market value, compares it to a constant number, and then uses the result to decide whether a buy path should be allowed.

In that case:

- the fetch node provides the indicator
- the constant node provides the rule
- the comparison node turns the value into a true-or-false result
- the action node receives that signal

That is how graphs turn information into decisions.

:::checkpoint
{
  "id": "checkpoint-build-indicator-signal",
  "title": "Checkpoint 2: Turn An Indicator Into A Signal",
  "instructions": [
    "Add a Fetch Price Data node.",
    "Add a Constant Number node.",
    "Add a Greater Than node.",
    "Connect Fetch Price Data into Greater Than.",
    "Connect Constant Number into Greater Than."
  ],
  "tasks": [
    {
      "id": "task-add-indicator-node",
      "label": "Add Fetch Price Data",
      "description": "Place a fetch node on the canvas so the graph has an indicator input."
    },
    {
      "id": "task-add-threshold-node",
      "label": "Add Constant Number",
      "description": "Place a constant node to represent a simple rule or threshold."
    },
    {
      "id": "task-add-comparison-node",
      "label": "Add Greater Than",
      "description": "Place a comparison node so the graph can evaluate the indicator."
    },
    {
      "id": "task-connect-indicator-to-comparison",
      "label": "Connect indicator to comparison",
      "description": "Wire Fetch Price Data into Greater Than."
    },
    {
      "id": "task-connect-threshold-to-comparison",
      "label": "Connect threshold to comparison",
      "description": "Wire Constant Number into Greater Than."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "const_number", "gt", "buy"],
    "starterNodes": [],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_exists", "nodeType": "fetch_price" },
    { "type": "node_exists", "nodeType": "const_number" },
    { "type": "node_exists", "nodeType": "gt" },
    { "type": "connection_exists", "sourceType": "fetch_price", "targetType": "gt" },
    { "type": "connection_exists", "sourceType": "const_number", "targetType": "gt" }
  ]
}
:::

:::sublecture { "id":"why-this-matters-for-strategy-building", "title":"Why This Matters For Strategy Building" }
# Why This Matters For Strategy Building

The point of learning indicators is not to memorize definitions.

The point is to understand what kind of information a graph can use.

Once you understand that, economic ideas start becoming much more practical. You stop thinking only in terms of facts and start thinking in terms of strategy questions.

Instead of saying:

- inflation is important
- earnings are important
- price matters

You begin saying:

- what node would fetch that value?
- what rule would I compare it against?
- what signal would that create?
- what action could that signal affect?

That is the shift this lecture is meant to introduce.

Economic indicators are useful because they give your graph something meaningful to react to.

## What to remember

An economic indicator is a measurable value that describes something important about the market, a business, or the economy.

In SEND, indicators can become graph inputs.

Once an indicator is fetched into a graph, it can be compared, turned into a signal, and used to help guide a strategy.

That means indicators are not only something to understand.

They are something you can build with.
