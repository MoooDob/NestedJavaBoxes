package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

public class FXBoxExample extends Application {

	float transformFactor = 1.0f;

	int seed = 1524;
	Random randomizer;
	
	int gap = 3;
	
	boolean showFiles = true;
	boolean showRandomDirectoryBackgroundColor = false;
	boolean showBorder = true;
	boolean usePadding = true;
	boolean showFilenames = true;
	
	// Files with this extension will be shown, null or empty array => all files 
	final String[] fileExtensionFilter = {}; // {"java", "cpp", "h"} // null /*=> all*/
	
	// files with this extension will shown using their dimension (max line length x lines),
	// other files will be shown using an equal sized rounded rectangle
	// null or empty array => show all files with dimensions
	final String[] dimensionDisplayExtensionFilter = {}; // {"java"}
	
	// files with this file name will be explicitly shown using their dimension 
	// (max line length x lines)
	final String[] dimensionDisplayFilenameFilter = {}; // {"readme.md"}
	
	
	// **********************
	
	// init the tooltip
	Tooltip tooltip = new Tooltip("No Tooltip");
	

	@Override
	public void start(Stage stage) {

		// init randomizer
		randomizer = new Random(seed);

		// mother of all flow panes
		final Pane root; 
		
		// Prepare tooltip
        tooltip.setConsumeAutoHidingEvents(true);
    	tooltip.setTextAlignment(TextAlignment.LEFT);

    	

		// ask for directory

		DirectoryChooser dc = new DirectoryChooser();
		File selectedDirectory = dc.showDialog(stage);

		if (selectedDirectory == null) {
			root = null;
			System.out.println("No directory selected. Terminated.");
		} else {
			// mother of all flow panes
			root = createSubTree(1, selectedDirectory);
		}

		// ScrollPane
		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(root);
		scrollPane.setPannable(true);
		
		// Create operator
		AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator();

		if (root != null) {
			// Listen to scroll events (similarly you could listen to a button click, slider, ...)
			root.setOnScroll(new EventHandler<ScrollEvent>() {
			    @Override
			    public void handle(ScrollEvent event) {
			        double zoomFactor = 1.5;
			        if (event.getDeltaY() <= 0) {
			            // zoom out
			            zoomFactor = 1 / zoomFactor;
			        }
			        zoomOperator.zoom(root, zoomFactor, event.getSceneX(), event.getSceneY());
			    }
			});
		}
		
		// Creating a scene object
		Scene scene = new Scene(scrollPane, 700, 800);
		
		// add stylesheet to scene
		scene.getStylesheets().add("styles.css");
		
		// Setting title to the Stage
		stage.setTitle("Directory structure of " + selectedDirectory.getAbsolutePath());

		// Adding scene to the stage
		stage.setScene(scene);
		
		// Displaying the contents of the stage
		stage.show();	
		
		FileWriter csvWriter;
		try {
			csvWriter = new FileWriter("areas.csv");
			csvWriter.append(String.format("\"name\",\"total px²\",\"used px²\",\"rel\"\n"));
			calcTreeArea(csvWriter, root);
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
			System.out.println("Problem while writing to areas.csv");
		} finally {
		}
		
	}

	private Pair<Double, Double> calcTreeArea(FileWriter csvWriter, Pane root) throws IOException {
		String name = ((Label)root.getChildren().get(0)).getText();
		Pane dirPane = (VBox)root.getChildren().get(1);		
		double totalArea = 0;
		double usedArea = 0;
		for (Node node : dirPane.getChildren()) {
			if (node instanceof VBox) {
				usedArea += calcTreeArea(csvWriter, (VBox)node).getValue();
			} else {
				double area = ((Pane)node).getHeight() * ((Pane)node).getWidth(); 
				usedArea += area;				
			}
		}
		totalArea += ((Pane)root).getHeight() * ((Pane)root).getWidth();
		csvWriter.append(String.format(Locale.US, "%s,%1.0f,%1.0f,%1.2f\n", name , totalArea, usedArea, totalArea/usedArea));
		return new Pair<Double, Double>(totalArea, usedArea);
	}

	private Pane createSubTree(int level, File directory) {
		
		// Creating a Flow Pane
		VBox vBox = new VBox();
		
		double maxPaneHeight = 0;
		double totalArea = 0;
		double totalPanesHeight = 0;

		ArrayList<Pane> panes = new ArrayList<Pane>();
		
		// Add label
		Label newLabel = new Label(directory.getName());
		newLabel.setFont(Font.font("System", FontWeight.BOLD, 8.0f));
		//newLabel.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, CornerRadii.EMPTY, Insets.EMPTY)));

		//nodes.add(newLabel);		
		vBox.getChildren().add(newLabel);
		
		
		Pane dirPane;
		dirPane = new VBox();
		// Setting the spacing between the nodes
		((VBox)dirPane).setSpacing(gap);
		((VBox)dirPane).setAlignment(Pos.TOP_LEFT); 		
		vBox.getChildren().add(dirPane);
		
				
		int numSubDirs = 0;
		String[] subFilesAndDirectories = directory.list(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		    	if (fileExtensionFilter == null || fileExtensionFilter.length == 0 ) {
		    		// No Filter defined
		    		return true;
		    	} else {
			    	// check if file extension is in the list of allowed extensions
			        return Arrays.stream(fileExtensionFilter).anyMatch(FilenameUtils.getExtension(name.toLowerCase())::equals);
		    	}
		    };
		});	

		for (String fileOrDirectoryName : subFilesAndDirectories) {
			
			File fileOrDirectory = new File(directory, fileOrDirectoryName);
			
			if (fileOrDirectory.isDirectory()) {
				panes.add(createSubTree(level++, fileOrDirectory));
				numSubDirs++;
			} else {
				if (showFiles) {
					panes.add(createFilePane(fileOrDirectory));
				}
			}
		}
			
		vBox.setSpacing(0);
		
		if (usePadding) {dirPane.setPadding(new Insets(gap,gap,gap,gap));}
		
		vBox.setPadding(new Insets(1,1,1,1));

		//flowPane.autosize();
		
		// Alignments
		vBox.setAlignment(Pos.TOP_LEFT); 

		if (showRandomDirectoryBackgroundColor) {
			vBox.setStyle("-fx-background-color: rgba(" + (randomizer.nextInt(155) + 100) +
					", " + (randomizer.nextInt(155) + 100) + ", " + (randomizer.nextInt(155) + 100) + ", " +
					1 + "); -fx-background-radius: 10;");
		} else {
//			vBox.setStyle("-fx-background-color: rgba(" + 255 + ", " + 255 + ", " + 255 + ", " + 0
//					+ "); -fx-background-radius: 10; " + (showBorder ? "-fx-border-color: gray" : "")
//			);
			vBox.setStyle("-fx-background-color: rgba(" + 240 + ", " + 240 + ", " + 240 + ", " + 1
					+ "); -fx-background-radius: 10; " + (showBorder ? "-fx-border-color: gray" : "")
			);

		}

		dirPane.setStyle("-fx-background-color: rgba(" + 255 + ", " + 255 + ", " + 255 + ", " + 0.5f
				+ "); -fx-background-radius: 10; " // + (showBorder ? "-fx-border-color: blue; -fx-border-style: dotted;" : "")
		);

		
		// Retrieving the childrens list of the parent pane
		ObservableList<Node> list = dirPane.getChildren();
		
		// Adding all the nodes to the parent pane
		for (Pane pane : panes) {

			list.add(pane);
			
			double currentHeight = 
					(pane instanceof VBox ? ((VBox)pane).getPrefHeight() + 
												(showBorder ? 2 : 0) /*top + bottom border*/ + 
												(usePadding ? 2 * gap : 0) /*padding*/ 
					: (pane instanceof Pane ? ((Pane)pane).getPrefHeight() : 12 /*label*/)
			);
			maxPaneHeight = Math.max(maxPaneHeight, currentHeight);
			totalPanesHeight += currentHeight;
			totalArea += pane.getPrefHeight() * pane.getPrefWidth();
//			System.out.println((p instanceof FlowPane ? "Folder" : "File") + ": " + ((Label)p.getChildren().get(0)).getText() + 
//					" p.PrefHeight: " + p.getPrefHeight() + " p.PrefWidth: " + p.getPrefWidth() + 
//					" p.Height: " + p.getHeight() + " p.Width: " + p.getWidth() +
//					(p instanceof FlowPane ? " p.PrefWrap: " + ((FlowPane)p).getPrefWrapLength() : "") +
//					" totalHeight: " + totalPanesHeight);
		}
			
		// height of squared total area
		double areaHeight = Math.sqrt(totalArea) * transformFactor;
		double prefHeight = Math.max(areaHeight, maxPaneHeight);
		
		prefHeight = Math.min(totalPanesHeight, areaHeight);
		
//		// Setting preferred heights
//		((VBox)dirPane).setPrefHeight(
//				totalPanesHeight
//				+ (panes.size() - 1) * gap /*gaps*/
//				// + 2 /*border*/
//		); 	
		
//		vBox.setPrefWrapLength(flowPane.getPrefWrapLength() + 
//				12 /*label height*/ + 
//				(showBorder ? 2 : 0) /*border around childFlowPane*/ +
//				(usePadding ? 2 * gap : 0) /*padding*/
//				);

			
		bindTooltip(vBox, tooltip);
		bindTooltip(dirPane,tooltip);
		bindTooltip(newLabel, tooltip);

		return vBox;
	}

	private Pane createFilePane(File file) {

		// analyse file to get height and width
		int lineCtr = 0;
		int maxLineLength = 0;
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				maxLineLength = Math.max(maxLineLength, line.length());
				lineCtr++;
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		double paneHeight = 0;;
		double paneWidth = 0;;
        if (Arrays.stream(dimensionDisplayFilenameFilter).anyMatch(file.getName().toLowerCase()::equals)
        		|| dimensionDisplayExtensionFilter == null 
        		|| dimensionDisplayExtensionFilter.length == 0 
        		|| Arrays.stream(dimensionDisplayExtensionFilter).anyMatch(FilenameUtils.getExtension(file.getName().toLowerCase())::equals)
        		) {
        	// extension is in dimensionDisplayExtensionFilter
    		paneHeight = lineCtr;
    		paneWidth = maxLineLength;        	
        } else {
        	// extension is not in dimensionDisplayExtensionFilter
    		paneHeight = 12;
    		paneWidth = 50;        
        }
		
		Pane newPane = new Pane();
		newPane.setPrefSize(paneWidth, paneHeight);
		newPane.setMaxSize(paneWidth, paneHeight);
		newPane.setStyle("-fx-background-color: rgba(" + randomizer.nextInt(255) + ", " + randomizer.nextInt(255)
		+ ", " + randomizer.nextInt(255) + ", 0.5); -fx-background-radius: 10;");

		// add label

		// Label newLabel = new Label(file.getName() + "\n" + (int)paneHeight + "x" + (int)paneWidth);
		Label newLabel = new Label(file.getName() + " " + (int)paneHeight + "x" + (int)paneWidth);
		newLabel.setTextAlignment(TextAlignment.RIGHT);
		newLabel.setFont(new Font(8.0f));
		newLabel.setVisible(showFilenames);

		// centering label in pane:
		// https://stackoverflow.com/questions/36854031/how-to-center-a-label-on-a-pane-in-javafx
		//newLabel.layoutXProperty().bind(newPane.widthProperty().subtract(newLabel.widthProperty()).divide(2));
		newLabel.layoutYProperty().bind(newPane.heightProperty().subtract(newLabel.heightProperty()).divide(2));

		newPane.getChildren().add(newLabel);


		
		bindTooltip(newPane, tooltip);
		bindTooltip(newLabel, tooltip);




		return newPane;
	}

	public static void main(String args[]) {
		launch(args);
	}
	
	public static void bindTooltip(final Node node, final Tooltip tooltip){
		   node.setOnMouseMoved(new EventHandler<MouseEvent>(){
		      @Override  
		      public void handle(MouseEvent event) {
		        // +15 moves the tooltip 15 pixels below the mouse cursor;
		        // if you don't change the y coordinate of the tooltip, you
		        // will see constant screen flicker
		    	tooltip.setText(
		    			node instanceof Label ? 
		    					((Label)node).getText() :
		    						// for VBoxes who manage children
		    						(node instanceof VBox || node instanceof HBox) // is VBox or HBox
		    						&& (((Pane)node).getParent() != null) // AND has parent
		    						&& ((Pane)((Pane)node).getParent()).getChildren().get(0) instanceof Label ? // AND parent has children, the first of which is a Label  
		    						((Label)((VBox)((Pane)node).getParent()).getChildren().get(0)).getText() :
		    					// for VBoxes who manage a Label and the child VBox
		    					node instanceof VBox || node instanceof Pane ? ((Label)((Pane)node).getChildren().get(0)).getText() :		    					
		    						""		    			
		    	);		    	
		         tooltip.show(node, event.getScreenX() + 1 , event.getScreenY() - 30);
//		         System.out.println(node.getClass().getName() + " " + tooltip.getText() + " MouseMove");
		         event.consume();
		      }
		   });  
		   node.setOnMouseExited(new EventHandler<MouseEvent>(){
		      @Override
		      public void handle(MouseEvent event){
		         tooltip.hide();
//		         System.out.println(node.getClass().getName() + " " + tooltip.getText() + " MouseExit");
		         event.consume();
		      }
		   });
		}
}

