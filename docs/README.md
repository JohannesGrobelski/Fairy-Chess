# Fairy-Chess
Play many variations of Fairy Chess against the computer or with friends.

## Firebase/Firestore Integration
The multiplayer functionality is powered by Firebase Firestore, which enables real-time game synchronization between players. The database consists of two main collections:
- `players_collection`: Stores player data including ELO ratings, wins, losses, and total games played
- `games_collection`: Manages active and completed games, storing game states, moves, player information, and time controls

Players can create new games that are automatically listed for others to join, or share specific game IDs to invite friends. The game state updates in real-time as moves are made, using Firestore's snapshot listeners. When a game concludes, player statistics are automatically updated in the database.

### Firebase Setup
1. Create a new project at [Firebase Console](https://console.firebase.google.com/)
2. Enable Firestore Database in native mode for your region
3. Create two collections: `players_collection` and `games_collection`
4. Set Firestore security rules to allow read/write access:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /players_collection/{document} {
         allow read, write: if true;
       }
       match /games_collection/{document} {
         allow read, write: if true;
       }
     }
   }**

#### Firebase Indexes
The app requires a composite index for game searches. To set this up:

1. The required composite index for `games_collection`:
   - `finished` (Ascending)
   - `player2ID` (Ascending)
   - `player1ID` (Ascending)
   - `name` (Ascending)

Note: Firebase provides automatic index creation links in error messages (task.exception). When you run a query that needs an index, Firebase will provide a direct link in the error message to create the exact index needed,
but it is recommended to create these before.

### Firebase Free Tier Limitations
The free tier of Firebase Firestore has the following daily limits:
- 50,000 document reads
- 20,000 document writes
- 20,000 document deletes
- 1 GiB total stored data
- 10 GiB network egress/month

Given that each chess game:
- Requires approximately 40 reads and writes per game (based on average move count)
- Requires 1 delete operation per completed game

The free tier can support approximately:
- ca 500 games per day (limited by write operations)
- ~20-21 games per hour
- A new game every 3 minutes

For applications expecting higher usage, consider:
1. Upgrading to a paid tier
2. Implementing caching strategies
3. Batching updates where possible
4. Cleaning up completed games efficiently

Note: These are approximate calculations and actual limits may vary based on:
- Game length/complexity
- Additional features requiring operations
- Distribution of games throughout the day