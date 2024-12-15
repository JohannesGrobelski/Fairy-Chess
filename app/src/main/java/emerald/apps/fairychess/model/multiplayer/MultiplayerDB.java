package emerald.apps.fairychess.model.multiplayer;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import emerald.apps.fairychess.controller.MainActivityListener;
import emerald.apps.fairychess.model.Chessgame;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiplayerDB {
    public static final double matchmakingWinningChanceOffset = 0.3; //0.0 ... 0.5
    public static final String TAG = "MultiplayerDB";

    //collection paths
    public static final String GAMECOLLECTIONPATH = "games_collection";
    public static final String PLAYERCOLLECTIONPATH = "players_collection";

    //fields in player (in player collection)
    public static final String PLAYER_ID = "name";
    public static final String PLAYER_GAMES_PLAYED = "played_games";
    public static final String PLAYER_GAMES_WON = "wins";
    public static final String PLAYER_GAMES_LOST = "losses";
    public static final String PLAYER_ELO = "elo";

    //fields in game (in game collection)
    public static final String GAMEFIELD_FINISHED = "finished";
    public static final String GAMEFIELD_GAMENAME = "gameName";
    public static final String GAMEFIELD_TIMEMODE = "timeMode";
    public static final String GAMEFIELD_FEN = "currentFen";  //save fen string after each move
    public static final String GAMEFIELD_PLAYER1ID = "player1ID";
    public static final String GAMEFIELD_PLAYER1Color = "player1Color";
    public static final String GAMEFIELD_PLAYER1ELO = "player1ELO";
    public static final String GAMEFIELD_PLAYER2ID = "player2ID";
    public static final String GAMEFIELD_PLAYER2Color = "player2Color";
    public static final String GAMEFIELD_PLAYER2ELO = "player2ELO";

    private MultiplayerDBSearchInterface multiplayerDBSearchInterface; //interface implemented by mainactivitylistener
    private MultiplayerDBGameInterface multiplayerDBGameInterface; //interface implemented by chessactivitylistener
    private FirebaseFirestore db;
    private Chessgame chessGame;

    public MultiplayerDB(MultiplayerDBGameInterface multiplayerDBGameInterface, Chessgame chessgame) {
        this.chessGame = chessgame;
        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();
        this.multiplayerDBGameInterface = multiplayerDBGameInterface;
    }

    public MultiplayerDB(MultiplayerDBSearchInterface multiplayerDBSearchInterface) {
        db = FirebaseFirestore.getInstance();
        this.multiplayerDBSearchInterface = multiplayerDBSearchInterface;
    }

    public MultiplayerDB(MultiplayerDBSearchInterface multiplayerDBSearchInterface, MultiplayerDBGameInterface multiplayerDBGameInterface) {
        db = FirebaseFirestore.getInstance();
        this.multiplayerDBSearchInterface = multiplayerDBSearchInterface;
        this.multiplayerDBGameInterface = multiplayerDBGameInterface;
    }

    public void shareGameId(String gameId) {
        //TODO: create alternative to dynamic link
        if (multiplayerDBSearchInterface != null) {
            multiplayerDBSearchInterface.processGameInvite(gameId);
        }
    }

    public void writeFenAfterMovement(String gameId, String fenAfterMovement){
        db.collection(GAMECOLLECTIONPATH).document(gameId).update(GAMEFIELD_FEN, fenAfterMovement);
    }


    public void searchForQuickmatch(String gameName, String userName) {
        List<MainActivityListener.GameSearchResult> resultList = new ArrayList<MainActivityListener.GameSearchResult>();
            db.collection(GAMECOLLECTIONPATH)
                .whereEqualTo(GAMEFIELD_FINISHED, false)
                .whereEqualTo(GAMEFIELD_PLAYER2ID, "")
                .whereNotEqualTo(GAMEFIELD_PLAYER1ID, userName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            resultList.add(new MainActivityListener.GameSearchResult(
                                document.getId(),
                                document.getString(GAMEFIELD_GAMENAME),
                                document.getString(GAMEFIELD_TIMEMODE),
                                document.getDouble(GAMEFIELD_PLAYER1ELO),
                                document.getString(GAMEFIELD_PLAYER2Color)
                            ));
                        }
                        if (multiplayerDBSearchInterface != null) {
                            multiplayerDBSearchInterface.onGameSearchComplete(resultList);
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }


    /**
     * Search for matching open games = documents with matching parameters (gameName and timeMode),
     * field finished = false and player1ID not matching player2ID. When search was successful call callback function
     * (onGameSearchComplete in MainActivityListener)
     */
    public void searchForOpenGames(String gameName, String timeMode, String player2ID) {
        List<MainActivityListener.GameSearchResult> resultList = new ArrayList<MainActivityListener.GameSearchResult>();
        Log.d(TAG, "search for games " + gameName + ", " + timeMode + ", " + player2ID);
        db.collection(GAMECOLLECTIONPATH)
            .whereEqualTo(GAMEFIELD_FINISHED, false)
            .whereEqualTo(GAMEFIELD_PLAYER2ID, "")
            .whereNotEqualTo(GAMEFIELD_PLAYER1ID, player2ID)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, document.getId() + " => " + document.getData());
                        resultList.add(new MainActivityListener.GameSearchResult(
                            document.getId(),
                            document.getString(GAMEFIELD_GAMENAME),
                            document.getString(GAMEFIELD_TIMEMODE),
                            document.getDouble(GAMEFIELD_PLAYER1ELO),
                            document.getString(GAMEFIELD_PLAYER2Color)
                        ));
                    }
                    if (multiplayerDBSearchInterface != null) {
                        multiplayerDBSearchInterface.onGameSearchComplete(resultList);
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });
    }

    /**
     * create open game = create document with matching gameName and timeMode and player1ID set.
     * when finished call the callback function (onCreateGame in MainActivityListener)
     * @return document_id of the gameMode
     */
    public void createGame(String gameName, String timeMode, String player1ID, double player1ELO) {
        // Create a new gameMode hashmap
        String player1Color = "WHITE"; //Color.randomColor()
        String player2Color = "BLACK"; //Color.oppositeColor(player1Color)
        Map<String, Object> gameHash = new HashMap<>();
        gameHash.put(GAMEFIELD_GAMENAME, gameName);
        gameHash.put(GAMEFIELD_FINISHED, false);
        gameHash.put(GAMEFIELD_TIMEMODE, timeMode);
        gameHash.put(GAMEFIELD_FEN, "");
        gameHash.put(GAMEFIELD_PLAYER1ID, player1ID);
        gameHash.put(GAMEFIELD_PLAYER1Color, player1Color);
        gameHash.put(GAMEFIELD_PLAYER1ELO, player1ELO);
        gameHash.put(GAMEFIELD_PLAYER2ID, "");
        gameHash.put(GAMEFIELD_PLAYER2Color, player2Color);

        // Add a new document with a generated ID
        db.collection(GAMECOLLECTIONPATH)
            .add(gameHash)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                if (multiplayerDBSearchInterface != null) {
                    multiplayerDBSearchInterface.onCreateGame(gameName, documentReference.getId(), player1Color);
                }
            })
            .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }

    /** Search for a user with userName and call onPlayerNameSearchComplete on success. */
    public void searchUsers(String userName) {
        List<String> resultList = new ArrayList<>();
        db.collection(PLAYERCOLLECTIONPATH)
            .whereEqualTo(PLAYER_ID, userName)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, document.getId() + " => " + document.getData());
                        resultList.add(document.getId());
                    }
                    if (multiplayerDBSearchInterface != null) {
                        multiplayerDBSearchInterface.onPlayerNameSearchComplete(userName, resultList.size());
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });
    }

    /** join the game with gameID by updating changeMap and if successful call getGameDataAndJoinGame */
    public void joinGame(String gameID, Map<String, Object> changeMap) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameID)
            .update(changeMap)
            .addOnSuccessListener(aVoid -> getGameDataAndJoinGame(gameID, changeMap.get(GAMEFIELD_PLAYER2ID).toString()));
    }

    public static class GameData {
        public String gameId;
        public String playerID;
        public String opponentID;
        public double playerELO;
        public double opponentELO;

        public GameData(String gameId, String playerID, String opponentID, double playerELO, double opponentELO) {
            this.gameId = gameId;
            this.playerID = playerID;
            this.opponentID = opponentID;
            this.playerELO = playerELO;
            this.opponentELO = opponentELO;
        }
    }

    /** get GameData for gameID and if successful call onJoinGame */
    public void getGameDataAndJoinGame(String gameId, String userName){
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String player1ID = documentSnapshot.getString(GAMEFIELD_PLAYER1ID);
                String player2ID = documentSnapshot.getString(GAMEFIELD_PLAYER2ID);
                double player1ELO = documentSnapshot.getDouble(GAMEFIELD_PLAYER1ELO);
                double player2ELO = documentSnapshot.getDouble(GAMEFIELD_PLAYER2ELO);
                String opponentID = player1ID;
                double playerELO = player2ELO;
                double opponentELO = player1ELO;
                if(opponentID.equals(userName)){
                    opponentID = player2ID;
                    opponentELO = player2ELO;
                }
                GameData gameData = new GameData(gameId, userName, opponentID, playerELO, opponentELO);

                String name = documentSnapshot.getString(GAMEFIELD_GAMENAME);
                String playMode = "human";
                String time = documentSnapshot.getString(GAMEFIELD_TIMEMODE);
                String playerColor = documentSnapshot.getString(GAMEFIELD_PLAYER1Color);
                if(userName.equals(player2ID)) playerColor = documentSnapshot.getString(GAMEFIELD_PLAYER2Color);
                MainActivityListener.GameParameters gameParameters = new MainActivityListener.GameParameters(name, playMode, time, playerColor, 0);

                if (multiplayerDBSearchInterface != null) {
                    multiplayerDBSearchInterface.onJoinGame(gameParameters, gameData);
                }
            })
            .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }

    /** create player with playerID and default player data, on success call onCreatePlayer */
    public void createPlayer(String playerID) {
        Map<String, Object> playerHash = new HashMap<>();
        playerHash.put(PLAYER_ELO, 400);
        playerHash.put(PLAYER_ID, playerID);
        playerHash.put(PLAYER_GAMES_WON, 0);
        playerHash.put(PLAYER_GAMES_LOST, 0);
        playerHash.put(PLAYER_GAMES_PLAYED, 0);

        // Add a new document with a generated ID
        db.collection(PLAYERCOLLECTIONPATH)
            .add(playerHash)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                if (multiplayerDBSearchInterface != null) {
                    multiplayerDBSearchInterface.onCreatePlayer(playerID);
                }
            })
            .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }

    public static class GameState {
        public boolean gameFinished;
        public String currentFen;

        public GameState(boolean gameFinished, String currentFen) {
            this.gameFinished = gameFinished;
            this.currentFen = currentFen;
        }
    }

    /** add snapshotlistener to listen to changes in a created game without second player
     *  if change occurs call onGameChanged */
    public void listenToGameSearch(String gameId) {
        ListenerRegistration registration = db.collection(GAMECOLLECTIONPATH).document(gameId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    if (multiplayerDBSearchInterface != null) {
                        multiplayerDBSearchInterface.onGameChanged(snapshot.getId());
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            });
    }

    /** add snapshotlistener to listen to changes on game (currentFen and gamestatus)
     *  if change occurs call readGameState */
    public void listenToGameIngame(String gameId) {
        ListenerRegistration registration = db.collection(GAMECOLLECTIONPATH).document(gameId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    readGameState(gameId);
                } else {
                    Log.d(TAG, "Current data: null");
                }
            });
    }

    /** check whether second player has joined in game with gameID
     *  if so call getGameDataAndJoinGame */
    public void hasSecondPlayerJoined(String gameId) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String player1ID = document.getString(GAMEFIELD_PLAYER1ID);
                        String player2ID = document.getString(GAMEFIELD_PLAYER2ID);
                        if (!player1ID.isEmpty() && !player2ID.isEmpty()) {
                            getGameDataAndJoinGame(gameId, player1ID);
                        }
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });
    }

    /** finish game by updating variable finished to true, on success call onFinishGame */
    public void finishGame(String gameId, String cause) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .update(GAMEFIELD_FINISHED, true)
            .addOnSuccessListener(aVoid -> {
                if (multiplayerDBGameInterface != null) {
                    multiplayerDBGameInterface.onFinishGame(gameId, cause);
                }
            });
    }

    /** read game state of game with gameID (document,finished,currentFen) and on sucess call onGameChanged,
     * (because this is only called if snapshot listener detects change in game document)*/
    public void readGameState(String gameId) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        boolean finished = document.getBoolean(GAMEFIELD_FINISHED);
                        String currentFen = document.getString(GAMEFIELD_FEN);
                        GameState gameState = new GameState(finished, currentFen);
                        if (multiplayerDBGameInterface != null) {
                            multiplayerDBGameInterface.onGameChanged(gameId, gameState);
                        }
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });
    }

    /** parcelable class PlayerStats that save statistics of player*/
    public static class PlayerStats implements Parcelable {
        public long games_played;
        public long games_won;
        public long games_lost;
        public double ELO;

        public PlayerStats(long games_played, long games_won, long games_lost, double ELO) {
            this.games_played = games_played;
            this.games_won = games_won;
            this.games_lost = games_lost;
            this.ELO = ELO;
        }

        protected PlayerStats(Parcel in) {
            games_played = in.readLong();
            games_won = in.readLong();
            games_lost = in.readLong();
            ELO = in.readDouble();
        }

        public static final Creator<PlayerStats> CREATOR = new Creator<PlayerStats>() {
            @Override
            public PlayerStats createFromParcel(Parcel in) {
                return new PlayerStats(in);
            }

            @Override
            public PlayerStats[] newArray(int size) {
                return new PlayerStats[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(games_played);
            dest.writeLong(games_won);
            dest.writeLong(games_lost);
            dest.writeDouble(ELO);
        }

        public static PlayerStats getDefault() {
            return new PlayerStats(0L, 0L, 0L, 0.0);
        }
    }

    /** get playerStats for player document with playerID (gamesPlayed,gamesWon,gamesLost and ELO) and on success call onGetPlayerstats*/
    public void getPlayerStats(String playerID) {
        db.collection(PLAYERCOLLECTIONPATH)
            .whereEqualTo(PLAYER_ID, playerID)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        long gamesPlayed = document.getLong(PLAYER_GAMES_PLAYED);
                        long gamesWon = document.getLong(PLAYER_GAMES_WON);
                        long gamesLost = document.getLong(PLAYER_GAMES_LOST);
                        double elo = document.getDouble(PLAYER_ELO);
                        if (multiplayerDBSearchInterface != null) {
                            multiplayerDBSearchInterface.onGetPlayerstats(new PlayerStats(gamesPlayed, gamesWon, gamesLost, elo));
                        }
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });
    }

    /** set playerStats for player document with playerID (gamesPlayed,gamesWon,gamesLost and ELO) and on success call onSetPlayerstats*/
    public void setPlayerStats(String playerID, PlayerStats playerStats, String cause) {
        //get doc id
        db.collection(PLAYERCOLLECTIONPATH)
            .whereEqualTo(PLAYER_ID, playerID)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        String docID = document.getId();
                        db.collection(PLAYERCOLLECTIONPATH)
                            .document(docID)
                            .update(
                                PLAYER_GAMES_PLAYED, playerStats.games_played,
                                PLAYER_GAMES_WON, playerStats.games_won,
                                PLAYER_GAMES_LOST, playerStats.games_lost,
                                PLAYER_ELO, playerStats.ELO
                            )
                            .addOnSuccessListener(aVoid -> {
                                if (multiplayerDBGameInterface != null) {
                                    multiplayerDBGameInterface.onSetPlayerstats(cause);
                                }
                            })
                            .addOnFailureListener(e -> Log.e("MultiplayerDB", "could not set player stats: " + e.getCause()));
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });

    }

    /** update GAMEFIELD_FINISHED to true in game with gameID */
    public void cancelGame(String gameId) {
        // Add a new document with a generated ID
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .update(GAMEFIELD_FINISHED, true);
    }
}
