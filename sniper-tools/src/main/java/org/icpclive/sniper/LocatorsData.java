package org.icpclive.sniper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LocatorsData {
    public static List<LocatorCamera> locatorCameras;

//    static {
//        Properties properties = new Properties();
//        try {
//            properties = Config.loadProperties("mainscreen");
////            InputStream resource = org.icpclive.sniper.LocatorsData.class.getResourceAsStream("/mainscreen.properties");
////            if (resource == null) {
////                throw new AssertionError("/mainscreen.properties not found");
////            }
////            properties.load(resource);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String camerasProperty = properties.getProperty("locator.cameras");
//        String locatorCoordinatesProperty = properties.getProperty("locator.coordinates");
//        if (camerasProperty == null) {
//            throw new AssertionError("locator.cameras expected in properties");
//        }
//        if (locatorCoordinatesProperty == null)  {
//            throw new AssertionError("locator.coordinates expected in properties");
//        }
//        String[] cameraIPs = camerasProperty.split(",");
//        String[] locatorCoordinates = locatorCoordinatesProperty.split(",");
//        if (cameraIPs.length != locatorCoordinates.length) {
//            throw new AssertionError("locator.cameras, locator.inputfiles and locator.coordinates must be of the same length");
//        }
//        locatorCameras = new ArrayList<>(cameraIPs.length);
//        for (int i = 0; i < cameraIPs.length; i++) {
//            try {
//                locatorCameras.add(new org.icpclive.sniper.LocatorCamera(cameraIPs[i],
//                        new File(locatorCoordinates[i]), i));
//            } catch (FileNotFoundException e) {
//                throw new AssertionError(e);
//            }
//        }
//    }
}
