
package br.com.filter.app;

import br.com.filter.util.MaskFilter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application{

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = buildView();
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private Parent buildView(){
        
         TextField tfCpf = new TextField();
        tfCpf.setTextFormatter(new TextFormatter(new MaskFilter("###.###.###-##")));
        
        TextField tfCep = new TextField();
        tfCep.setTextFormatter(new TextFormatter(new MaskFilter("#####-###", '_')));
        
        TextField tfNumber = new TextField();
        tfNumber.setTextFormatter(new TextFormatter(new MaskFilter("(##)#-####-####")));
        
        GridPane gp = new GridPane();
        
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        
        gp.setPadding(new Insets(40));
        gp.setVgap(4);
        
        gp.add(new Label("CPF"), 0, 0); gp.add(tfCpf, 1, 0);
        
        gp.add(new Label("CEP"), 0, 1); gp.add(tfCep, 1, 1);
        
        gp.add(new Label("Número"), 0, 2); gp.add(tfNumber, 1, 2);
        
        final ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        
        gp.getColumnConstraints().addAll(col, col);
       
        vBox.getChildren().add(gp);
        
        VBox.setVgrow(vBox, Priority.ALWAYS);
        
        return vBox;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
}
