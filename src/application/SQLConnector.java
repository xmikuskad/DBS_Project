package application;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;

import ui.Filter;
import ui.Paging;

import data.User;
import ui.admin.ChefsVBoxController;
import ui.admin.FoodVBoxController;
import data.Chef;
import data.FoodItem;
import data.Ingredient;

//Tato classa sluzi na pripojenie k MySQL a vykonanie Queries
public class SQLConnector {
    private Connection connection = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
   
    private String username = "root";
    private String password = "root";
    
    
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

    public void updateUserDB(User u)
    {
        if(connection == null) {    return ;}
        try {
            preparedStatement = connection.prepareStatement("UPDATE users SET NAME=?, ADDRESS=?, PRIVILEDGED=? WHERE ID=?");
            preparedStatement.setString(1, u.getName());
            preparedStatement.setString(2, u.getAddress());
            preparedStatement.setBoolean(3, u.isPriviledged());
            preparedStatement.setInt(4, u.getId());
            int updated = preparedStatement.executeUpdate();

            System.out.println("Updating user id:"+u.getId());

        } catch (SQLException e) {
            System.out.println("Problem with checking user");
            e.printStackTrace();
        }

    }

    public ArrayList<FoodItem> getFoodListFromDB(Paging f){
        
        if(connection == null) {    return null;}

        int startFrom = (f.getPage() - 1) * (int)(f.getResultsPerPage());
        ArrayList<FoodItem> list = new ArrayList<FoodItem>();

        try {
            //zistim kolko je zaznamov v tabulke 
            f.setNumberOfRecords(getRecordNumberFromDB("food"));

            //Pocet stran na ktore rozdelim zaznamy
            f.setTotalPages(Math.ceil(f.getNumberOfRecords() / f.getResultsPerPage()));

            //vyberiem z tabulky potrebny pocet zaznamov na stranu
            preparedStatement = connection.prepareStatement("SELECT f.ID, f.NAME, f.PRICE, ch.NAME, GROUP_CONCAT(i.name) FROM food f JOIN food_chef fc ON fc.FOOD_ID = f.ID JOIN chefs ch ON ch.ID = fc.CHEF_ID LEFT JOIN food_ingredients fi ON fi.FOOD_ID = f.ID LEFT JOIN ingredients i ON fi.INGREDIENTS_ID = i.ID GROUP BY fc.ID ORDER BY fc.ID ASC LIMIT ?, ?");
            preparedStatement.setInt(1, startFrom);
            preparedStatement.setInt(2, (int)(f.getResultsPerPage()));
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                float price = resultSet.getFloat(3);
                String chef = resultSet.getString(4);
                String ingredientsString = resultSet.getString(5);
                ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
                
                if(ingredientsString == null){
                    ingredients.clear();
                }
                else {
                    if(ingredientsString.contains(",")){
                        String[] splitted = ingredientsString.split(",");
                        for(String s : splitted){
                            ingredients.add(new Ingredient(s));
                        }
                    }
                    else {
                        ingredients.add(new Ingredient(ingredientsString));
                    }   
                }
                list.add(new FoodItem(id,name,price,chef,ingredients));
            }

            return list;

        } catch (SQLException e) {
            System.out.println("Problem with loading food from database");
            e.printStackTrace();
            return null;
        }

    }

    public ArrayList<FoodItem> getFoodListFromDB(Paging p, Filter f) {
		if(connection == null) {    return null;}

        int startFrom = (p.getPage() - 1) * (int)(p.getResultsPerPage());
        ArrayList<FoodItem> list = new ArrayList<FoodItem>();

        StringBuilder cmd = new StringBuilder("SELECT f.ID, f.NAME, f.PRICE, ch.NAME, GROUP_CONCAT(i.name) as igroup FROM food f JOIN food_chef fc ON fc.FOOD_ID = f.ID JOIN chefs ch ON ch.ID = fc.CHEF_ID LEFT JOIN food_ingredients fi ON fi.FOOD_ID = f.ID LEFT JOIN ingredients i ON fi.INGREDIENTS_ID = i.ID ");
        cmd.append("GROUP BY fc.ID ");
        cmd.append("HAVING f.PRICE <= " + f.getMaxPrice() + " ");

        if(!f.getShouldContain().isEmpty()){
            for(String s : f.getShouldContain()){
                cmd.append("AND igroup LIKE '%" + s + "%' ");
            }
        }

        if(!f.getShouldNotContain().isEmpty()){
            for(String s : f.getShouldNotContain()){
                cmd.append("AND igroup NOT LIKE '%" + s + "%' ");
            }
        }

        if(!f.getChefName().isEmpty()){
            cmd.append("AND ch.NAME LIKE '%" + f.getChefName() + "%' ");
        }

        if(!f.getFoodName().isEmpty()){
            cmd.append("AND f.NAME LIKE '%" + f.getFoodName() + "%' ");
        }
        

        cmd.append("ORDER BY fc.ID ASC LIMIT ");
        
        System.out.println(cmd);

        try {
            //zistim kolko je zaznamov v tabulke 
            p.setNumberOfRecords(getRecordNumberFromDB("food"));

            //Pocet stran na ktore rozdelim zaznamy
            p.setTotalPages(Math.ceil(p.getNumberOfRecords() / p.getResultsPerPage()));

            //vyberiem z tabulky potrebny pocet zaznamov na stranu
            preparedStatement = connection.prepareStatement(cmd.toString() + "?,?");
            preparedStatement.setInt(1, startFrom);
            preparedStatement.setInt(2, (int)(p.getResultsPerPage()));
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                float price = resultSet.getFloat(3);
                String chef = resultSet.getString(4);
                String ingredientsString = resultSet.getString(5);
                ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
                
                if(ingredientsString == null){
                    ingredients.clear();
                }
                else {
                    if(ingredientsString.contains(",")){
                        String[] splitted = ingredientsString.split(",");
                        for(String s : splitted){
                            ingredients.add(new Ingredient(s));
                        }
                    }
                    else {
                        ingredients.add(new Ingredient(ingredientsString));
                    }   
                }
                list.add(new FoodItem(id,name,price,chef,ingredients));
            }

            return list;

        } catch (SQLException e) {
            System.out.println("Problem with loading food from database");
            e.printStackTrace();
            return null;
        }

    }

    public ArrayList<Chef> getChefListFromDB(ChefsVBoxController f){
        
        if(connection == null) {    return null;}

        int startFrom = (f.getPage() - 1) * (int)(f.getResultsPerPage());
        ArrayList<Chef> list = new ArrayList<Chef>();

        try {
            //zistim kolko je zaznamov v tabulke
            f.setNumberOfRecords(getRecordNumberFromDB("chefs"));
            
            //Pocet stran na ktore rozdelim zaznamy
            f.setTotalPages(Math.ceil(f.getNumberOfRecords() / f.getResultsPerPage()));

            //vyberiem z tabulky potrebny pocet zaznamov na konkretnu stranu
            preparedStatement = connection.prepareStatement("SELECT * FROM chefs ORDER BY ID ASC LIMIT ? , ?");
            preparedStatement.setInt(1, startFrom);
            preparedStatement.setInt(2, (int)(f.getResultsPerPage()));
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                int id = resultSet.getInt("ID");
                String name = resultSet.getString("NAME");
                
                list.add(new Chef(id,name));
            }

            return list;

        } catch (SQLException e) {
            System.out.println("Problem with loading chefs from database");
            e.printStackTrace();
            return null;
        }

    }

    public int getRecordNumberFromDB(String table){
        if(connection == null) {return 0;}
        int count = 0;

        try {
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                count = resultSet.getInt(1);
                System.out.println(count);
            }
            return count;
        } catch (SQLException e) {
            e.printStackTrace();
            return count;
        }
    }

    public float getMaxPriceFromDB(){
        
        if(connection == null) {return 0;}
        float maxPrice = 0;
        try {
            preparedStatement = connection.prepareStatement("SELECT MAX(food.PRICE) FROM food");
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                maxPrice = resultSet.getFloat(1);
            }

            return maxPrice;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return maxPrice;
        }
    }

    public float getMinPriceFromDB(){
        
        if(connection == null) {return 0;}
        float minPrice = 0;
        try {
            preparedStatement = connection.prepareStatement("SELECT MIN(food.PRICE) FROM food");
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                minPrice = resultSet.getFloat(1);
            }

            return minPrice;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return minPrice;
        }
    }

    public ArrayList<Ingredient> getIngredientsListFromDB(){
        
        if(connection == null) {    return null;}

        ArrayList<Ingredient> list = new ArrayList<Ingredient>();

        try {

            //vyberiem z tabulky potrebny pocet zaznamov na stranu
            preparedStatement = connection.prepareStatement("SELECT DISTINCT res.ingNAME FROM (SELECT i.ID AS ingID, REGEXP_SUBSTR(i.NAME,'[A-z[[:space:]]]+') AS ingNAME FROM ingredients AS i) as res");
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                String name = resultSet.getString(1);
                
                list.add(new Ingredient(name));
            }

            return list;

        } catch (SQLException e) {
            System.out.println("Problem with loading Ingredients from database");
            e.printStackTrace();
            return null;
        }

    }

    public ArrayList<Ingredient> getIngredientListFromDB(Paging f){
        
        if(connection == null) {    return null;}

        int startFrom = (f.getPage() - 1) * (int)(f.getResultsPerPage());
        ArrayList<Ingredient> list = new ArrayList<Ingredient>();

        try {
            //zistim kolko je zaznamov v tabulke 
            f.setNumberOfRecords(getRecordNumberFromDB("ingredients"));

            //Pocet stran na ktore rozdelim zaznamy
            f.setTotalPages(Math.ceil(f.getNumberOfRecords() / f.getResultsPerPage()));

            //vyberiem z tabulky potrebny pocet zaznamov na stranu
            preparedStatement = connection.prepareStatement("SELECT * from ingredients ORDER BY ID ASC LIMIT ?, ?");
            preparedStatement.setInt(1, startFrom);
            preparedStatement.setInt(2, (int)(f.getResultsPerPage()));
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                
                list.add(new Ingredient(id,name));
            }

            return list;

        } catch (SQLException e) {
            System.out.println("Problem with loading Ingredients from database");
            e.printStackTrace();
            return null;
        }

    }

    public ArrayList<User> getUserListFromDB(Paging f){
        
        if(connection == null) {    return null;}

        int startFrom = (f.getPage() - 1) * (int)(f.getResultsPerPage());
        ArrayList<User> list = new ArrayList<User>();

        try {
            //zistim kolko je zaznamov v tabulke 
            f.setNumberOfRecords(getRecordNumberFromDB("users"));

            //Pocet stran na ktore rozdelim zaznamy
            f.setTotalPages(Math.ceil(f.getNumberOfRecords() / f.getResultsPerPage()));

            //vyberiem z tabulky potrebny pocet zaznamov na stranu
            preparedStatement = connection.prepareStatement("SELECT * from users ORDER BY ID ASC LIMIT ?, ?");
            preparedStatement.setInt(1, startFrom);
            preparedStatement.setInt(2, (int)(f.getResultsPerPage()));
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                String address = resultSet.getString(3);
                String email = resultSet.getString(5);
                boolean privileged = resultSet.getBoolean(6);
                
                list.add(new User(id,name,address,email,privileged));
            }

            return list;

        } catch (SQLException e) {
            System.out.println("Problem with loading Ingredients from database");
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
