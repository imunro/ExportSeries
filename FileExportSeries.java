/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2014 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.io.IOException;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageWriter;
import loci.formats.meta.IMetadata;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;
import loci.formats.MetadataTools;

import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;

/**
 * Example class that shows how to export raw pixel data to OME-TIFF using
 * Bio-Formats version 4.2 or later.
 */
public class FileExportSeries {
  
  private int sizeT = 3;
  private int rows = 2;
  private int cols = 2;

  /** The file writer. */
  private ImageWriter writer;

  /** The name of the output file. */
  private String outputFile;

  /**
   * Construct a new FileExport that will save to the specified file.
   *
   * @param outputFile the file to which we will export
   */
  public FileExportSeries(String outputFile) {
    this.outputFile = outputFile;
  }

  /** Save a single 2x2 uint16 plane of data. */
  public void export() {
    int width = 2, height = 2;
    int pixelType = FormatTools.UINT16;
    Exception exception = null;

    IMetadata omexml = initializeMetadata(width, height, pixelType);
    int series = 0;
    int index = 0;
    int nSeries = rows * cols;
    
    // only save a plane if the file writer was initialized successfully
    boolean initializationSuccess = initializeWriter(omexml);
    if (initializationSuccess) {
    
    while (series < nSeries) {
        index = 0;
        for (int p = 0; p < sizeT; p++) {
          System.out.println(" about to save a plane");
          savePlane(width, height, pixelType, index);
          index++;
        }
        series++;
          try {
            writer.setSeries(series);
          } catch (FormatException e) {
            exception = e;
          } 
      }  //endwhile
    }   //endif
    cleanup();
  }

  /**
   * Set up the file writer.
   *
   * @param omexml the IMetadata object that is to be associated with the writer
   * @return true if the file writer was successfully initialized; false if an
   *   error occurred
   */
  private boolean initializeWriter(IMetadata omexml) {
    // create the file writer and associate the OME-XML metadata with it
    writer = new ImageWriter();
    writer.setMetadataRetrieve(omexml);

    Exception exception = null;
    try {
      writer.setId(outputFile);
    }
    catch (FormatException e) {
      exception = e;
    }
    catch (IOException e) {
      exception = e;
    }
    if (exception != null) {
      System.err.println("Failed to initialize file writer.");
      exception.printStackTrace();
    }
    return exception == null;
  }

  /**
   * Populate the minimum amount of metadata required to export an image.
   *
   * @param width the width (in pixels) of the image
   * @param height the height (in pixels) of the image
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
  private IMetadata initializeMetadata(int width, int height, int pixelType) {
    Exception exception = null;
    try {
      // create the OME-XML metadata storage object
      ServiceFactory factory = new ServiceFactory();
      OMEXMLService service = factory.getInstance(OMEXMLService.class);
      OMEXMLMetadata meta = service.createOMEXMLMetadata();
      //IMetadata meta = service.createOMEXMLMetadata();
      meta.createRoot();
      
      String ImageStr = "Image:0-";
      String suffixStr;
      int series = 0;
      
      
      for (int row = 0; row  < rows; row++) {
        for (int column = 0; column < cols; column++) {
         
          
          suffixStr = Integer.toString(series);

          String imageID = MetadataTools.createLSID("Image:" + suffixStr, series);
          meta.setImageID(imageID, series);
          meta.setImageName("Image: " + suffixStr, series);
          meta.setPixelsID("Pixels:0:"+suffixStr, series);

          // specify that the pixel data is stored in big-endian format
          // change 'TRUE' to 'FALSE' to specify little-endian format
          meta.setPixelsBinDataBigEndian(Boolean.TRUE,  series, 0);

          // specify that the images are stored in ZCT order
          meta.setPixelsDimensionOrder(DimensionOrder.XYZCT, series);

          // specify that the pixel type of the images
          meta.setPixelsType(PixelType.fromString(FormatTools.getPixelTypeString(pixelType)), series);

          // specify the dimensions of the images
          meta.setPixelsSizeX(new PositiveInteger(width), series);
          meta.setPixelsSizeY(new PositiveInteger(height), series);
          meta.setPixelsSizeZ(new PositiveInteger(1), series);
          meta.setPixelsSizeC(new PositiveInteger(1), series);
          meta.setPixelsSizeT(new PositiveInteger(sizeT), series);

          // define each channel and specify the number of samples in the channel
          // the number of samples is 3 for RGB images and 1 otherwise
          meta.setChannelID("Channel:0:0", series,0 );
          meta.setChannelSamplesPerPixel(new PositiveInteger(1), series, 0);

           // add FLIM annotation 
          //CoreMetadata modlo = createModuloAnn(meta);
          //service.addModuloAlong(meta, modlo, 0);
          
          series++;
      
        }
      }
  

      return meta;
      }
    
    
    catch (ServiceException e) {
      exception = e;
    }
    catch (EnumerationException e) {
      exception = e;
    }
    catch (DependencyException e) {
      exception = e;
    }
    

    System.err.println("Failed to populate OME-XML metadata object.");
    exception.printStackTrace();
    return null;
      
  }
  
  
   /**
   * Add ModuloAlong annotation.
   */
  private CoreMetadata createModuloAnn(IMetadata meta) {

    CoreMetadata modlo = new CoreMetadata();

    modlo.moduloT.type = loci.formats.FormatTools.LIFETIME;
    modlo.moduloT.unit = "ps";
    modlo.moduloT.typeDescription = "Gated";

    modlo.moduloT.labels = new String[sizeT];

    for (int i = 0; i < sizeT; i++) {
      modlo.moduloT.labels[i] = Integer.toString(i * 1000);
    }

    return modlo;

  }

  /**
   * Generate a  plane of pixel data and save it to the output file.
   *
   * @param width the width of the image in pixels
   * @param height the height of the image in pixels
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
  private void savePlane(int width, int height, int pixelType, int index) {
    byte[] plane = createImage(width, height, pixelType, index);
    Exception exception = null;
    try {
      writer.saveBytes(index, plane);
    }
    catch (FormatException e) {
      exception = e;
    }
    catch (IOException e) {
      exception = e;
    }
    if (exception != null) {
      System.err.println("Failed to save plane.");
      exception.printStackTrace();
    }
  }

  /**
   * Generate a plane of pixel data.
   *
   * @param width the width of the image in pixels
   * @param height the height of the image in pixels
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
  private byte[] createImage(int width, int height, int pixelType, int index) {
    // create a blank image of the specified size
    byte[] img =
      new byte[width * height * FormatTools.getBytesPerPixel(pixelType)];

    // fill it with random data
    for (int i=1; i<img.length; i+=2) {
      img[i] = (byte)(10 * (sizeT - index));
    }
    return img;
  }

  /** Close the file writer. */
  private void cleanup() {
    try {
      writer.close();
    }
    catch (IOException e) {
      System.err.println("Failed to close file writer.");
      e.printStackTrace();
    }
  }

  /**
   * To export a file to OME-TIFF:
   *
   * $ java FileExport output-file.ome.tiff
   */
  public static void main(String[] args) throws Exception {
    FileExportSeries exporter = new FileExportSeries("SeriesFromJava.ome.tiff");
    exporter.export();
  }

}
