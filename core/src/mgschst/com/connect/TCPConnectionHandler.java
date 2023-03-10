package mgschst.com.connect;

import com.badlogic.gdx.Gdx;
import mgschst.com.screens.GameMatchingScreen;
import mgschst.com.screens.GameScreen;
import mgschst.com.MainMgschst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class TCPConnectionHandler implements TCPConnectionListener {
    final MainMgschst game;
    TCPConnection conn;

    public TCPConnectionHandler(final MainMgschst game) {
        this.game = game;
        conn = this.game.getPlayerConnection();
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {    }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        ArrayList<String> commandList = new ArrayList<>();
        Collections.addAll(commandList, value.split(","));
        if (conn == null) game.recreatePlayerConnection();
        Gdx.app.postRunnable(() -> {
            switch (commandList.get(0)) {
                case "writeChatMsg" -> {
                    System.out.println(commandList.get(1) + ": " + commandList.get(2));
                    if (commandList.get(2).split(":")[0].equals("Game created with ID"))
                        game.setCurrentGameID(Integer.parseInt(commandList.get(2).split(":")[1].trim()));
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.changeLabel(commandList.get(1) + ": " + commandList.get(2).replace("```", ","));
                }
                case "games" -> {
                    commandList.remove(0);
                    GameMatchingScreen currentScreen = (GameMatchingScreen) game.getScreen();
                    currentScreen.setUpGameList(commandList);
                }
                case "acceptJoin" -> {
                    game.setScreen(new GameScreen(game));
                    game.setCurrentGameID(Integer.parseInt(commandList.get(1)));
                }
                case "closeGame" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.openMenu();
                }
                case "startGame" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.startGame(commandList.get(1));
                }
                case "showFirstPlayer" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.showFirstPlayer(commandList.get(1));
                }
                case "changeTime" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.changeTimer(Integer.parseInt(commandList.get(1)));
                }
                case "takeTurn" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.takeTurn();
                }
                case "takeCard" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.takeCard(Integer.parseInt(commandList.get(1)));
                }
                case "enemyCard" -> {
                    System.out.println(commandList);
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.placeEnemyCard(commandList.get(1), commandList.get(2));
                }
                case "enemyCardFromHand" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.dropCardFromEnemyHand();
                }
                case "enemyTakeCard" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.enemyTakeCard();
                }
                case "updateResources" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.updateResources();
                }
                case "checkRoundEndStatus" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.checkRoundEndStatus();
                }
            }
        });
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {    }

    @Override
    public void onException(TCPConnection tcpConnection, IOException e) {    }
}
