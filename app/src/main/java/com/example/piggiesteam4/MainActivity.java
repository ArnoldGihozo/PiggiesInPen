/**
 * AUCSC 220
 * PiggiesTeam4
 *
 * MainActivity.java
 *
 * The main, single screen where all games take place.
 *
 * On first launch, loads a 5x5 single player game.
 * Visible grids are represented by fragments, and are swapped in and out as necessary
 * into a frame layout
 */
package com.example.piggiesteam4;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.widget.ZoomButton;


import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener, GridFragment.OnFragmentInteractionListener {

    //Class Variables
    public Game singlePlayer;
    public Game multiPlayer;
    public Game currentGame;
    public Player currentPlayer;
    public Button p1Score;
    public Button p2Score;
    public int[] p1Color;
    public int[] p2Color;
    private final int GRID_SIZE_REQUEST = 1;
    private final int DEFAULT_GRID_SIZE = 5;
    private int requestedGridSize;
    public resetListener resetListener;
    GridFragment multiPlayerFragment;
    GridFragment singlePlayerFragment;

    /**
     * On creation, creates a default single player game (5x5 grid)
     * Eventually we want to be able to load from a saved state
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializations
        ImageButton menuButton = findViewById(R.id.hamMenuButton);
        NavigationView menuView = findViewById(R.id.navigationId);
        p1Score = (Button) findViewById(R.id.p1ScoreButton);
        p2Score = (Button) findViewById(R.id.p2ScoreButton);
        final ImageButton customButton = findViewById(R.id.tuggleBtn);

        SharedPreferences preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        boolean isFirstTime = preferences.getBoolean("isFirstTime", true);

        if (isFirstTime) {

            popUpMenu();

            newGame(5, false);
            newGame(5, true);

            currentGame = singlePlayer;
            setGridFragment(singlePlayerFragment);
            setScoreButtonColor();

            //"Initializes" highscores to prevent retrieving nulls
            HighScores.save(getApplicationContext());

        }//if

        else {
            retrieveGame();
        }//else

        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                changeGameModeIcon(customButton);

                swap(currentGame.isMultiplayer());
                toggleCurrentGame();
                setScoreButtonColor();

            }//onClick
        });

        //listens for drawer menu items being selected
        menuView.setNavigationItemSelectedListener(this);

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawerLayout = findViewById(R.id.drawerId);
                drawerLayout.openDrawer(GravityCompat.START);
            }//onClick
        });

        showTopScore();

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }//if

        }//if

    }//onCreate

     private void changeGameModeIcon(ImageButton customButton) {
         if (currentGame.isMultiplayer()) {
             Toast.makeText(MainActivity.this, "You are now in Single Player mode",
                     Toast.LENGTH_SHORT).show();
             customButton.setImageResource(R.drawable.singleperson);
         }else{
             Toast.makeText(MainActivity.this, "You are now in Multi Player Mode",
                     Toast.LENGTH_SHORT).show();
             customButton.setImageResource(R.drawable.twopeople);

         }// else
     }// changeGameModeIcon

     /**
     * This function will display a pop up message the very first time the game is opened.
     * Afterwards the message will stop displaying.
     * By Arnold
     */
    private void popUpMenu() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.welcome))
                .setMessage(R.string.popup_help_message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "You are now in " +
                                "Single Player mode. You are playing again'st the Computer",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .create().show();
        SharedPreferences preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isFirstTime", false);
        editor.apply();
    }// end of PopUpMenu

    /**
     * @param isMulti
     */
    public void swap(boolean isMulti) {

        FragmentTransaction fragmentTransaction;
        FragmentManager fragManager = getSupportFragmentManager();
        fragmentTransaction = fragManager.beginTransaction();

        if (isMulti) {
            fragmentTransaction.replace(R.id.fragment_container, singlePlayerFragment, "singleplayer");
            fragmentTransaction.commit();
        }//else

        else {
            fragmentTransaction.replace(R.id.fragment_container, multiPlayerFragment, "multiplayer");
            fragmentTransaction.commit();
        }//else
    }//swap

    /**
     * This function will be creating a fragment for the multiPlayer and singlePlayer
     * the very first time the game is launched.
     *
     * by Arnold
     * @param size-- the size of the game
     * @param isMultiPlayer-- whether the game is multiplayer or not.
     */
    public void newGame(int size, boolean isMultiPlayer) {

        setP1Color(getColor(R.color.red), getColor(R.color.lightRed));

         if(isMultiPlayer){
             setP2Color(getColor(R.color.blue), getColor(R.color.lightBlue));
             multiPlayer = new Game(size, true, p1Color,p2Color);
             multiPlayerFragment = createGridFragment(isMultiPlayer);
             multiPlayerFragment.setMain(this);
         }
         else{
            setP2Color(getColor(R.color.ai), getColor(R.color.aiLight));
            singlePlayer = new Game(size,false,p1Color,p2Color);
            singlePlayerFragment = createGridFragment(isMultiPlayer);
            singlePlayerFragment.setMain(this);

         }//else
     }// end of newGame

    /**
     * This basically tells the main activity that is has an options menu
     * (the navigation drawer) and inflates it so that it is visible
     *
     * By Keegan
     *
     * @param menu - Gonna be honest, I don't know
     * @return - true, as the the menu is created on creation of the activity
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nav_menu_layout, menu);
        return true;
    }//onCreateOptionsMenu

    /**
     * On selection of the an item in the drawer menu,
     * the corresponding activity is started.
     *
     * By Keegan
     *
     * @param item - the item in the menu that has been selected
     * @return - true, as the selected item's activity should be started
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Intent navIntent;

        switch (item.getItemId()) {

            case R.id.nav_leaderboard:
                navIntent = new Intent(this, LeaderboardActivity.class);
                this.startActivity(navIntent);
                break;

            case R.id.nav_grid_size:
                navIntent = new Intent(this, GridSizeActivity.class);
                navIntent.putExtra("currentSize", currentGame.getGrid().getX());
                this.startActivityForResult(navIntent, GRID_SIZE_REQUEST);
                break;

            case R.id.nav_sound:
                navIntent = new Intent(this, SoundActivity.class);
                navIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                this.startActivity(navIntent);
                break;

            case R.id.nav_help:
                navIntent = new Intent(this, HelpActivity.class);
                this.startActivity(navIntent);
                break;

            case R.id.nav_about:
                navIntent = new Intent(this, AboutActivity.class);
                this.startActivity(navIntent);
                break;

            case R.id.nav_reset:
                askForResetConfirmation();
                break;

            default:
                return super.onOptionsItemSelected(item);

        }//switch

        return true;

    }//onNavigationItemSelected

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String mode;

        if (requestCode == GRID_SIZE_REQUEST) {

            if (resultCode == RESULT_OK) {

                requestedGridSize = data.getIntExtra("size", DEFAULT_GRID_SIZE);

                if (currentGame.isMultiplayer()) {
                    mode = "multiplayer";
                    getSupportFragmentManager().beginTransaction().remove(multiPlayerFragment).commit();
                    newGame(requestedGridSize, true);
                    setGridFragment(multiPlayerFragment);
                }//if

                else {
                    mode = "single player";
                    getSupportFragmentManager().beginTransaction().remove(singlePlayerFragment).commit();
                    newGame(requestedGridSize, false);
                    setGridFragment(singlePlayerFragment);
                }//else

                //Toast for testing purposes, to show what was selected.
                Toast.makeText(getApplicationContext(),
                        "New " + mode + " game of size " + ((Integer) requestedGridSize).toString()
                                + "×" + ((Integer) requestedGridSize).toString(), Toast.LENGTH_LONG).show();

            }//if

        }//if

        DrawerLayout drawerLayout = findViewById(R.id.drawerId);
        drawerLayout.closeDrawer(GravityCompat.START);

    }//onActivity

    //Maybe change variable away from class variable.
    String receivedName;

    /**
     * Shows a popup asking for the name of the winner.
     * To be called on game end.
     * Will end the game and get the winner's score and name. May be displayed on leaderboard.
     */
    public void askForName(final Game game) {
        NameQueryFragment nameQuery = new NameQueryFragment();
        nameQuery.show(getSupportFragmentManager(), "nameQuery");

        NameQueryFragment.QueryDialogListener listener = new NameQueryFragment.QueryDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialog) {
                NameQueryFragment query = (NameQueryFragment) dialog;
                receivedName = query.getName().trim();
                if (receivedName.equals("")) {
                    receivedName = getString(R.string.anonymous);
                }
                game.endGame(receivedName);
                HighScores.save(getApplicationContext());
            }//onDialogPositiveClick

            @Override
            public void onDialogNegativeClick(DialogFragment dialog) {
                receivedName = getString(R.string.anonymous);
                game.endGame(receivedName);
            }//OnDialogNegativeClick
        };//QueryDialogListener
        nameQuery.setListener(listener);
    }//askForName

    //This method is probably not needed.
    //The variable might be better off as not a class variable.

    /**
     * Gets the name of the winner.
     *
     * @return player's name
     */
    public String getName() {
        return receivedName;
    }

    /**
     * Asks for confirmation to reset the game.
     */
    public void askForResetConfirmation() {
        ResetConfirmationFragment confirmation = new ResetConfirmationFragment();
        confirmation.show(getSupportFragmentManager(), "resetConfirmation");

        ResetConfirmationFragment.ResetConfirmationListener listener = new ResetConfirmationFragment.ResetConfirmationListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialog) {
                resetListener.reset();
                p1Score.setText("0");
                p2Score.setText("0");
                DrawerLayout drawerLayout = findViewById(R.id.drawerId);
                drawerLayout.closeDrawer(GravityCompat.START);
            }//onDialogPositiveClick

            @Override
            public void onDialogNegativeClick(DialogFragment dialog) {
                ;
            }//onDialogNegativeClick
        };//ResetConfirmationListener
        confirmation.setListener(listener);
    }//askForResetConfirmation

     public void onLossAlert(){
        OnLossFragment loss = new OnLossFragment();
        loss.show(getSupportFragmentManager(), "onLoss");
     }

    public void showTieAlert() {
        TieAlertFragment alert = new TieAlertFragment();
        alert.show(getSupportFragmentManager(), "tieAlert");
    }//showTieAlert

    /**
     * Sets player 1's color locally
     * Also takes it's color's lighter version for un-highlighting
     *
     * Stored as an array of these two colors, as they're heavily associated with each other
     *
     * By Keegan
     *
     * @param color      - the main color of the player
     * @param colorLight - a more softer color of the player's main color
     */
    public void setP1Color(int color, int colorLight) {
        p1Color = new int[]{color, colorLight};
    }//setP1Color

    /**
     * Sets player 2's color locally
     * Also takes it's color's lighter version for un-highlighting
     *
     * Stored as an array of these two colors, as they're heavily associated with each other
     *
     * By Keegan
     *
     * @param color - the main color of the player
     * @param colorLight - a more softer color of the player's main color
     */
    public void setP2Color(int color, int colorLight) {
        p2Color = new int[]{color, colorLight};
    }//setP2Color

    /**
     * Creates a fragment for a 5x5 grid, then inflates it to fragment_container with call
     * to setGridFragment()
     *
     * By Keegan
     *
     * @param isMultiplayer - if the grid being created is for a multiplayer game
     * @return - the created instance of the fragment grid
     */
    public GridFragment createGridFragment(boolean isMultiplayer) {

        // Create a new Fragment to be placed in the activity layout
        GridFragment grid = new GridFragment();
        setGridFragmentListener(grid);

        Bundle args = new Bundle();
        args.putBoolean("multiplayer", isMultiplayer);
        grid.setArguments(args);

        return grid;

    }//createGridFragment

    /**
     * Sets the 5x5 grid on the screen by placing it in fragment_container
     * NOT intended to be used to replace fragments in the container!
     *
     * By Keegan
     *
     * @param fragment - the 5x5 grid fragment to display
     */
    public void setGridFragment(GridFragment fragment) {

        // Add the fragment to the 'fragment_container' FrameLayout
        androidx.fragment.app.FragmentTransaction fragTrans =
                getSupportFragmentManager().beginTransaction();

        fragTrans.add(R.id.fragment_container, fragment).commit();

    }//setGridFragment

    /**
     * Sets the initial highlighting of the player scores to indicate
     * who's turn is first
     *
     * By Keegan
     */
    public void setScoreButtonColor() {

        if (currentGame.getCurrentPlayer() == currentGame.getPlayer1()) {

            p1Score.getBackground().setColorFilter(currentGame.getCurrentPlayer().getColor(),
                    PorterDuff.Mode.MULTIPLY);

            p2Score.getBackground().setColorFilter(currentGame.getNonCurrentPlayer().getColorLight(),
                    PorterDuff.Mode.MULTIPLY);

        }//if

        else {

            p2Score.getBackground().setColorFilter(currentGame.getCurrentPlayer().getColor(),
                    PorterDuff.Mode.MULTIPLY);

            p1Score.getBackground().setColorFilter(currentGame.getNonCurrentPlayer().getColorLight(),
                    PorterDuff.Mode.MULTIPLY);

        }//else

    }//setScoreButtonColor

    /**
     * This functions changes whether the game is in single player or multi player
     *
     * By Arnold
     */
    public void toggleCurrentGame() {

        if (currentGame.isMultiplayer())
            currentGame = singlePlayer;
        else
            currentGame = multiPlayer;

    }//toggleCurrentGame

    /**
     * Intentionally left blank
     *
     * Needs to be "implemented" for fragments to display
     * and not cause the app to throw an exception
     *
     * By Keegan
     *
     * @param uri - no idea
     */
    @Override
    public void onFragmentInteraction(Uri uri) {
        //empty
    }//onFragmentInteraction

    /**
     * Saves the active games.
     * Note: Due to how GSON saves objects, variables pointing to the same instance of an object
     * will instead be saved with their own separate instances of that object.
     */
    public void saveGame() {
        Context context = getApplicationContext();
        SharedPreferences pref;
        Gson gson = new Gson();
        pref = context.getSharedPreferences("games", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("singleplayer", gson.toJson(singlePlayer));
        editor.putString("multiplayer", gson.toJson(multiPlayer));
        editor.putBoolean("isMulti", currentGame.isMultiplayer());
        editor.putInt("gridSizeSingle", singlePlayer.getGrid().getX());
        editor.putInt("gridSizeMulti", multiPlayer.getGrid().getX());
        editor.commit();

    }

    /**
     * @return
     */
    public boolean retrieveGame() {
        try {
            Context context = getApplicationContext();
            SharedPreferences pref;
            pref = context.getSharedPreferences("games", Context.MODE_PRIVATE);
            Gson gson = new Gson();
            boolean isNull = false;

            boolean isMulti = pref.getBoolean("isMulti", true);
            String singlePlayerString = pref.getString("singleplayer", null);
            String multiPlayerString = pref.getString("multiplayer", null);
            int singleGridSize = pref.getInt("gridSizeSingle", 5);
            int multiGridSize = pref.getInt("gridSizeMulti", 5);

            newGame(singleGridSize * 11, false);
            newGame(multiGridSize * 11, true);

            if (singlePlayerString != null) {
                singlePlayer = gson.fromJson(singlePlayerString, Game.class);
                if (singlePlayer != null) {
                    singlePlayerFragment.setFragmentGame(singlePlayer);
                    singlePlayer.cyclePlayers();
                    isNull = true;
                }
            }//if

            if (multiPlayerString != null) {
                multiPlayer = gson.fromJson(multiPlayerString, Game.class);
                if (multiPlayer != null) {
                    multiPlayerFragment.setFragmentGame(multiPlayer);
                    multiPlayer.cyclePlayers();
                    isNull = true;
                }
            }//if

            if (isMulti) {
                currentGame = multiPlayer;
            }//if
            else{
                currentGame = singlePlayer;
            }//else

            requestedGridSize = pref.getInt("gridSize", DEFAULT_GRID_SIZE);

            try {
                p1Score.setText(((Integer) currentGame.getPlayer1().getScore()).toString());
                p2Score.setText(((Integer) currentGame.getPlayer2().getScore()).toString());
            }

            catch (NullPointerException e) {
                p1Score.setText("0");
                p2Score.setText("0");
            }

            Log.d("retrieving", "Adding fragments and showing");
            FragmentManager fragManager = getSupportFragmentManager();
            FragmentTransaction fragTrans = fragManager.beginTransaction();
            if (isMulti){
                fragTrans.add(R.id.fragment_container, multiPlayerFragment).commit();
                multiPlayerFragment.setScoreButtons(p1Score, p2Score);
                multiPlayerFragment.updateScoreView();
            }
            else{
                fragTrans.add(R.id.fragment_container, singlePlayerFragment).commit();
                singlePlayerFragment.setScoreButtons(p1Score, p2Score);
                singlePlayerFragment.updateScoreView();
            }
            setScoreButtonColor();
            ImageButton imageButton = (ImageButton) findViewById(R.id.tuggleBtn);
            Log.e("setImage", "run");
            if (isMulti){
                Log.e("setImage", "Multiplayer");
                imageButton.setImageResource(R.drawable.twopeople);
            }
            else{
                Log.e("setImage", "Singleplayer");
                imageButton.setImageResource(R.drawable.singleperson);
            }

            return true;
        }//try
        catch (Exception e) {
            Log.d("retrieveGameFail", "No game found, new game will be created");
            return false;
        }
    }//retrieveGame

    @Override
    protected void onPause() {
        super.onPause();
        saveGame();
        showTopScore();
    }//onPause

    GridFragment.gameStateListener listener = new GridFragment.gameStateListener() {

        /**
         * Ends the game, showing a popup asking for the winner's name.
         * @param game the game being ended.
         * @param frag the fragment.
         */
        @Override
        public void endGame(Game game, GridFragment frag) {
            Log.d("endGame", "Called");
            if (game.isGameOver()) {

                if (frag.fragmentGame.getPlayer1().getScore() == frag.fragmentGame.getPlayer2().getScore()) {
                    game.endGame();
                    showTieAlert();
                }//if
                else if (frag.fragmentGame.isMultiplayer() ||
                        (frag.fragmentGame.getPlayer1().getScore() > frag.fragmentGame.getPlayer2().getScore())){
                    askForName(game);
                }//else
                else{
                    game.endGame();
                    onLossAlert();
                }

                HighScores.save(getApplicationContext());
                saveGame();
                frag.resetFences();
                frag.setP1Current();
                frag.resetPens();
                p1Score.setText("0");
                p2Score.setText("0");
                showTopScore();
            }//if
        }//endGame

//        /**
//         * Saves the game.
//         * @param frag the fragment.
//         */
//        @Override
//        public void saveGame(GridFragment frag) {
//            Context context = getApplicationContext();
//            SharedPreferences pref;
//            Gson gson = new Gson();
//
//            if (frag.isMultiplayer){
//                pref = context.getSharedPreferences("multi", Context.MODE_PRIVATE);
//                SharedPreferences.Editor editor = pref.edit();
//                editor.putString("game", gson.toJson(multiPlayer));
//                editor.putString("fragGame", gson.toJson(frag.fragmentGame));
//                editor.commit();
//            }//if
//            else{
//                pref = context.getSharedPreferences("single", Context.MODE_PRIVATE);
//                SharedPreferences.Editor editor = pref.edit();
//                editor.putString("game", gson.toJson(singlePlayer));
//                editor.putString("fragGame", gson.toJson(frag.fragmentGame));
//                editor.commit();
//            }//else
//        }//saveGame
//
//        /**
//         * Retrieves a saved game.
//         * @param frag the fragment.
//         * @return whether a game has been retrieved.
//         */
//        @Override
//        public boolean retrieveGame(GridFragment frag) {
//            Context context = getApplicationContext();
//            SharedPreferences pref;
//            Gson gson = new Gson();
//
//            if (frag.isMultiplayer){
//                pref = context.getSharedPreferences("multi", Context.MODE_PRIVATE);
//            }//if
//            else{
//                pref = context.getSharedPreferences("single", Context.MODE_PRIVATE);
//            }//else
//
//            String game = pref.getString("game", "");
//            String fragGame = pref.getString("fragGame", "");
//
//            if (game.equals("") && fragGame.equals("")){
//                return false;
//            }//if
//
//            if (frag.isMultiplayer){
//                multiPlayer = gson.fromJson(game, Game.class);
//            }//if
//            else{
//                singlePlayer = gson.fromJson(game,Game.class);
//            }//else
//
//            frag.fragmentGame = gson.fromJson(fragGame, Game.class);
//            frag.p1ScoreButton.setText(((Integer) frag.fragmentGame.getPlayer1().getScore()).toString());
//            frag.p2ScoreButton.setText(((Integer) frag.fragmentGame.getPlayer2().getScore()).toString());
//            return true;
//        }//retrieveGame
    };//gameStateListener

    /**
     * Sets the listener in a grid fragment.
     *
     * @param grid the grid fragment.
     */
    public void setGridFragmentListener(GridFragment grid) {
        grid.setListener(listener);
    }//setGridFragmentListener

    public void showTopScore() {
        TextView bestScore = (TextView) findViewById(R.id.HighestScoreGrid);
        if (currentGame == null) {
            bestScore.setText(R.string.main_highscore_default);
            return;
        }
        HighScores.retrieveScores(getApplicationContext());
        HighScores.Score topScore = HighScores.getHighScore(currentGame.getGrid());
        List<HighScores.Score> aa = HighScores.getHighScores(0);
        if (topScore == null){
            bestScore.setText(R.string.main_highscore_default);
            return;
        }
        int score = topScore.getScore();
        bestScore.setText(getString(R.string.main_highscore) + score);

    }//showTopScore

    public interface resetListener {
        void reset();
    }//resetListener

    public void setResetListener(resetListener reset) {
        resetListener = reset;
    }//setResetListener

}//MainActivity
