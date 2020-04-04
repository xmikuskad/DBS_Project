package application;

import java.sql.*;
import java.util.ArrayList;

import data.User;
import ui.admin.FoodVBoxController;
import data.FoodItem;

//Tato classa sluzi na pripojenie k MySQL a vykonanie Queries
public class SQLConnector {
    private Connection connection = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
   
    private String username = "root";
    private String password = "infernoinferno";
    
    
    //Nacitanie drivera a pripojenie k databaze
    public void connectToDB()
    {
    	//Inicializacia ovladaca
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found");
            e.printStackTrace();
            return;
        }

        try {
        	//Localhost databaza - bude lepsia pri 1milione zaznamoch
        	connection = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/dbs_db?characterEncoding=latin1",username, password);
            System.out.println("Database Connected");
        	
             
        } catch (SQLException e) {              	
            System.out.println("Error code "+e.getErrorCode());
            
            //V pripade, ze databaza neexistuje tak ju vytvorime
            if(e.getErrorCode() == 1049) {
            	Connection baseConnection = getMysqlConnection();
            	SQLDatabaseCreator.createDatabase(baseConnection,this);
            }
            
            return;
        }
    }
    
    //Pripojenie celkovo na localhost nie na konkretnu databazu
    private Connection getMysqlConnection()
    {
    	try {
        	connection = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/?characterEncoding=latin1",username, password);        	
        	return connection;
        } catch (SQLException e) {              	
        	e.printStackTrace();
            return null;
        }
    }
    
    public Connection getConnection() {	return connection;	}

    //Skontroluje, ci sme pripojeny k databaze
    public boolean isConnectedToDB() {	return connection != null;	}

    //Prida pouzivate do databazy.
    //Vrati true ak bola akcia uspesna inak false
    public boolean addUserToDB(String name,String address, String password,String email)
    {
        if(connection == null) {return false;}

        try {
            preparedStatement = connection
                    .prepareStatement("INSERT INTO users (NAME,ADDRESS,PASSWORD,EMAIL,PRIVILEDGED) VALUES (?,?,?,?,?)");
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, address);
            preparedStatement.setString(3, MD5Hashing.getSecurePassword(password));
            preparedStatement.setString(4, email);
            preparedStatement.setBoolean(5, false);
            preparedStatement.executeUpdate();

            System.out.println("User: "+email+" passw: "+password+" added.");
        } catch (SQLException e) {
            System.out.println("Problem with adding user - code "+ e.getErrorCode());
            
            if(e.getErrorCode() == 1062){
            	System.out.println("User already exists!");
            }
            else {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }


    //Skontroluje, ci sa v DB nachadza dany pouzivatel aj s heslom ak ano vrati objekt User
    public User getUserInDB(String email, String password)
    {
        if(connection == null) {    return null;}
        try {
            preparedStatement = connection
                    .prepareStatement("SELECT * FROM users WHERE EMAIL = ?");
            preparedStatement.setString(1, email);
            resultSet = preparedStatement.executeQuery();

            System.out.println("Checking if user exists");

            //Porovnava aj email kedze select je case insensitive
            while(resultSet.next())
            {
                String recievedPass = resultSet.getString("PASSWORD");
                String recievedEmail = resultSet.getString("EMAIL");

                if(recievedPass.equals(MD5Hashing.getSecurePassword(password)) && email.equals(recievedEmail)) {
                        User temp = new User(resultSet.getInt("ID"),resultSet.getString("NAME"),resultSet.getString("ADDRESS"),resultSet.getString("EMAIL"),resultSet.getBoolean("PRIVILEDGED"));
                        return temp;
                }
            }
        } catch (SQLException e) {
            System.out.println("Problem with checking user");
            e.printStackTrace();
            return null;
        }

        return null;
    }

    //Zmaze cely obsah tabulky - iba na test
    public void deleteUserDB()
    {
        try {
            statement = connection.createStatement();

            statement.execute("DELETE from users");
            //Reset autoincrement
            statement.execute("ALTER TABLE dbs_db.users AUTO_INCREMENT = 1;");

            System.out.println("USERS TABLE DELETED");

        } catch (SQLException e) {
            System.out.println("Problem with deleting users table");
            e.printStackTrace();
        }
    }

    public ArrayList<FoodItem> getFoodListFromDB(int page){
        
        if(connection == null) {    return null;}

        int startFrom = (page - 1) * (int)FoodVBoxController.RESULTSPERPAGE;
        ArrayList<FoodItem> foodList = new ArrayList<FoodItem>();

        try {
            //zistim kolko je zaznamov v tabulke 
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM food");
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                FoodVBoxController.numberOfRecords = resultSet.getInt(1);
            }

            FoodVBoxController.totalPages = Math.ceil(FoodVBoxController.numberOfRecords / FoodVBoxController.RESULTSPERPAGE);

            //vyberiem z tabulky potrebny pocet zaznamov na stranu
            preparedStatement = connection.prepareStatement("SELECT * FROM food ORDER BY ID ASC LIMIT ? , ?");
            preparedStatement.setInt(1, startFrom);
            preparedStatement.setInt(2, (int)FoodVBoxController.RESULTSPERPAGE);
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                //TODO dorobit nacitavanie kuchara a ingrediencie;
                FoodItem temp = new FoodItem(resultSet.getInt("ID"),resultSet.getString("NAME"),resultSet.getInt("PRICE"),"Blank",null);

                foodList.add(temp);
            }

            return foodList;

        } catch (SQLException e) {
            System.out.println("Problem with loading food from database");
            e.printStackTrace();
            return null;
        }

        


    }

    //Zavrie otvorene pripojenia
    public void closeConnection() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }

            System.out.println("Connection Closed");
            
        } catch (Exception e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
        }
    }
}
