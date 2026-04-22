:::lecture { "id": "logic--getting-started--understanding-the-replay-system", "slug": "understanding-the-replay-system", "path": { "slug": "logic", "title": "Logic", "description": "Reasoning-focused lessons and graph-building fundamentals." }, "category": { "slug": "getting-started", "title": "Getting Started" }, "title": "Understanding The Replay System", "summary": "Learn how the sandbox replay system lets you step through a strategy day by day and inspect what happened.", "estimatedMinutes": 11 }

:::sublecture { "id":"what-the-replay-system-is", "title":"What The Replay System Is" }
# What The Replay System Is

Once a strategy has been built, the next question is no longer just **what does this graph mean?** It becomes **what did this graph actually do over time?**

That is the purpose of the replay system.

The replay system is available inside the sandbox. It lets you move through the execution of your strategy one day at a time and inspect what happened across the selected date range. Rather than only seeing a final result, you can step from the start date to the end date and watch how the strategy behaved on each day.

For every day in the replay, you can inspect what values were fetched, what signals or conditions became true or false, whether an action tried to execute, and what effect that had on the portfolio.

This matters because a graph may look correct on the canvas, but replay shows how that logic behaved when it was actually run across time.

## What replay helps you understand

As you move through the replay, you are usually trying to understand questions like these:

- what value did the graph fetch on this day?
- what signal or comparison result came out of the logic?
- did a buy or sell happen, or did nothing execute?
- how much cash was left after that day?
- what assets or open positions existed on that day?
- how did profit and loss change over time?

:::sublecture { "id":"reading-the-replay-day-by-day", "title":"Reading The Replay Day By Day" }
# Reading The Replay Day By Day

The easiest way to understand replay is to read it one day at a time.

Each selected day is a snapshot of what the strategy saw and what it produced on that date. As you move through the timeline, the replay view updates to show the fetched values, the graph outputs, and the portfolio state for that moment.

That means you are not just looking at the strategy in general. You are looking at one specific day in its execution.

A fetched price may appear for that day. A comparison node may show true or false. An action node may execute, or it may show that no execution happened because the conditions were not fully met.

For example, a sell path may receive a signal but still not produce a trade if there is no open position available to sell. That kind of result is important, because it helps explain not only what the logic said, but what actually happened.

## A useful way to read each day

When you stop on a day in replay, it helps to read it in this order:

1. start with the fetched values  
2. check the important signals or outputs  
3. see whether an action executed  
4. look at cash, holdings, and positions afterward 

:::sublecture { "id":"following-executions-across-the-range", "title":"Following Executions Across The Range" }
# Following Executions Across The Range

Replay becomes even more useful when you stop looking at each day as an isolated snapshot and start following the strategy across the full run.

From the start date to the end date, you can move through the execution and see when trades occurred, what price was involved, and how those trades changed the portfolio. Some days may have no execution at all. Other days may contain a buy or sell that changes your cash, your open positions, and the market value of what you hold.

This is what makes replay so useful for understanding timing.

A final result can tell you whether a strategy ended up positive or negative, but it does not tell you when the strategy entered, when it exited, or how the portfolio changed along the way. Replay gives you that missing story.

## What replay lets you track over time

As you move through the replay range, you can follow:

- when trades happened
- the price used for each trade
- how cash changed after each execution
- how assets or positions changed over time
- what the portfolio looked like on each day

That makes it much easier to understand why the strategy ended where it did.

:::sublecture { "id":"understanding-balances-and-pl", "title":"Understanding Balances And P/L" }
# Understanding Balances And P/L

One of the most important parts of replay is that it does not only show node behavior. It also shows the portfolio state that came out of that behavior.

Across the replay, you can inspect values like cash, assets, equity, and trade activity. This lets you connect the graph’s decisions to actual portfolio results instead of viewing them as separate things.

You can also follow profit and loss over time.

Replay shows both **realized P/L** and **unrealized P/L**.

Realized P/L comes from gains or losses that have already been locked in through completed trades. Unrealized P/L comes from positions that are still open and whose value is still changing with the market.

Both matter, because they tell different parts of the story.

A strategy may look strong because unrealized gains are high, but those gains are not final yet. Another strategy may have smaller unrealized value but stronger realized results because profitable trades have already been closed.

## Why the full range matters

This is one reason replay should be read across the whole date range, not only at the end.

A strategy might finish with a strong result, but replay may show that it went through long weak periods before recovering. Another strategy may end with only a moderate result but show much steadier behavior throughout.

By stepping through the full replay, you get a more honest picture of how the strategy behaved across time.

:::sublecture { "id":"what-to-remember-about-replay", "title":"What To Remember About Replay" }
# What To Remember About Replay

The replay system is a sandbox feature that lets you inspect the execution of a strategy day by day.

It shows what the graph fetched on each day, what outputs or signals were produced, whether actions executed, when trades occurred, what trade prices were used, and how cash and holdings changed afterward.

It also gives you a running view of realized and unrealized profit and loss, so you can understand both the daily portfolio state and the final result at the end of the replay.

That means replay is not just a playback feature. It is a way to understand how a strategy behaved across time.

It helps answer the most important questions after building a strategy:

- what did the graph see?
- what did the graph decide?
- what changed because of that decision?
- what result did that create over time?
