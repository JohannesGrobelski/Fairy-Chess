package emerald.apps.fairychess.controller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import emerald.apps.fairychess.R;
import emerald.apps.fairychess.databinding.ActivityMainBinding;
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB;
import emerald.apps.fairychess.model.multiplayer.MultiplayerDBSearchInterface;
import emerald.apps.fairychess.model.rating.ChessRatingSystem;
import emerald.apps.fairychess.view.ChessActivity;
import emerald.apps.fairychess.view.ChessDesignerActivity;
import emerald.apps.fairychess.view.MainActivity;
import emerald.apps.fairychess.view.fragments.main.ProfileViewModel;

public class MainActivityListener implements View.OnClickListener, MultiplayerDBSearchInterface {
    private MainActivity mainActivity;
    private ActivityMainBinding binding;
    private MultiplayerDB multiplayerDB;
    private ProfileViewModel viewModel;

    private AlertDialog userNameDialog;
    private AlertDialog joinWaitDialog;
    private AlertDialog createAIGameDialog;
    private AlertDialog createHumanGameDialog;
    private Map<String, Boolean> launchedGamesMap = new HashMap<>();

    private String userName;
    private MultiplayerDB.PlayerStats playerStats;
    private MultiplayerDB.PlayerStats opponentStats;

    public static class GameSearchResult {
        public String id;
        public String gameName;
        public String timeMode;
        public double player1ELO;
        public String player2Color;

        public GameSearchResult(String id, String gameName, String timeMode, double player1ELO, String player2Color) {
            this.id = id;
            this.gameName = gameName;
            this.timeMode = timeMode;
            this.player1ELO = player1ELO;
            this.player2Color = player2Color;
        }
    }

    public static class GameParameters {
        public String name;
        public String playMode;
        public String time;
        public String playerColor;
        public int difficulty;

        public GameParameters(String name, String playMode, String time, String playerColor, int difficulty) {
            this.name = name;
            this.playMode = playMode;
            this.time = time;
            this.playerColor = playerColor;
            this.difficulty = difficulty;
        }
    }

    private GameParameters gameParameters = new GameParameters("", "", "", "", 0);
    private String createdGameID = "";

    public static final boolean DEBUG = false;
    public static final String TAG = "MainActivityListener";
    public static final String userNameExtra = "userNameExtra";
    public static final String gameIdExtra = "gameId";
    public static final String gamePlayerNameExtra = "playerNameExtra";
    public static final String gamePlayerStatsExtra = "playerStatsExtra";
    public static final String gameOpponentNameExtra = "opponentNameExtra";
    public static final String gameOpponentStatsExtra = "opponentStatsExtra";
    public static final String gameModeExtra = "gameMode";
    public static final String gameNameExtra = "name";
    public static final String gameTimeExtra = "gameTime";
    public static final String gameDifficultyExtra = "gameDifficultyExtra";
    public static final String playerColorExtra = "playerColor";

    public MainActivityListener(MainActivity mainActivity, Intent intent) {
        this.mainActivity = mainActivity;
        loadOrCreateUserName();
        multiplayerDB = new MultiplayerDB(this);
        loadPlayerStats();
        handleDeepLink(intent);
        viewModel = new ViewModelProvider(mainActivity).get(ProfileViewModel.class);
    }

    public void onResume() {
        loadPlayerStats();
    }

    public void loadPlayerStats(){
        playerStats = MultiplayerDB.PlayerStats.getDefault();
        tryFirebaseOperation(() -> multiplayerDB.getPlayerStats(userName));
    }


    @Override
    public void onClick(View v) {
        if (v != null) {
            int viewId = v.getId();

            if (viewId == R.id.btn_ai) {
                gameParameters.playMode = "ai";
                displayAlertDialogAIMatch();
            } else if (viewId == R.id.btn_searchGame) {
                gameParameters.playMode = "human";
                displayAlertDialogSearchForGames();
            } else if (viewId == R.id.btn_createGame) {
                gameParameters.playMode = "human";
                displayAlertDialogCreateOnlineGame();
            } else if (viewId == R.id.btn_chess_creator_new) {
                Intent intent = new Intent(mainActivity, ChessDesignerActivity.class);
                mainActivity.startActivity(intent);
            } else if (viewId == R.id.btn_chess_creator_edit) {
                // TODO: open popup of variant to edit and then
                // String variant_edit = "";
                // Intent intent = new Intent(mainActivity, ChessDesignerActivity.class);
                // intent.putExtra("variant", variant_edit);
                // mainActivity.startActivity(intent);
            } else if (viewId == R.id.tv_playerstats) {
                if (this.playerStats != null) {
                    display_alertDialogPlayerStats();
                } else {
                    Toast.makeText(mainActivity, "loading player stats", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void display_alertDialogPlayerStats(){
        LayoutInflater inflater = LayoutInflater.from(mainActivity);

        //inflate the layout (depending on mode)
        View playerStatsDialogView = inflater.inflate(
            R.layout.alertdialog_playerstats,
            null,
            false
        );

        TextView playerNameTextView = playerStatsDialogView.findViewById(R.id.aD_playerStats_playerName);
        playerNameTextView.setText("playername : " + userName);
        TextView eloTextView = playerStatsDialogView.findViewById(R.id.aD_playerStats_ELO);
        eloTextView.setText("ELO : " + playerStats.ELO);
        TextView gamesPlayedTextView = playerStatsDialogView.findViewById(R.id.aD_playerStats_gamesPlayed);
        gamesPlayedTextView.setText("games played : " + playerStats.games_played);
        TextView gamesWonTextView = playerStatsDialogView.findViewById(R.id.aD_playerStats_gamesWon);
        gamesWonTextView.setText("games won : " + playerStats.games_won);
        TextView gamesLostTextView = playerStatsDialogView.findViewById(R.id.aD_playerStats_gamesLost);
        gamesLostTextView.setText("games lost : " + playerStats.games_lost);

        AlertDialog.Builder playerStatsDialogBuilder = new AlertDialog.Builder(mainActivity).setView(playerStatsDialogView);
        playerStatsDialogBuilder.setPositiveButton("OK", null);
        playerStatsDialogBuilder.show();
    }

    public void displayAlertDialogAIMatch(){
        String[] colors = mainActivity.getResources().getStringArray(R.array.colors);
        String[] gamemode_descriptions = mainActivity.getResources().getStringArray(R.array.gamemode_descriptions);
        String[] timeModes = mainActivity.getResources().getStringArray(R.array.timemodes);
        String[] difficultyModes = mainActivity.getResources().getStringArray(R.array.difficultyModes);
        String[] gamemodes = mainActivity.getResources().getStringArray(R.array.gamemodes);
        LayoutInflater inflater = LayoutInflater.from(mainActivity);

        //inflate the layout (depending on mode)
        View createDialogView = inflater.inflate(
            R.layout.alertdialog_create_game,
            null,
            false
        );

        //create dialog
        Spinner spinner_gameName = createDialogView.findViewById(R.id.spinner_createGame_gameName);
        spinner_gameName.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            gamemodes
        ));

        // Update TextView description when a game mode is selected
        TextView tv_gameMode_description = createDialogView.findViewById(R.id.tv_gameMode_description);
        spinner_gameName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                for (String desc : gamemode_descriptions) {
                    if (desc.split(":")[0].toLowerCase(Locale.ROOT).equals(spinner_gameName.getSelectedItem().toString().toLowerCase())) {
                        tv_gameMode_description.setText(desc);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tv_gameMode_description.setText("Select a game mode to see its description");
            }
        });

        Spinner spinner_timemode = createDialogView.findViewById(R.id.spinner_createGame_timemode);
        spinner_timemode.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            timeModes
        ));
        Spinner spinner_diff = createDialogView.findViewById(R.id.spinner_createGame_difficulty);
        spinner_diff.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            difficultyModes
        ));

        Spinner spinner_color = createDialogView.findViewById(R.id.spinner_createGame_color);
        spinner_color.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            colors
        ));

        Button btn_create_game = createDialogView.findViewById(R.id.btn_createGame_create_game);

        // Create dialog with blur theme
        createAIGameDialog = new AlertDialog.Builder(mainActivity, R.style.BlurDialogTheme)
            .setView(createDialogView)
            .create();

        // Apply background and other window attributes
        if (createAIGameDialog.getWindow() != null) {
            createAIGameDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            createAIGameDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            setBlurRadius(15);
        }

        btn_create_game.setOnClickListener(v -> {
            gameParameters.name = spinner_gameName.getSelectedItem().toString().toLowerCase();
            gameParameters.time = spinner_timemode.getSelectedItem().toString();
            gameParameters.playerColor = spinner_color.getSelectedItem().toString().toLowerCase();
            double diffAi = Double.parseDouble(spinner_diff.getSelectedItem().toString().split(" ")[1]);
            gameParameters.difficulty = Integer.parseInt(spinner_diff.getSelectedItem().toString().split(" ")[1]);
            this.opponentStats = new MultiplayerDB.PlayerStats(0L, 0L, 0L, diffAi);
            MultiplayerDB.GameData gameData = new MultiplayerDB.GameData("aigame", userName, "AI", playerStats.ELO, diffAi);
            startGameWithParameters(gameData, gameParameters);
        });

        createAIGameDialog.show();
    }

    public void displayAlertDialogSearchForGames(){
        String[] colors = mainActivity.getResources().getStringArray(R.array.colors);
        String[] gameModes = mainActivity.getResources().getStringArray(R.array.gamemodes);
        String[] timeModes = mainActivity.getResources().getStringArray(R.array.timemodes);
        LayoutInflater inflater = LayoutInflater.from(mainActivity);

        //inflate the layout (depending on mode)
        View searchDialogView = inflater.inflate(
            R.layout.alertdialog_search_game,
            null,
            false
        );

        //create dialog
        Button btn_quickmatch = searchDialogView.findViewById(R.id.btn_quickmatch);
        btn_quickmatch.setOnClickListener(v -> {
            if (createAIGameDialog != null) {
                createAIGameDialog.show();
            }
            gameParameters.playMode = "human";
            quickMatch();
        });

        Spinner spinner_gameName = searchDialogView.findViewById(R.id.spinner_gameName);
        spinner_gameName.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            gameModes
        ));
        Spinner spinner_timemode = searchDialogView.findViewById(R.id.spinner_timemode);
        spinner_timemode.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            timeModes
        ));

        Spinner spinner_color = searchDialogView.findViewById(R.id.spinner_createGame_color);
        spinner_color.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            colors
        ));

        Button btn_search_game = searchDialogView.findViewById(R.id.btn_search_game);

        // Create dialog with blur theme
        AlertDialog searchDialog = new AlertDialog.Builder(mainActivity, R.style.BlurDialogTheme)
            .setView(searchDialogView)
            .create();

        // Apply background and other window attributes
        if (searchDialog.getWindow() != null) {
            searchDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            searchDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            setBlurRadius(15);
        }

        btn_search_game.setOnClickListener(v -> {
            gameParameters.name = spinner_gameName.getSelectedItem().toString().toLowerCase();
            gameParameters.time = spinner_timemode.getSelectedItem().toString();
            gameParameters.playerColor = spinner_color.getSelectedItem().toString().toLowerCase();
            searchForGames(gameParameters.name, gameParameters.time);
        });
        searchDialog.show();
    }

    public void displayAlertDialogCreateOnlineGame(){
        String[] colors = mainActivity.getResources().getStringArray(R.array.colors);
        String[] gameModes = mainActivity.getResources().getStringArray(R.array.gamemodes);
        String[] timeModes = mainActivity.getResources().getStringArray(R.array.timemodes);
        LayoutInflater inflater = LayoutInflater.from(mainActivity);
        String[] gamemodeDescriptions = mainActivity.getResources().getStringArray(R.array.gamemode_descriptions);

        //inflate the layout (depending on mode)
        View createGameDialogView = inflater.inflate(
            R.layout.alertdialog_create_game,
            null,
            false
        );

        //create dialog
        Spinner spinner_gameName = createGameDialogView.findViewById(R.id.spinner_createGame_gameName);
        spinner_gameName.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            gameModes
        ));
        Spinner spinner_timemode = createGameDialogView.findViewById(R.id.spinner_createGame_timemode);
        spinner_timemode.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            timeModes
        ));

        Spinner spinner_color = createGameDialogView.findViewById(R.id.spinner_createGame_color);
        spinner_color.setAdapter(new ArrayAdapter<>(
            mainActivity,
            android.R.layout.simple_list_item_1,
            colors
        ));

        // Update TextView description when a game mode is selected
        TextView tv_gameMode_description = createGameDialogView.findViewById(R.id.tv_gameMode_description);
        spinner_gameName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                for (String desc : gamemodeDescriptions) {
                    if (desc.split(":")[0].toLowerCase(Locale.ROOT).equals(spinner_gameName.getSelectedItem().toString().toLowerCase())) {
                        tv_gameMode_description.setText(desc);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tv_gameMode_description.setText("Select a game mode to see its description");
            }
        });

        //disable spinner difficulty as this isnt used in human vs human matches
        Spinner spinner_difficultymode = createGameDialogView.findViewById(R.id.spinner_createGame_difficulty);
        spinner_difficultymode.setVisibility(View.GONE);

        // Create dialog with blur theme
        Button btn_create_game = createGameDialogView.findViewById(R.id.btn_createGame_create_game);
        createHumanGameDialog = new AlertDialog.Builder(mainActivity, R.style.BlurDialogTheme)
            .setView(createGameDialogView)
            .create();

        // Apply background and other window attributes
        if (createHumanGameDialog.getWindow() != null) {
            createHumanGameDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            createHumanGameDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            setBlurRadius(15);
        }

        btn_create_game.setOnClickListener(v -> {
            gameParameters.name = spinner_gameName.getSelectedItem().toString().toLowerCase();
            gameParameters.time = spinner_timemode.getSelectedItem().toString();
            gameParameters.playerColor = spinner_color.getSelectedItem().toString().toLowerCase();
            tryFirebaseOperation(() -> multiplayerDB.createGame(gameParameters.name, gameParameters.time, userName, playerStats.ELO));
        });
        createHumanGameDialog.show();
    }

    public void openCreateUserNameDialog(String takenUserName){
        AlertDialog.Builder createUserNameDialogBuilder = new AlertDialog.Builder(mainActivity, R.style.BlurDialogTheme);
        createUserNameDialogBuilder.setTitle("Create User");

        // Set up the input
        EditText input = new EditText(mainActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Type in a unique user name");
        if (!takenUserName.isEmpty()) input.setHint(takenUserName + " is already taken");
        createUserNameDialogBuilder.setView(input);

        // Set up the buttons
        createUserNameDialogBuilder.setPositiveButton("Create User", null);
        createUserNameDialogBuilder.setNegativeButton("Close", (dialog, which) -> dialog.cancel());
        userNameDialog = createUserNameDialogBuilder.create();
        userNameDialog.setOnShowListener(dialog -> {
            Button button = userNameDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                userName = input.getText().toString();
                //check if username is unique
                tryFirebaseOperation(() -> multiplayerDB.searchUsers(userName));
                //dont dismiss dialog ... yet
            });
        });

        if (userNameDialog.getWindow() != null) {
            userNameDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            userNameDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            setBlurRadius(15);
        }

        userNameDialog.show();
    }

    public void quickMatch(){
        tryFirebaseOperation(() -> multiplayerDB.searchForQuickmatch(userName, userName));
    }

    public void searchForGames(String gameName, String timeMode){
        gameParameters.name = gameName;
        gameParameters.time = timeMode;
        tryFirebaseOperation(() -> multiplayerDB.searchForOpenGames(gameName, timeMode, userName));
    }

    /** saves game parameters and player data into bundle and start chess activity */
    public void startGameWithParameters(MultiplayerDB.GameData gameData, GameParameters gameParameters) {
        launchedGamesMap.put(gameData.gameId, true);
        if (joinWaitDialog != null) joinWaitDialog.dismiss();
        if (createAIGameDialog != null) createAIGameDialog.dismiss();

        Intent intent = new Intent(mainActivity, ChessActivity.class);
        intent.putExtra(gameIdExtra, gameData.gameId);
        intent.putExtra(gamePlayerNameExtra, gameData.playerID);
        intent.putExtra(gamePlayerStatsExtra, playerStats);
        intent.putExtra(gameOpponentNameExtra, gameData.opponentID);
        intent.putExtra(gameOpponentStatsExtra, opponentStats);
        intent.putExtra(gameNameExtra, gameParameters.name);
        intent.putExtra(gameModeExtra, gameParameters.playMode);
        intent.putExtra(gameDifficultyExtra, gameParameters.difficulty);
        intent.putExtra(gameTimeExtra, gameParameters.time);
        intent.putExtra(playerColorExtra, gameParameters.playerColor);
        mainActivity.startActivity(intent);
    }

    /** try to load username from shared prefs, if not exist create one in sp and multiplayer db */
    public void loadOrCreateUserName() {
        Context context = mainActivity;
        String sharedPrefsName = "FairyChess";
        String userNameExtra = "userNameExtra";
        userName = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE).getString(userNameExtra, "");
        if (userName.isEmpty()) {
            openCreateUserNameDialog("");
        }
    }

    /** call back method after creating player in multiplayer db
     *  save name in sp and notify of success via Toast */
    public void onCreatePlayer(String playerID) {
        // save username
        Context context = mainActivity;
        String sharedPrefsName = "FairyChess";
        String userNameExtra = "userNameExtra";
        context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE).edit()
                .putString(userNameExtra, userName)
                .apply();
        userNameDialog.dismiss();
        Toast.makeText(context, "user created " + playerID, Toast.LENGTH_SHORT).show();
    }

    /** call back method after searching player name (in process of creating new player)
     *  create player if playername unique, else notify user via toast */
    public void onPlayerNameSearchComplete(String playerID, int occurrences) {
        if (occurrences == 0) {
            tryFirebaseOperation(() -> multiplayerDB.createPlayer(userName));
        } else {
            userNameDialog.setMessage("user name already taken");
            Toast.makeText(mainActivity, "user name already taken " + playerID, Toast.LENGTH_SHORT).show();
        }
    }

    /** call back method after searching for game
     *  go through games matching parameters and pick a game with chance of winning in the defined area (if possible)
     *  and then join the game; if no matching game is found notify user via Toast */
    public void onGameSearchComplete(List<GameSearchResult> gameSearchResultList) {
        if (!gameSearchResultList.isEmpty()) {
            // pre filter for fair games (winning chance is >30%)
            List<GameSearchResult> fairGames = new ArrayList<>();
            for (GameSearchResult game : gameSearchResultList) {
                if (true || ChessRatingSystem.getProbability(playerStats.ELO, game.player1ELO)
                        >= (0.5 - MultiplayerDB.matchmakingWinningChanceOffset) && ChessRatingSystem.getProbability(playerStats.ELO, game.player1ELO)
                        <= (0.5 + MultiplayerDB.matchmakingWinningChanceOffset)) {
                    fairGames.add(game);
                }
            }

            if (!fairGames.isEmpty()) {
                // chose random game
                Random random = new Random();
                GameSearchResult chosenGame = fairGames.get(random.nextInt(fairGames.size()));
                gameParameters.time = chosenGame.timeMode; // assign because in case of quickmatch this is unknown
                gameParameters.name = chosenGame.gameName;

                Log.d(TAG, "gameMode found => join gameMode");
                if (DEBUG) System.out.println("gameMode found => join gameMode");
                gameParameters.playerColor = chosenGame.player2Color;

                // set
                Map<String, Object> changeMap = new HashMap<>();
                changeMap.put(MultiplayerDB.GAMEFIELD_PLAYER2ID, userName);
                changeMap.put(MultiplayerDB.GAMEFIELD_PLAYER2ELO, playerStats.ELO);
                if (chosenGame.gameName.startsWith("all")) {
                    if (!gameParameters.name.startsWith("all")) {
                        changeMap.put(MultiplayerDB.GAMEFIELD_GAMENAME, gameParameters.name);
                    } else {
                        changeMap.put(MultiplayerDB.GAMEFIELD_GAMENAME, "normal chess"); // standard
                    }
                }
                if (chosenGame.timeMode.startsWith("all")) {
                    if (!gameParameters.time.startsWith("all")) {
                        changeMap.put(MultiplayerDB.GAMEFIELD_TIMEMODE, gameParameters.time);
                    } else {
                        changeMap.put(MultiplayerDB.GAMEFIELD_TIMEMODE, "blitz (2 minutes)"); // standard
                    }
                }

                // join game
                tryFirebaseOperation(() -> multiplayerDB.joinGame(chosenGame.id, changeMap));
            } else {
                createNoGamesFoundDialog(true);
            }
        } else {
            createNoGamesFoundDialog(false);
        }
    }

    public void createNoGamesFoundDialog(boolean withCreateOption) {
        // Create dialog with blur theme
        AlertDialog.Builder noGamesFoundDialogBuilder = new AlertDialog.Builder(mainActivity, R.style.BlurDialogTheme)
                .setTitle("no games found")
                .setNegativeButton("close", null);

        if (withCreateOption) {
            noGamesFoundDialogBuilder.setPositiveButton("create game", (dialog, which) -> 
                tryFirebaseOperation(() -> multiplayerDB.createGame(gameParameters.name, gameParameters.time, userName, playerStats.ELO)));
        }
        AlertDialog noGamesFoundDialog = noGamesFoundDialogBuilder.create();

        // Apply background and other window attributes
        if (noGamesFoundDialog.getWindow() != null) {
            noGamesFoundDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            noGamesFoundDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            setBlurRadius(15);
        }

        noGamesFoundDialog.show();
    }

    /** call back method after creating multiplayer game in db
     *  display "waiting for opponent" dialog */
    public void onCreateGame(String gameName, String gameID, String playerColor) {
        Log.d(TAG, "gameMode created");
        if (DEBUG) System.out.println("gameMode created");
        Toast.makeText(mainActivity, "created gameMode: " + gameName, Toast.LENGTH_SHORT).show();
        createdGameID = gameID;
        tryFirebaseOperation(() -> multiplayerDB.listenToGameSearch(gameID));
        gameParameters.playerColor = playerColor;

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setCancelable(false);
        builder.setView(R.layout.waiting_for_join_dialog);

        joinWaitDialog = builder.create();
        joinWaitDialog.show();
        Button btn_canceln = joinWaitDialog.findViewById(R.id.btn_cancelJoinWait);
        btn_canceln.setOnClickListener(v -> {
            if (!createdGameID.isEmpty()) {
                joinWaitDialog.dismiss();
                tryFirebaseOperation(() -> multiplayerDB.cancelGame(createdGameID));
                createdGameID = "";
            }
        });
        Button btn_createDynLink = joinWaitDialog.findViewById(R.id.btn_createDynamicLink);
        btn_createDynLink.setOnClickListener(v -> tryFirebaseOperation(() -> multiplayerDB.shareGameId(createdGameID)));
    }

    /** call back method after after game was changed,
     *  check if second player has joined */
    public void onGameChanged(String gameId) {
        if (!launchedGamesMap.containsKey(gameId)) {
            tryFirebaseOperation(() -> multiplayerDB.hasSecondPlayerJoined(gameId));
        }
    }

    public void onGetPlayerstats(MultiplayerDB.PlayerStats playerStats) {
        // Your existing logic here
        this.playerStats = playerStats;
        viewModel.updateStats(playerStats, userName);
    }

    public void processGameInvite(String gameId) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        // Create message with both deep link and manual instructions
        String deepLink = "fairychess://game/" + gameId;
        String shareMessage = "Join my FairyChess game!\n\n" +
                "Click here to join: " + deepLink + "\n\n" +
                "Or manually:\n" +
                "1. Open FairyChess app\n" +
                "2. Click \"Join Game\"\n" +
                "3. Enter Game ID: " + gameId;

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        Intent chooser = Intent.createChooser(shareIntent, "Share game invite via");
        mainActivity.startActivity(chooser);
    }

    public void handleDeepLink(Intent intent) {
        // Handle incoming deep links
        if (intent.getData() != null) {
            String scheme = intent.getData().getScheme();
            String host = intent.getData().getHost();
            if ("fairychess".equals(scheme) && "game".equals(host)) {
                String gameId = intent.getData().getLastPathSegment();
                if (gameId != null) {
                    // Join the game with the ID
                    tryFirebaseOperation(() -> multiplayerDB.joinGame(gameId, new HashMap<>()));
                }
            }
        }
    }

    /** call back method after joining game
     *  save game parameters and start game */
    public void onJoinGame(GameParameters onlineGameParameters, MultiplayerDB.GameData gameData) {
        if (!launchedGamesMap.containsKey(gameData.gameId)) {
            gameParameters.name = onlineGameParameters.name;
            gameParameters.time = onlineGameParameters.time;
            gameParameters.playerColor = onlineGameParameters.playerColor;

            opponentStats = new MultiplayerDB.PlayerStats(0, 0, 0, gameData.opponentELO);

            Log.d(TAG, userName + " joined gameMode: " + gameParameters.playMode);
            if (DEBUG) System.out.println(userName + " joined gameMode: " + gameParameters.name);
            Toast.makeText(mainActivity, userName + " joined gameMode: " + gameParameters.name, Toast.LENGTH_SHORT).show();
            startGameWithParameters(gameData, gameParameters);
            if (createHumanGameDialog != null) createHumanGameDialog.dismiss();
        }
    }

    private void tryFirebaseOperation(Runnable function) {
        try {
            function.run();
        } catch (Exception e) {
            Toast.makeText(mainActivity, "We've reached our daily game limit. Please try again tomorrow!", Toast.LENGTH_LONG).show();
        }
    }

    public void setBlurRadius(int blurBehindRadius){
        // Access window and modify attributes
        Window window = mainActivity.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            // Set other attributes such as dim amount, background transparency, etc.
            layoutParams.alpha = 0.9f;  // You can adjust opacity (this is just an example)
            window.setAttributes(layoutParams);
            
            // You can also apply other customizations like changing the background or adding effects using third-party libraries
        }
    }
}

