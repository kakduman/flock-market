### 1. Inventory and Currency Management:
* Players can add or remove in-game items or currency to/from their market account.
* Commands such as `/deposit_item <item> <quantity>` (or `/deposit_item` with an item in their hand) and `/deposit_money <amount>` allow players to manage their inventory and balance.
* The added items and money become part of their virtual market balance, used exclusively for trading in the bazaar.
### 2. Trading Platform (Web UI):
* A web-based UI where players can manage their "portfolios", place orders, and view some (albeit limited, I don't think I'm interested in fully fleshing this out as opposed to leaving it up to players) bazaar statistics.
* Should display a player’s holdings, open orders, transaction history, and other relevant information.
* Offers real-time data such as current prices, bid/ask spreads, trading volumes, etc.
### 3. Order Types:
* Limit Orders: Allows players to buy or sell items at a specified price. The trade executes when the market reaches the set price.
* Market Orders: Allows players to buy or sell items immediately at the current market price.
* Maybe some others, don't need it to be incredibly fleshed-out
### 4. API and Algorithmic Trading:
* An API that allows for programmatic access to trading operations and market data.
* Players can develop and deploy their trading algorithms, enabling automated trading based on predefined criteria.
* Ensure that the API is secure and has rate limits to prevent abuse.
* This is the primary reason I want to make this plugin and is uncompromisable. 
* I imagine this would appeal to the hardcore economy players but not the average player, and would be hidden in the web UI.
### 5. Other advanced features that appeal to hardcore econ players
* e.g. shorting, with a server-mandated deposit of variability\*duration\*item price so ppl can't go negative

Purpose here is more experimental than anything, as I think this would appeal to a very niche set of servers (maybe I'm wrong? Let me know if you personally would be interested). As a Birdflop project, the ultimate goal is to expand interest in CS/math fields by allowing ppl to gain experience with algorithms in a controlled setting. We'd hide advanced layers behind a layer of abstraction so most players would only see current buy/sell rates for the most part, allowing it to be an economy plugin that appeals to the masses. Maybe GUI or something to just immediately buy/sell items. A smart player (who would almost definitely get baltop 1) could play the role of a market maker and facilitate transactions for other players.

Outside of the experimental implications, benefits to the server include
- increased engagement from devs/technical individuals
- increased focus on economy for hard-economy servers
- increased competitiveness (as part of prior point)
- significantly easier for players to find a buyer/seller for resources, lowers barrier to economy entry

withdrawing, depositing, checking balance mostly complete (want to add a prohibited-items.yml and a way for admins to create new listable items, but the core functionality is there)

next step is to implement /create_listing, /remove_listing, /buy, /sell
then API
then GUI
then config w/ price floors, ceilings, prohibited items
then try to see how I can work with keeping the /bal economy instead of using a separate market economy?
