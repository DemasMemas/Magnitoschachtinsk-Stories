package mgschst.com.dbObj;

import mgschst.com.connect.DatabaseHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SecondPlayer {
    Connection conn = new DatabaseHandler().getConnection();

    public String nickname;
    public String cardPicturePath;
    public String profilePicturePath;

    public SecondPlayer(String nick){
        PreparedStatement preparedStatement;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
            preparedStatement.setString(1, nick);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            nickname = resultSet.getString("nickname");
            cardPicturePath = resultSet.getString("card_picture_path");
            profilePicturePath = resultSet.getString("profile_picture_path");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

    }
    public static SecondPlayer getPlayerByNick(String nick){
        return new SecondPlayer(nick);
    }
}
