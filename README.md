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