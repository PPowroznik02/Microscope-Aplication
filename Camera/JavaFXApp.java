import java.awt.image.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.*;
import javafx.application.Application;
import javafx.beans.*;
import javafx.beans.value.*;
import javafx.concurrent.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javax.imageio.*;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.Group;
import javafx.scene.image.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.stage.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;

import cam.Frames;


class GifSequenceWriter {

    protected ImageWriter writer;
    protected ImageWriteParam params;
    protected IIOMetadata metadata;

    public GifSequenceWriter(ImageOutputStream out, int imageType, int delay, boolean loop) throws IOException {
        writer = ImageIO.getImageWritersBySuffix("gif").next();
        params = writer.getDefaultWriteParam();

        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params);

        configureRootMetadata(delay, loop);

        writer.setOutput(out);
        writer.prepareWriteSequence(null);
    }

    private void configureRootMetadata(int delay, boolean loop) throws IIOInvalidTreeException {
        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(delay / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
        commentsNode.setAttribute("CommentExtension", "Created by: https://memorynotfound.com");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loopContinuously = loop ? 0 : 1;
        child.setUserObject(new byte[]{ 0x1, (byte) (loopContinuously & 0xFF), (byte) ((loopContinuously >> 8) & 0xFF)});
        appExtensionsNode.appendChild(child);
        metadata.setFromTree(metaFormatName, root);
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName){
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++){
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)){
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return(node);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        writer.writeToSequence(new IIOImage(img, null, metadata), params);
    }

    public void close() throws IOException {
        writer.endWriteSequence();
    }
}

public class JavaFXApp extends Application
 {
    private static final int FRAME_WIDTH  = 640;
    private static final int FRAME_HEIGHT = 480;

    int pro=0;
    int rec=0;
    GraphicsContext gc;
    GraphicsContext gc1;
    GraphicsContext gc2;
    Canvas canvas;
    byte buffer[];
    byte buffer2[];
    byte photo[];
    List<byte[]> gif = new ArrayList<byte[]>();
    int prom = 5;
    PixelWriter pixelWriter;
    PixelFormat<ByteBuffer> pixelFormat;

    Frames frames;
    Stage stage;
    
    public static void main(String[] args) {
        launch(args);
    }

@Override
  public void start(Stage primaryStage) {

    final Screen primaryScreen = Screen.getPrimary();
    final List<Screen> allScreens = Screen.getScreens();
    Screen secondaryScreen;
    if (allScreens.size() <= 1) {
        System.out.println("Only one screen");
        secondaryScreen = primaryScreen;
    } else {
        // UPDATED:
        if (allScreens.get(0).equals(primaryScreen)) {
            secondaryScreen = allScreens.get(1);
        } else {
            secondaryScreen = allScreens.get(0);
        }
    }

    configureAndShowMenu("Primary", primaryStage, primaryScreen);

    final Stage secondaryStage = new Stage();
    configureAndShowStage("Secondary", secondaryStage, secondaryScreen);

    final Stage thirdStage = new Stage();
    configureAndShowStage("Third", thirdStage, secondaryScreen);
 }

 private void configureAndShowStage(final String name, final Stage stage, final Screen screen) {

    int result;
    frames = new Frames();
    result = frames.open_shm("/frames");

    stage.setTitle(name);

    Menu menu1 = new Menu("File");
    MenuItem menuItem1 = new MenuItem("Item 1");
    MenuItem menuItem2 = new MenuItem("Exit");

    menuItem2.setOnAction(e -> { System.out.println("Exit Selected"); exit_dialog(); });

    menu1.getItems().add(menuItem1);
    menu1.getItems().add(menuItem2);

    MenuBar menuBar = new MenuBar();
    menuBar.getMenus().add(menu1);
    VBox vBox = new VBox(menuBar);

    //Scene scene = new Scene(vBox, FRAME_WIDTH, FRAME_HEIGHT);

    Group root = new Group();
    canvas     = new Canvas(FRAME_WIDTH+10, FRAME_HEIGHT+37);


    Timeline timeline;
    if (name=="Secondary") {
        gc2 = canvas.getGraphicsContext2D();
        timeline = new Timeline(new KeyFrame(Duration.millis(130), e->disp_frame_proc()));
    }
    else {
        gc = canvas.getGraphicsContext2D();
        timeline = new Timeline(new KeyFrame(Duration.millis(130), e->disp_frame()));
    }

    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();

    root.getChildren().add(canvas);
    root.getChildren().add(vBox);
    Scene scene = new Scene(root);
    stage.setScene(scene);
    //stage.setOnCloseRequest(e -> { e.consume(); exit_dialog(); });

    Rectangle2D bounds = screen.getBounds();
    //System.out.println(bounds);
    stage.setX(bounds.getMinX() + (bounds.getWidth() - 300) / 2);
    stage.setY(bounds.getMinY() + (bounds.getHeight() - 200) / 2);
    stage.show();

}

private void configureAndShowMenu(final String name, final Stage stage, final Screen screen) {

    stage.setTitle(name);

    Menu menu1 = new Menu("File");
    MenuItem menu1Item1 = new MenuItem("Save");
    MenuItem menu1Item2 = new MenuItem("Save as...");

    menu1Item1.setOnAction(new EventHandler<ActionEvent>() {
        public void handle(ActionEvent event) {
            try {
                // retrieve image
                BufferedImage bi = frames.convert_to_BI(photo);
                File outputfile = new File("/home/student/GiN/Java/AR/projekt_mikroskop/recordings/"+LocalDateTime.now()+".png");
                ImageIO.write(bi, "png", outputfile);
            } catch (IOException e) {
                System.out.println("oj");
            }
        }
    });

          //Creating a File chooser
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save");
    fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
    //Adding action on the menu item
    menu1Item2.setOnAction(new EventHandler<ActionEvent>() {
        public void handle(ActionEvent event) {
            try {
                // retrieve image
                BufferedImage bi = frames.convert_to_BI(photo);
                File outputfile = fileChooser.showSaveDialog(stage);
                ImageIO.write(bi, "png", outputfile);
            } catch (IOException e) {
                System.out.println("oj");
            }

            //Opening a dialog box

        }
    });
    //menuItem2.setOnAction(e -> { System.out.println("Exit Selected"); exit_dialog(); });

    menu1.getItems().add(menu1Item1);
    menu1.getItems().add(menu1Item2);

    Menu menu2 = new Menu("Camera");
    MenuItem menu2Item1 = new MenuItem("Take a photo");
    MenuItem menu2Item4 = new MenuItem("Take a processed photo");
    MenuItem menu2Item2 = new MenuItem("Start recording");
    MenuItem menu2Item3 = new MenuItem("Stop recording");

    menu2Item1.setOnAction(e -> {
        pixelWriter = gc1.getPixelWriter();
        pixelFormat = PixelFormat.getByteRgbInstance();
        photo = frames.get_frame();
        pixelWriter.setPixels(5, 32, FRAME_WIDTH, FRAME_HEIGHT, pixelFormat, photo, 0, FRAME_WIDTH*3);
    });

    menu2Item4.setOnAction(e -> {
        pixelWriter = gc1.getPixelWriter();
        pixelFormat = PixelFormat.getByteRgbInstance();
        photo = buffer2;
        pixelWriter.setPixels(5, 32, FRAME_WIDTH, FRAME_HEIGHT, pixelFormat, photo, 0, FRAME_WIDTH*3);
    });

     menu2Item2.setOnAction(e -> {
        rec = 1;
    });

    menu2Item3.setOnAction(e -> {

        rec = 0;
        try
        {
            BufferedImage first = frames.convert_to_BI(gif.get(0));
            ImageOutputStream output = new FileImageOutputStream(new File("/home/student/GiN/Java/AR/projekt_mikroskop/recordings/"+LocalDateTime.now()+".gif"));
            System.out.println("aaa");
            GifSequenceWriter writer = new GifSequenceWriter(output, first.getType(), 250, true);

            for (int k=1; k<gif.size(); k++) {
                BufferedImage next = frames.convert_to_BI(gif.get(k));
                writer.writeToSequence(next);
            }
            gif.clear();
            writer.close();
            output.close();
        }
        catch (Exception ex)
        {
            System.out.println(ex);
        }
    });

    menu2.getItems().add(menu2Item1);
    menu2.getItems().add(menu2Item4);
    menu2.getItems().add(menu2Item2);
    menu2.getItems().add(menu2Item3);

    Menu menu3 = new Menu("Processing");
    MenuItem menu3Item0 = new MenuItem("Clear");
    MenuItem menu3Item1 = new MenuItem("Mirrored");
    MenuItem menu3Item2 = new MenuItem("Negative");
    MenuItem menu3Item3 = new MenuItem("Binar");
    MenuItem menu3Item4 = new MenuItem("Hypnosis");

    menu3Item0.setOnAction(e -> {
        pro = 0;
    });

    menu3Item1.setOnAction(e -> {
        pro = 3;
    });

    menu3Item2.setOnAction(e -> {
        pro = 1;
    });

    menu3Item3.setOnAction(e -> {
        pro = 2;
    });

    menu3Item4.setOnAction(e -> {
        pro = 4;
    });
    menu3.getItems().add(menu3Item0);
    menu3.getItems().add(menu3Item1);
    menu3.getItems().add(menu3Item2);
    menu3.getItems().add(menu3Item3);
    menu3.getItems().add(menu3Item4);

    Menu menu4 = new Menu("Analysis");

    Menu menu5 = new Menu("Galery");


    MenuBar menuBar = new MenuBar();
    menuBar.getMenus().add(menu1);
    menuBar.getMenus().add(menu2);
    menuBar.getMenus().add(menu3);
    menuBar.getMenus().add(menu4);
    menuBar.getMenus().add(menu5);

    VBox vBox = new VBox(menuBar);

    Group root = new Group();
    canvas     = new Canvas(FRAME_WIDTH+10, FRAME_HEIGHT+10);
    gc1         = canvas.getGraphicsContext2D();

    root.getChildren().add(canvas);
    root.getChildren().add(vBox);
    Scene scene = new Scene(root);
    stage.setScene(scene);

    Rectangle2D bounds = screen.getBounds();
    //System.out.println(bounds);
    stage.setX(bounds.getMinX() + (bounds.getWidth() - 600) / 2);
    stage.setY(bounds.getMinY() + (bounds.getHeight() - 500) / 2);
    stage.show();
}


 private void disp_frame()
    {
      pixelWriter = gc.getPixelWriter();
      pixelFormat = PixelFormat.getByteRgbInstance();
      buffer = frames.get_frame();

      pixelWriter.setPixels(5, 32, FRAME_WIDTH, FRAME_HEIGHT, pixelFormat, buffer, 0, FRAME_WIDTH*3);
    }

 private void disp_frame_proc()
    {
      pixelWriter = gc2.getPixelWriter();
      pixelFormat = PixelFormat.getByteRgbInstance();
      buffer2 = frames.get_frame();

      switch(pro) {
         case 1:    //negatyw
            for(int k=0; k<buffer2.length; k++) {
                int help = Byte.toUnsignedInt(buffer2[k]);
                help = 255 - help;
                buffer2[k] = (byte) (help & 0xff);
            }
            break;
         case 2:    //binaryzacja
            for(int k=0; k<buffer2.length; k+=3) {
                int help1 = Byte.toUnsignedInt(buffer2[k]);
                int help2 = Byte.toUnsignedInt(buffer2[k+1]);
                int help3 = Byte.toUnsignedInt(buffer2[k+2]);
                int help = (help1 + help2 + help3)/3;

                if(help > 127) {
                    help = 255;
                }
                else {
                    help = 0;
                }
                buffer2[k] = (byte) (help & 0xff);
                buffer2[k+1] = (byte) (help & 0xff);
                buffer2[k+2] = (byte) (help & 0xff);
            }
         break;
         case 3:    //lustro
            for (int k=0; k<buffer2.length; k+=FRAME_WIDTH*3) {
                int f = k + FRAME_WIDTH*3 - 1;
                for(int l=k; l<(FRAME_WIDTH*3/2)+k; l+=3){
                    byte a = buffer2[l];
                    buffer2[l] = buffer2[f-2];
                    buffer2[f-2] = a;
                    a = buffer2[l+1];
                    buffer2[l+1] = buffer2[f-1];
                    buffer2[f-1] = a;
                    a = buffer2[l+2];
                    buffer2[l+2] = buffer2[f];
                    buffer2[f] = a;
                    f = f-3;
                }
            }
         break;
         case 4:    //hipnoza
            int center_x = FRAME_WIDTH/2;
            int center_y = FRAME_HEIGHT/2;

            if(prom < 20){
                prom = prom + 1;
            }
            else {
                prom = 15;
            }

            int row = 0;
            for (int k=0; k<buffer2.length; k+=FRAME_WIDTH*3) {
                //int f = k + FRAME_WIDTH*3 - 1;
                row = row + 1;
                int col = 0;
                for(int l=k; l<(FRAME_WIDTH*3)+k; l+=3){
                    col = col + 1;
                    byte a = buffer2[l];
                    double od = Math.sqrt(Math.pow(center_x-col,2)+Math.pow(center_y-row,2));

                    if (od%prom <= prom/4){
                        int buff_r = Byte.toUnsignedInt(buffer2[l]);
                        int buff_g = Byte.toUnsignedInt(buffer2[l+1]);
                        int buff_b = Byte.toUnsignedInt(buffer2[l+2]);
                        double wwww = 60*Math.sin(2*Math.PI*od);
                        int wart = (int)wwww;

                        buff_r = buff_r + wart;
                         buff_g = buff_g + wart;
                          buff_b = buff_b + wart;

                        buffer2[l] = (byte) (buff_r & 0xff);
                        buffer2[l+1] = (byte) (buff_g & 0xff);
                        buffer2[l+2] = (byte) (buff_b & 0xff);
                    }
                    else {
                        int buff_r = Byte.toUnsignedInt(buffer2[l]);
                        int buff_g = Byte.toUnsignedInt(buffer2[l+1]);
                        int buff_b = Byte.toUnsignedInt(buffer2[l+2]);
                        double wwww = 60*Math.sin(2*Math.PI*od);
                        int wart = (int)wwww;

                        buff_r = buff_r - wart;
                         buff_g = buff_g - wart;
                          buff_b = buff_b - wart;

                        buffer2[l] = (byte) (buff_r & 0xff);
                        buffer2[l+1] = (byte) (buff_g & 0xff);
                        buffer2[l+2] = (byte) (buff_b & 0xff);
                    }
                }
            }
         break;
         default:
            break;
      }


      if(rec!=0){
        gif.add(buffer2);
      }
      pixelWriter.setPixels(5, 32, FRAME_WIDTH, FRAME_HEIGHT, pixelFormat, buffer2, 0, FRAME_WIDTH*3);
    }
 

 public void item_1()
  {
   System.out.println("item 1");
  } 
 
 public void exit_dialog()
  {
   System.out.println("exit dialog");
   Alert alert = new Alert(AlertType.CONFIRMATION,
                           "Do you really want to exit the program?.", 
 			    ButtonType.YES, ButtonType.NO);

   alert.setResizable(true);
   alert.onShownProperty().addListener(e -> { 
                                             Platform.runLater(() -> alert.setResizable(false)); 
                                            });

  Optional<ButtonType> result = alert.showAndWait();
  if (result.get() == ButtonType.YES)
   {
    Platform.exit();
   }
  }
}
