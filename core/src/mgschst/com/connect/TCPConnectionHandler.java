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
                case "enemyNewObjective" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.setNewEnemyObjective(Integer.parseInt(commandList.get(1)));
                }
                case "updateEnemyVictoryPoints" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.updateEnemyVictoryPoints(Integer.parseInt(commandList.get(1)), commandList.get(2));
                }
                case "updateEnemyObjectiveDuration" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.updateEnemyObjectiveDuration(Integer.parseInt(commandList.get(1)));
                }
                case "removeKilledAlly" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.removeKilledAlly(Integer.parseInt(commandList.get(1)));
                }
                case "removeKilledEnemy" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.removeKilledEnemy(Integer.parseInt(commandList.get(1)));
                }
                case "changeEnemyPersonStatus" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.changeEnemyPersonStatus(Integer.parseInt(commandList.get(1)), commandList.get(2));
                }
                case "changeAllyPersonStatus" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.changeAllyPersonStatus(Integer.parseInt(commandList.get(1)), commandList.get(2));
                }
                case "updateDefenders" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.updateDefenders(Integer.parseInt(commandList.get(1)), commandList.get(2),
                            Integer.parseInt(commandList.get(3)));
                }
                case "updateStatuses" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.updateStatuses(commandList.get(1));
                }
                case "updateMinedUp" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.updateMinedUp(commandList.get(1));
                }
                case "sendEndStatus" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.sendEndStatus();
                }
                case "endScreen" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.startEndScreen(commandList.get(1));
                }
                case "playSound" -> {
                    GameScreen currentScreen = (GameScreen) game.getScreen();
                    currentScreen.playSound(commandList.get(1));
                }
            }
        });
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {    }

    @Override
    public void onException(TCPConnection tcpConnection, IOException e) {    }
}
