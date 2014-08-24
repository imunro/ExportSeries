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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

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
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.enums.NamingConvention;

/**
 * Example class that shows how to export raw pixel data to OME-TIFF as a Plate using
 * Bio-Formats version 4.2 or later.
 */
public class FileExportSPW {
  
  private int pixelType = FormatTools.UINT16;
  private int rows;
  private int cols;
  private int width;
  private int height;
  private int sizet;
  boolean initializationSuccess = false;
  

  /** The file writer. */
  private ImageWriter writer;

  /** The name of the current output file. */
  private String outputFile;
  
  /** The name of the current output file. */
  private String outputBase;
  

  /**
   * Construct a new FileExport that will save to the specified file.
   *
   * @param outputFile the file to which we will export
   */
  public FileExportSPW(String outputFile) {
    this.outputFile = outputFile;
    
            
  }
  
  public boolean init( int[][] nFov, int sizeX, int  sizeY, int sizet)  {
    this.rows = nFov.length;
    this.cols = nFov[0].length;
    width = sizeX;
    this.height = sizeY;
    this.sizet = sizet;
    IMetadata omexml = initializeMetadata(nFov);
     
    Path path = FileSystems.getDefault().getPath(outputFile);
      //delete if exists 
    //NB deleting old files seems to be critical when changing size
    try {
      boolean success = Files.deleteIfExists(path);
      System.out.println("Delete status: " + success);
    } catch (IOException | SecurityException e) {
      System.err.println(e);
    }
    
    initializationSuccess = initializeWriter(omexml);
    
    return initializationSuccess;
    
  }

  /** Save a single 2x2 uint16 plane of data. */
  public void export(byte[] plane, int series, int index) {
    
    Exception exception = null;
    

    if (initializationSuccess) {
      if (series != writer.getSeries())  {
        try {
          writer.setSeries(series);
        } catch (FormatException e) {
          exception = e;
        }
      }
      savePlane( plane, index);
    }   //endif 
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
   * Populate the minimum amount of metadata required to export a Plate.
   *
   * @param width the width (in pixels) of the image
   * @param height the height (in pixels) of the image
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
  private IMetadata initializeMetadata(int[][] nFovs) {
    Exception exception = null;
    try {
      // create the OME-XML metadata storage object
      ServiceFactory factory = new ServiceFactory();
      OMEXMLService service = factory.getInstance(OMEXMLService.class);
      OMEXMLMetadata meta = service.createOMEXMLMetadata();
      //IMetadata meta = service.createOMEXMLMetadata();
      meta.createRoot();
      
      String suffixStr;
      int plateIndex = 0;
      int series = 0;
      int well = 0;
      int nFov;
      String wellSampleID;
      
      // Create Minimal 2x2 Plate 
      meta.setPlateID(MetadataTools.createLSID("Plate", 0), 0);
   
      meta.setPlateRowNamingConvention(NamingConvention.LETTER, 0);
      meta.setPlateColumnNamingConvention(NamingConvention.NUMBER, 0);

      meta.setPlateRows(new PositiveInteger(rows), 0);
      meta.setPlateColumns(new PositiveInteger(cols), 0);
      meta.setPlateName("First test Plate", 0);
      PositiveInteger pwidth = new PositiveInteger(width);
      PositiveInteger pheight = new PositiveInteger(height);
      
      for (int row = 0; row  < rows; row++) {
        for (int column = 0; column < cols; column++) {
         
          suffixStr = Integer.toString(series);
          // Create Image
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
          meta.setPixelsSizeX(pwidth, series);
          meta.setPixelsSizeY(pheight, series);
          meta.setPixelsSizeZ(new PositiveInteger(1), series);
          meta.setPixelsSizeC(new PositiveInteger(1), series);
          meta.setPixelsSizeT(new PositiveInteger(sizet), series);

          // define each channel and specify the number of samples in the channel
          // the number of samples is 3 for RGB images and 1 otherwise
          meta.setChannelID("Channel:0:"+suffixStr, series,0 );
          meta.setChannelSamplesPerPixel(new PositiveInteger(1), series, 0);
          
          nFov = nFovs[row][column];
          
          if (nFov > 0)  {
            // set up wells
            String wellID = MetadataTools.createLSID("Well:" + suffixStr, 0, well);
            meta.setWellID(wellID, plateIndex, well);
            meta.setWellRow(new NonNegativeInteger(row), plateIndex, well);
            meta.setWellColumn(new NonNegativeInteger(column), plateIndex, well);

            for (int sampleIndex = 0; sampleIndex < nFov; sampleIndex++) {
              wellSampleID = MetadataTools.createLSID("WellSample:" + Integer.toString(sampleIndex), 0, series, sampleIndex);
              meta.setWellSampleID(wellSampleID, 0, well, sampleIndex);
              meta.setWellSampleIndex(new NonNegativeInteger(series), 0, series, sampleIndex);
              meta.setWellSampleImageRef(imageID, 0, series, sampleIndex);
            }
          }

          // add FLIM ModuloAlongT annotation if required 
          CoreMetadata modlo = createModuloAnn(meta);
          service.addModuloAlong(meta, modlo, series);
           
          series++;
          well++;
      
        }
      }
      
      //String dump = meta.dumpXML();
      //System.out.println("dump = ");
      //System.out.println(dump);
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

    modlo.moduloT.labels = new String[sizet];

    for (int i = 0; i < sizet; i++) {
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
  private void savePlane(byte[] plane, int index) {
    
    
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
  private byte[] createImage(int width, int height, int index, int series) {
    // create a blank image of the specified size
    int bpp = FormatTools.getBytesPerPixel(pixelType);
    byte[] img = new byte[width * height * bpp];
    
    ByteBuffer bb = ByteBuffer.wrap(img);
    bb.order(ByteOrder.BIG_ENDIAN);
    
    // fill it with background 
    for (int i = 0; i < img.length; i += bpp) {
      bb.putShort(i, (short) 200);
    }

    //then set 1 pixel to non-zero. Different values in each plane
    switch (index) {
      case 0: bb.putShort(series * bpp, (short) 1000);
              break;
      case 1: bb.putShort(series * bpp, (short) 700);
              break;
      case 2: bb.putShort(series * bpp, (short) 300);
              break;
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
    String fileName  = "SPWFromJava.ome.tiff";
    int nRows = 2;
    int nCols = 2;
    // 2d  Array *nRows x nCols giving no of FOVs per well
    int [][] nFovInWell = new int[nRows][nCols];
    int nImages = 0;
    for (int row = 0; row  < nRows; row++) {
        for (int column = 0; column < nCols; column++) {
          nFovInWell[row][column] = 1;
          nImages++;
        }
    }
    //DEBUG try a zero FOV
    nFovInWell[1][1] = 0;
    // last 3 args are sizeX, sizeY, sizet
    FileExportSPW exporter = new FileExportSPW(fileName);
    boolean ok = exporter.init(nFovInWell, 2, 2, 3);
    byte[] plane;
    if (ok)  {
      for (int i = 0;i < nImages; i++)  {
        for(int t = 0; t < 3; t++)  {
          plane = exporter.createImage(2,2, t, i);
          exporter.export(plane, i , t);
        }
      }
    }
    exporter.cleanup();
  }

}
