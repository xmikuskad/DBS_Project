package ui.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import data.FoodItem;
import data.Ingredients;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ui.FoodFilterController;

public class FoodVBoxController {

	@FXML Button foodFilterBtn,foodNextBtn,foodPreviousBtn;	
	@FXML TableView<FoodItem> foodTableView;
	
	public void initialize()
	{
		System.out.println("initialize() foodVBoxController");
		
		foodFilterBtn.setOnAction(e->{
			openFilterMenu();
		});	
		
		//Pridat funkcie addFood, next a previous - aby sme nezobrazili 1milion zaznamom naraz
		foodNextBtn.setOnAction(e->{
			System.out.println("Next btn pressed");
		});
		foodPreviousBtn.setOnAction(e->{
			System.out.println("Previous btn pressed");
		});
		
		//Vytvorenie moznosti pri kliku praveho tlacidla
		MenuItem deleteMenu = new MenuItem("Delete");
		MenuItem editMenu = new MenuItem("Edit");
		ContextMenu menu = new ContextMenu();
		menu.getItems().addAll(editMenu,deleteMenu);
		
		//Set up table
		foodTableView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("name"));
		foodTableView.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("price"));
		foodTableView.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("chef"));
		
		foodTableView.setRowFactory(e -> {
			TableRow<FoodItem> tRow = new TableRow<>();
		    tRow.setOnMouseClicked(event -> {
		    	
		    	if(tRow.isEmpty())
		    		return;	    	
		    	FoodItem food= tRow.getItem();
	            
	            //Vybratie moznosti delete
				deleteMenu.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						System.out.println("Deleting! "+food.getName());
						showConfirmBox(food);
					}
				});
				//Vyboratie moznosti edit
				editMenu.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						System.out.println("Editing! "+food.getName());	
						openEditMenu(food);
					}
				});
		    	
		        tRow.setContextMenu(menu);
		    	
		        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !tRow.isEmpty() ) {
		            System.out.println("ID OF CLICKED food "+food.getId());		     
		            openDetailMenu(food);
		        }
		    });
		    return tRow ;
		});	
		
		//TODO pridat nacitanie jedla z DB
		
		//Debug
		for(int i=1;i<1000;i++)
		{
			foodTableView.getItems().add(new FoodItem(i, "Gulas"+i, 25, "Jozko Mrkvicka",null));
		}

	}
	
	public void changeFoodFilter(){
		//TODO Tu by mal byt kod na zmenu filtrovania jedla
	}
	
	//Otvori okno s filtrom
	public void openFilterMenu()
	{
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/FoodFilter.fxml"));
			VBox root = loader.load();
			Scene scene = new Scene(root);
			Stage primaryStage = new Stage();
			primaryStage.setScene(scene);
			primaryStage.setResizable(false);
			primaryStage.setTitle("Filter");
			//Nastavuje prioritu. Neda sa vratit naspat dokial nezavru toto okno
			primaryStage.initModality(Modality.APPLICATION_MODAL); 
			primaryStage.show();
			
			//Nastavenie referencie aby sa dal uplatnit filter
			FoodFilterController filterScreenController = (FoodFilterController)loader.getController();
			filterScreenController.setAdminFoodFilter(this);	
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}	

	private void openEditMenu(FoodItem food)
	{
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/admin/edit/FoodEditPopup.fxml"));	
			FoodEditController con = new FoodEditController();
			loader.setController(con);
			VBox root = (VBox) loader.load();
			Scene scene = new Scene(root);		
			Stage stage = new Stage();
		
			stage.setScene(scene);
			stage.setResizable(false);
			stage.setTitle("Editing "+food.getName());
		
			//Nastavuje prioritu. Neda sa vratit naspat dokial nezavru toto okno
			stage.initModality(Modality.APPLICATION_MODAL); 			
			stage.show();	
		
			con.setFood(food, stage,foodTableView);			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Vytvara upozornenie pri mazani
	private void showConfirmBox(FoodItem item)
	{
		Platform.runLater(() -> {
            ButtonType okay = new ButtonType("Delete");
            ButtonType cancel = new ButtonType("Cancel");
            Alert alert = new Alert(Alert.AlertType.WARNING,"",okay,cancel);
            alert.setTitle("Continue?");
            alert.setHeaderText("Do you really want to delete that record?");
            Optional<ButtonType> result = alert.showAndWait();
            
            if(result.orElse(cancel) == okay)
            {
            	System.out.println("Pressed delete");
            	foodTableView.getItems().remove(item);
            	//TODO vymazat zaznam food
            }
        }
		);
	}
	
	//Vytvori nove okno s detailom o polozke
	private String DETAIL_TITLE = "Detail o polozke";
	private void openDetailMenu(FoodItem food)
	{
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/ItemDetail.fxml"));	
			VBox root = (VBox) loader.load();
			Scene scene = new Scene(root);		
			Stage stage = new Stage();
			
			stage.setScene(scene);
			stage.setResizable(false);
			stage.setTitle(DETAIL_TITLE);
			
			Label detailLabel;
			detailLabel = (Label)root.getChildren().get(0);
			
			String textString = "Name of the food: "+food.getName()+"\nPrice: "+String.valueOf(food.getPrice())+
					"\nChef: "+food.getChef()+"\nIngredients:\n";
			
			if(food.getIngredients()!= null)
			{
				for (Ingredients ing: food.getIngredients()) {
					textString+=ing.getName()+"\n";
				}
			}
			
			detailLabel.setText(textString);
						
			//Nastavuje prioritu. Neda sa vratit naspat dokial nezavru toto okno
			stage.initModality(Modality.APPLICATION_MODAL); 			
			stage.show();			
					
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

//Toto je controller pre edit menu
class FoodEditController
{
	@FXML TextField nameField,priceField,chefField;
	@FXML Button saveBtn;
	@FXML MenuButton containBox;

	public void setFood(FoodItem food, Stage stage, TableView<FoodItem> table)
	{
		nameField.setText(food.getName());
		priceField.setText(String.valueOf(food.getPrice()));
		chefField.setText(food.getChef());
	
		containBox.setText("");
		containBox.getItems().clear();
		//Vlozenie obsahujucich ingredienci
		if(food.getIngredients() != null) {
			for (Ingredients ing : food.getIngredients()) {
				String nameString = ing.getName();
				CheckBox cb0 = new CheckBox(nameString);  
				cb0.setUserData(ing);
				CustomMenuItem item0 = new CustomMenuItem(cb0); 
				item0.setHideOnClick(false);  
				containBox.getItems().add(item0);
				cb0.setSelected(true);
			}
		}
	
		//Debug
		List<Ingredients> dIngredients = new ArrayList<>();
		dIngredients.add(new Ingredients(0, "oregano"));
		dIngredients.add(new Ingredients(0, "paprika"));
		dIngredients.add(new Ingredients(0, "kecup"));
		dIngredients.add(new Ingredients(0, "bazalka"));
		dIngredients.add(new Ingredients(0, "horcica"));
		
		for (Ingredients ing : dIngredients) {
			CheckBox cb0 = new CheckBox(ing.getName());  
			cb0.setUserData(ing);
			CustomMenuItem item0 = new CustomMenuItem(cb0); 
			item0.setHideOnClick(false);  
			containBox.getItems().add(item0);
		}
		
		//Vlozenie ostatnych ingrediencii
		//TODO vlozenie ostatnych ingrediencii z DB
		/*for (Ingredients ing : DB ) {
			String nameString = ing.getName();
			CheckBox cb0 = new CheckBox(nameString);  
			CustomMenuItem item0 = new CustomMenuItem(cb0); 
			item0.setHideOnClick(false);  
			containBox.getItems().add(item0);
			cb0.setSelected(true);
		}*/
		
		
		saveBtn.setOnAction(e-> {
			System.out.println("Changes saved!");
			food.setName(nameField.getText());
			
			try {
				food.setPrice(Integer.valueOf(priceField.getText()));
			}catch (Exception e2) {
				System.out.println("Wrong nubmer formatting!");
			}
			food.setChef(chefField.getText());
			
			List<Ingredients> newList = new ArrayList<>();			
			for (MenuItem custom : containBox.getItems()) {
				CheckBox item = (CheckBox)((CustomMenuItem)custom).getContent();
				if(item.isSelected()) {
					newList.add((Ingredients)item.getUserData());
				}
			}		
			food.setIngredients(newList);
			table.refresh();
			
			//TODO ulozit zmeny food
			stage.close();
		});
	}
	
	
}
