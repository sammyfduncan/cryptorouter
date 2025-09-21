# CryptoRouter (WIP)

High performance Smart Order Router (SOR) for real-time crypto order execution
simulation across multiple CEXs. The engine connects to live market data feeds, normalises the data into a unified order book then calculates the optimal routing strategy to minimise slippage, achieving the best possible VWAP. 

Complex trading logic and high frequency financial data is handled through concurrency and scalability. Currently it's simulated, acting as a PoC, but may become a real tool once development is complete. 

### Main features

- **Real-time data ingestion** by connecting to WebSocket feeds of major CEXs to receive live level 2 order book data. 
- **Data normalisation** is handled by translating exchange-specific data formats into a standardised internal order book model
- **Consolidated order book** aggregates liquidity from all connected exchanges into a single view of the market
- **Optimal route calculation** implemented by a price/depth optimisation algorithm to find the best execution path for a simulated order, thereby minimising slippage
- **Web dashboard** provides a simple UI to visualise market data and the SORs routing decisions

### Tech 

- **Java 25 LTS**
- **Spring Boot 3**
- **Concurrency** handled with Reactive programming with project reactor 
- **Build tool** - Gradle Kotlin DSL
- **Data handling** with WebSocket clients 
- **Frontend** - TBD

### Architecture

Features a highly modular architecture:

1. **Connectors**: Each CEX has a dedicated connector module which establishes a WebSocket connection. It then subscribes to order book feeds and handles that exchanges specific data format. 
2. **Normalisation**: Each of those connectors translates the raw data into a standardised `NormalisedOrderBook` object.
3. **Consolidated order book**: A central module that connects to the streams of normalised data from all connectors and aggregates them into a single order book, then sorting it. This represents total market liquidity.
4. **Routing engine**: The core engine. It takes a simulated order request and queries the consolidated order book to find out the best execution plan across all possible options using the algorithms. 
5. **API layer**: Implemented with Spring WebFlux. Exposes endpoints for the  dashboard, so it can stream the book data and routing engine's choices. 


### Future 

After initial version is complete, the project will be expanded to be a CEX-DEX bridge, allowing for DEXs on-chain data to be included into the existing routing logic. 
