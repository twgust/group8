package controller;

import entity.Ingredient;
import entity.IngredientAmount;
import entity.Product;
import entity.Recipe;
import view.CreateRecipeView;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Controller {

    private CreateRecipeView recipeView;
    private Connector connector;
    private Product product;

    public Controller () {
        connector = new Connector();
        recipeView = new CreateRecipeView(this);
        recipeView.setVisible(true);
    }

    public void searchIngredient(){
        recipeView.getListIngModel().clear();
        ArrayList<Product> prodList = new ArrayList<>();
        try {
            String search = recipeView.getSearchRep();
            if(search == null){
                return;
            }
            String query = "SELECT * FROM FoodBankDB.dbo.Livsmedel where l_namn Like '%" + search + "%'";
            Statement st = connector.getConnection().createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                float price = rs.getFloat(3);
                String unit = rs.getString(4);
                prodList.add(new Product(id, name, price, unit));
                recipeView.getListIngModel().addElement(name);
            }
            recipeView.setProdList(prodList);
            st.close();
            rs.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public void searchIngredient(int searchID){
        recipeView.getListIngModel().clear();
        ArrayList<Product> prodList = new ArrayList<>();
        try {
            String query = "SELECT * FROM FoodBankDB.dbo.Livsmedel where l_id = " + searchID + ";";
            Statement st = connector.getConnection().createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                float price = rs.getFloat(3);
                String unit = rs.getString(4);
                prodList.add(new Product(id, name, price, unit));
                recipeView.getListIngModel().addElement(name);
            }
            recipeView.setProdList(prodList);
            st.close();
            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Adds a new recipe into the database
     */
    public void addRecipe(String recipeName, int portions, String description, ArrayList<Ingredient> ingList) {

        try {
            CallableStatement call = connector.getConnection().prepareCall("{call FoodBankDB.dbo.addRecipe(?, ?, ?, ?)}");
            call.setString(1, recipeName);
            call.setInt(2, portions);
            call.setString(3, description);
            call.registerOutParameter(4, Types.INTEGER);
            call.execute();

            int recipeID = call.getInt(4);
            insertIngredientsToRecipe(recipeID, ingList);

            System.out.println("Added " + recipeName + " to database");
            call.close();
        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    /*
    Creates a statement and does an executeUpdate on a given query.
     */
    public void executeUpdateQuery(String query){
        try{
            Statement st = connector.getConnection().createStatement();
            st.executeUpdate(query);
            st.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    /*
    Adds a new ingredient to the database.
     */
    public void addIngredient(String name, float price, String unit) {
        String queryAddIngredient = "Insert into FoodBankDB.dbo.Livsmedel(l_namn, l_pris, l_enhet) values('" + name + "'," + price + ",'" + unit + "');";
        System.out.println(queryAddIngredient); //For tracing process
        executeUpdateQuery(queryAddIngredient);
    }

    /*
    Edits an ingredient by overwriting all of its data.
     */
    public void editIngredient(int id, String name, float price, String unit){
        String query = "UPDATE FoodBankDB.dbo.Livsmedel SET l_namn = '" + name + "' , l_pris = " + price + " , l_enhet = '" + unit + "' WHERE l_id = " + id + ";";
        executeUpdateQuery(query);
    }

    /*
    Deletes the connection between a recipe and an ingredient in the database.
     */
    public void deleteIngredientFromRecipe(int ingredientID, int recipeID){
        String query = "DELETE FROM FoodBankDB.dbo.ReceptIngredienser " +
                "WHERE l_id = " + ingredientID + " AND r_id = " + recipeID + ";";
        executeUpdateQuery(query);
    }

    /*
    Edits an ingredient in a recipe.
     */
    public void editIngredientInRecipe(int ingredientID, int recipeID, float amount){
        String query = "UPDATE FoodBankDB.dbo.ReceptIngredienser" +
                " SET mängd = " + amount +
                " WHERE l_id = " + ingredientID + " AND r_id = " + recipeID + ";";
        executeUpdateQuery(query);
    }

    /*
    Retrieves all recipes from the database and returns them in an ArrayList
     */
    public ArrayList<Recipe> getAllRecipes(){
        return searchRecipe("");
    }

    /*
    Searches for a recipe based on a search term. Returns a list of relevant recipes.
     */
    public ArrayList<Recipe> searchRecipe(String searchWord){
        String query = "SELECT * FROM FoodBankDB.dbo.Recept WHERE r_namn Like '%" + searchWord + "%'";
        ArrayList<Recipe> recList = new ArrayList<>();
        try {
            Statement st = connector.getConnection().createStatement();
            ResultSet res = st.executeQuery(query);

            //Iterates through resultset, creating a new Recipe object of every row and adding it to recList
            while(res.next()){
                int recipeID = res.getInt(1);
                String name = res.getString(2);
                int portions = res.getInt(3);
                String description = res.getString(4);
                recList.add(new Recipe(recipeID, name, portions, description));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return recList;
    }

    /*
    Returns details of a recipe not found in Recept table. Should be Ingredient names, amounts, prices and units.
    The data returned is stored as a HashMap of Product as key, IngredientAmount as value.
     */
    public HashMap<Product, IngredientAmount> getRecipeIngredients(int recipeID){
        String query = "SELECT * FROM FoodBankDB.dbo.ReceptIngredienser WHERE r_id = " + recipeID;
        HashMap<Product, IngredientAmount> map = new HashMap<>();
        try {

            //Gets all ingredients linked to recipe
            Statement st = connector.getConnection().createStatement();
            ResultSet res = st.executeQuery(query);
            ArrayList<IngredientAmount> iaList = new ArrayList<>();

            //Creates an IngredientAmount object from every row in resultset, saves in iaList
            while(res.next()){
                int ingredientID = res.getInt(1);
                float amount = res.getFloat(3);
                iaList.add(new IngredientAmount(ingredientID, amount));
            }

            //Gets every ingredient from DB table Livsmedel by the ingredientID
            for (int i = 0; i < iaList.size(); i++) {
                String newQuery = "SELECT * FROM FoodBankDB.dbo.Livsmedel WHERE l_id = " + iaList.get(i).getIngredientID();
                ResultSet resultSet = st.executeQuery(newQuery);
                resultSet.next();
                String prodName = resultSet.getString(2);
                float price = resultSet.getFloat(3);
                String unit = resultSet.getString(4);

                //Creates a Product from the resultset, puts both Product linked IngredientAmount in map
                Product product = new Product(prodName, price, unit);
                map.put(product, iaList.get(i));

            }
            st.close();
            res.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
        return map;
    }

    /*
    Deletes recipe from database with recipeID.
     */
    public void deleteRecipe(int recipeID){
        try {
            PreparedStatement ptsmt = connector.getConnection().prepareCall("{call FoodBankDB.dbo.deleteRecipe(?)}");
            ptsmt.setInt(1, recipeID);
            ptsmt.execute();
            ptsmt.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    /*
    Edits a recipe in the database by overwriting its data.
     */
    public void editRecipe(int recipeID, String name, int portions, String description, ArrayList<Ingredient> ingList){
        String updateQuery =
                "UPDATE FoodBankDB.dbo.Recept " +
                "SET r_namn = '" + name + "', r_portioner = " + portions + ", r_beskrivning = '" + description +
                "' WHERE r_id = " + recipeID + ";";
        executeUpdateQuery(updateQuery);

        insertIngredientsToRecipe(recipeID, ingList);
    }

    /*
    Inserts a list of ingredients in junction to a recipe into the database.
     */
    private void insertIngredientsToRecipe(int recipeID, ArrayList<Ingredient> ingList) {
        try {
            Statement st = connector.getConnection().createStatement();
            for (int i = 0; i < ingList.size(); i++) {
                int ingredientID = ingList.get(i).getIngredientAmount().getIngredientID();
                float amount = ingList.get(i).getIngredientAmount().getAmount();
                String query = "INSERT INTO FoodBankDB.dbo.ReceptIngredienser(l_id, r_id, mängd) VALUES" + "(" + ingredientID + ", " + recipeID + ", " + amount + ")";
                st.executeUpdate(query);
            }
            st.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /*
    Get product information from DB and returns object. Use name as search variable
     */
    public Product getProductInfo(String productName){
        int id;
        String name;
        float price;
        String unit;
        try {
            Statement statement = connector.getConnection().createStatement();
            String query = "SELECT * FROM FoodBankDB.dbo.Livsmedel where l_namn ='"+productName +"'";
            ResultSet result = statement.executeQuery(query);
            while (result.next()){
                 id = result.getInt(1);
                name = result.getString(2);
                price = result.getFloat(3);
                unit = result.getString(4);
                product = new Product(id,name,price,unit);
            }
            statement.close();
            result.close();
        }catch (SQLException e){
            e.printStackTrace();
        }

        return product;

    }
    public int getProductID(){
        return product.getId();
    }
}
