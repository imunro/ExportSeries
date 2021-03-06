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
  
  private int sizeT = 3;
  private int rows = 2;
  private int cols = 2;

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
  public FileExportSPW(String outputBase) {
    this.outputBase = outputBase;
  }

  /** Save a single 2x2 uint16 plane of data. */
  public void export() {
    int width = 4, height = 4;
    int pixelType = FormatTools.UINT16;
    Exception exception = null;

   
    
    int series = 0;
    int index = 0;
    int nSeries = rows * cols;
    Path path;
    
    IMetadata [] omexmlSets = initializeMetadata(width, height, pixelType);
    
    while (series < nSeries) {
    
      outputFile = outputBase + Integer.toString(series + 1) + ".ome.tiff";
      path = FileSystems.getDefault().getPath(outputFile);
    
      //delete if exists 
      //NB deleting old files seems to be critical when changing size
      try {
        boolean success = Files.deleteIfExists(path);
        System.out.println("Delete status: " + success);
      } catch (IOException | SecurityException e) {
        System.err.println(e);
      }
    
      IMetadata omexml = omexmlSets[series];
      // only save a plane if the file writer was initialized successfully
      boolean initializationSuccess = initializeWriter(omexml);
      
    
      if (initializationSuccess) {
        System.out.println("saving to ");
        System.out.println(outputFile);
        index = 0;
          for (int p = 0; p < sizeT; p++) {
            savePlane(width, height, pixelType, index, 0);
            index++;
          }
        cleanup();
        series++;
      }  //endif
    }  //endwhile
   
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
  private IMetadata[] initializeMetadata(int width, int height, int pixelType) {
    Exception exception = null;
    
     PositiveInteger pwidth = new PositiveInteger(width);
     PositiveInteger pheight = new PositiveInteger(height);
     
     String plateId = MetadataTools.createLSID("Plate", 0);
     int series = 0;
     int well = 0;
     OMEXMLMetadata [] metaDataSets = new OMEXMLMetadata[4];
     
     String [] imageIDs = new String[4];
     imageIDs[0] = MetadataTools.createLSID("Image:0", 0);
     imageIDs[1] = MetadataTools.createLSID("Image:1", 0);
     imageIDs[2] = MetadataTools.createLSID("Image:2", 0);
     imageIDs[3] = MetadataTools.createLSID("Image:3", 0);
     
     String [] wellIDs = new String[4];
     wellIDs[0] = MetadataTools.createLSID("Well:0", 0);
     wellIDs[1] = MetadataTools.createLSID("Well:1", 0);
     wellIDs[2] = MetadataTools.createLSID("Well:2", 0);
     wellIDs[3] = MetadataTools.createLSID("Well:3", 0);
     
     String [] wellSampleIDs = new String[4];
     wellSampleIDs[0] = MetadataTools.createLSID("WellSample:0", 0);
     wellSampleIDs[1] = MetadataTools.createLSID("WellSample:1", 0);
     wellSampleIDs[2] = MetadataTools.createLSID("WellSample:2", 0);
     wellSampleIDs[3] = MetadataTools.createLSID("WellSample:3", 0);
     
     String suffixStr;
     int plateIndex = 0;
     int sampleIndex = 0;
    
    try {
      
       ServiceFactory factory = new ServiceFactory();
       OMEXMLService service = factory.getInstance(OMEXMLService.class);
      
       for (int row = 0; row  < rows; row++) {
        for (int column = 0; column < cols; column++) {
          
          // create the OME-XML metadata storage object
          
          OMEXMLMetadata meta = service.createOMEXMLMetadata();
          //IMetadata meta = service.createOMEXMLMetadata();
          meta.createRoot();


          // Create Minimal 2x2 Plate 
          meta.setPlateID(plateId, 0);

          meta.setPlateRowNamingConvention(NamingConvention.LETTER, 0);
          meta.setPlateColumnNamingConvention(NamingConvention.NUMBER, 0);

          meta.setPlateRows(new PositiveInteger(rows), 0);
          meta.setPlateColumns(new PositiveInteger(cols), 0);
          meta.setPlateName("First test Plate", 0);
     
       
          suffixStr = Integer.toString(well);
          
         // Create Image
          
         
          String imageID = imageIDs[well];
          
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
          meta.setPixelsSizeT(new PositiveInteger(sizeT), series);

          // define each channel and specify the number of samples in the channel
          // the number of samples is 3 for RGB images and 1 otherwise
          meta.setChannelID("Channel:0:"+suffixStr, series,0 );
          meta.setChannelSamplesPerPixel(new PositiveInteger(1), series, 0);
          
          
          // create a well for each row & column within each of the metadata blocs
          int inwell = 0;
          
          for (int inrow = 0; inrow  < rows; inrow++) {
            for (int incolumn = 0; incolumn < cols; incolumn++) {
          
              String wellID = wellIDs[inwell];
              String inImageID = imageIDs[inwell];
              // set up wells
              meta.setWellID(wellID, plateIndex, inwell);
              meta.setWellRow(new NonNegativeInteger(inrow), plateIndex, inwell);
              meta.setWellColumn(new NonNegativeInteger(incolumn), plateIndex, inwell);

              // one sample per well
              String wellSampleID = wellSampleIDs[inwell];
              meta.setWellSampleID(wellSampleID, 0, inwell, sampleIndex);
              meta.setWellSampleIndex(new NonNegativeInteger(inwell), 0, inwell, sampleIndex);
              //void setWellSampleImageRef(String image,int plateIndex, int wellIndex, int wellSampleIndex)
              meta.setWellSampleImageRef(inImageID, 0, inwell, sampleIndex);

              inwell++;
            }
          }
    
            

          // add FLIM ModuloAlongT annotation if required 
          CoreMetadata modlo = createModuloAnn(meta);
          service.addModuloAlong(meta, modlo, series);
          
           metaDataSets[well] = meta;
           
          well++;
      
        }
      }
      
      //String dump = meta.dumpXML();
      //System.out.println("dump = ");
      //System.out.println(dump);
      return metaDataSets;
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
  private void savePlane(int width, int height, int pixelType, int index, int series ) {
    byte[] plane = createImage(width, height, pixelType, index, series);
    
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
  private byte[] createImage(int width, int height, int pixelType, int index, int series) {
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
    String baseName  = "SPWFromJava";
    
    FileExportSPW exporter = new FileExportSPW(baseName);
    exporter.export();
  }

}
