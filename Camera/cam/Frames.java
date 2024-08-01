package cam;

import java.awt.image.*;
import java.awt.image.BufferedImage;



public class Frames
 {
  private static final int FRAME_WIDTH  = 640;
  private static final int FRAME_HEIGHT = 480;

  static public native int   open_shm(String shm_name);
  static public native byte[] get_frame();

  int RGB_pixels[];

  public BufferedImage bi;

  public Frames()
   {
    System.loadLibrary("frames");

    RGB_pixels = new int[FRAME_WIDTH*FRAME_HEIGHT];
   }

  public BufferedImage convert_to_BI(byte buffer[])
   {
    int i, j;

    j = 0;
    for(i = 0; i < RGB_pixels.length; i++)
     {
      RGB_pixels[i] = (int) (buffer[j] << 16) + (buffer[j+1]<< 8) + buffer[j+2];
      j+=3;
     }

    bi = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);

    bi.setRGB(0, 0, FRAME_WIDTH, FRAME_HEIGHT, RGB_pixels, 0, FRAME_WIDTH);

    return bi;
   }



 }





















