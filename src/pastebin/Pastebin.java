package pastebin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import org.jsoup.nodes.Document;
import java.util.Scanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;

/*TODO:
 * 1. Comment code
 * 2. Refactor out to methods
 * 3. Implement object to contain paste URLs
 * 4. Integrate raw-paste cleaning into Paste classes
 * 4. Differentiate between expiring pastes and forever pastes and separate accordingly 
 * 5. Implement scanning queue
 * 6. Implement timer to prevent site lockout
 * 
 * PROSPECTIVE CHANGES TO MAIN: 
 *      -Make ArrayList of VolatilePaste and InvolatilePaste
 *      -
 * 
 * -----------------TODO Classes--------------------------------------
 *  1.  -VolatilePaste class
 *          -properties:
 *              -String: Paste URL
 *              -String: Paste Title
 *              -String: Paste Type
 *              -String: Raw paste url
 *              -Integer: Expiry time
 *              -Long: Expiry time in terms of epoch
 *          -methods
 *              -Getters
 *                  -String - getURL (raw paste URL)
 *                  -String - getType (paste type)
 *                  -String - getTitle (paste Title)
 *                  -Integer - getExpiry
 *                  -Long - getExpiry_Epoch (Get the expiry in terms of the epoch)
 *              -Setters
 *                  -void - setURL (raw paste URL)
 *                  -void - setType (paste type)
 *                  -void - setTitle (paste Title)
 *                  -void - setExpiry
 *                  -void - setExpiry_Epoch (Get the expiry in terms of the epoch)
 *  2.  -InvolatilePaste class
 *          -properties
 *              -String: Paste URL
 *              -String: Paste Title
 *              -String: Paste Type
 *              -String: Raw paste url
 *          -methods
 *              -Getters
 *                  -String - getURL (raw paste URL)
 *                  -String - getType (paste type)
 *                  -String - getTitle (paste Title)
 *              -Setters
 *                  -String - setURL (raw paste URL)
 *                  -String - setType (paste type)
 *                  -String - setTitle (paste Title)
 * --------------------------------------------------------------------
 */

public class Pastebin {

    public static Document doc;
    public static Document temp;
    public static Elements list;
    public static File file;
    public static ArrayList<String> rawHREF = new ArrayList<String>();
    public static ArrayList<String> pasteNames = new ArrayList<String>();
    public static ArrayList<String> linksPartial = new ArrayList<String>();
    public static ArrayList<String> linksPartial2 = new ArrayList<String>();
    public static ArrayList<String> linksTitle = new ArrayList<String>();
    public static ArrayList<String> urls = new ArrayList<String>();
    public static ArrayList<String> pasteName = new ArrayList<String>();
    public static ArrayList<String> pasteType = new ArrayList<String>();
    public static ArrayList<String> pasteNamesOrig = new ArrayList<String>();
    public static int ptime;
    private static int INDEX_START = 10;
    private static int INDEX_STOP = 18;

//pastebin archive has 250 recent paste capacity
    public static void main(String[] args) throws Exception {


        System.out.println("Specify scrape interval (In Minutes)");
        Scanner input1 = new Scanner(System.in);
        ptime = input1.nextInt();

        ptime = ptime * 60000;

        ArrayList<String> pastes = new ArrayList<String>(); //main list of pastes

        
        //Main program logic
        while (true) {

            scanThis("http://pastebin.com/archive/");

            try {
                list = doc.select(".maintable"); //gets element of list of pastes


                Elements filteredList = list.select("[href]"); //Gets the hrefs of pastes from the list
                //System.out.println(filteredList);
                for (Element hrefs : filteredList) { //adds them all to RawHREF
                    rawHREF.add(hrefs.toString());
                }
            } catch (Exception e) {
                System.out.println("Sorry, the site had blocked you");
            }

                                                                                                                           //START PASTE CLEANING
            
            linkLineSort(rawHREF); //separates Paste ID(raw href tags) from Paste Names

            for (String e : linksPartial) {
                linksPartial2.add(e);
            }

            isolateText(INDEX_START, INDEX_STOP);  //splits out the IDs from the raw href tags in linksPartial


            for (String k : linksPartial) {  //makes the final urls
                urls.add("http://pastebin.com/raw.php?i=" + k);
            }

            pasteNamesOrig = (ArrayList<String>) pasteNames.clone(); //pasteNamesOrig is an arraylist that stores all the pasteNames

            //isolate the Titles of the Pastes
            for (int y = 0; y < pasteNames.size(); y++) {
                isolateName(20, y);
            }

            //Isolate types of paste
            for (int y = 0; y < pasteNamesOrig.size(); y++) {
                isolateType(y);
            }

            //Fix each Name to make sure it does not contain invalid characters.
            for (int w = 0; w < pasteName.size(); w++) { 
                String temp;
                temp = pasteName.get(w);
                temp = temp.replace("\\", "_");
                temp = temp.replace("/", "_");
                temp = temp.replace(":", "_");
                temp = temp.replace("*", "_star_");
                temp = temp.replace("?", "_qmark_");
                temp = temp.replace("<", "_leftcaret_");
                temp = temp.replace(">", "_rightcaret_");
                temp = temp.replace("|", "_pipe_");
                pasteName.set(w, temp);
            }
                                                                                                                                //END PASTE CLEANING

            
            //BEGIN SYNTAX DETECTION
            int g = 0;
            int numFiles = 0;
            for (String u : urls) {

                String heuristics = "";
                String tempContents;
                try {
                    temp = Jsoup.connect(u).userAgent("Mozilla").get();
                } catch (Exception e) {
                    System.out.println("Sorry, the site had blocked you");
                }
                tempContents = temp.text();
                if ((tempContents.contains("youtube.com"))) {
                    heuristics = "youtube";
                }
                if ((tempContents.contains("password"))) {
                    heuristics = "Passwords";
                }
                if ((tempContents.contains("<?php") || (tempContents.contains("include")))) {
                    heuristics = "PHP_C++";
                }

                if ((tempContents.matches("using.[;]"))) {
                    //The regex here means "using" at the beginning and ";" at the end (in a given phrase).
                    heuristics = "C++";
                }
                if ((tempContents.contains("public class") || (tempContents.contains("public static void")))) {
                    heuristics = "Java";
                }
                if ((tempContents.contains("elif") || (tempContents.contains("endif")))) {
                    heuristics = "BashScripts";
                }
                if ((tempContents.contains("<html>") || (tempContents.contains("<body>")) || (tempContents.contains("<div>")))) {
                    heuristics = "HTML";
                }
                if ((tempContents.contains("<?xml version"))) {
                    heuristics = "XML";
                }
                if ((tempContents.contains("DEBUG"))) {
                    heuristics = "Debug_Logs";
                }

                if (!(pasteType.get(g).contentEquals("text"))) {
                    heuristics = pasteType.get(g);
                } else {
                    heuristics = "";
                }

                //END SYNTAX DETECTION
                
                

                URL v = new URL(u);
                //File file = new File("L:\\" + pasteType.get(g) + "\\pastebin" + " - " + pasteName.get(g) + ".txt");
                File folder = new File("L:" + File.separator + "pastes" + File.separator);
                numFiles = folder.listFiles().length;

                try {
                    if (!(heuristics.contentEquals(""))) {
                        file = new File("L:" + File.separator + "pastes" + File.separator + heuristics + File.separator + "Pastebin" + " - " + pasteName.get(g) + " - " + linksPartial.get(g) + ".txt");
                    } else {
                        file = new File("L:" + File.separator + "pastes" + File.separator + "Pastebin" + " - " + pasteName.get(g) + " - " + linksPartial.get(g) + ".txt");
                    }

                    heuristics = "";
                    if (!(file.exists())) { //check for overlapping files
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                        FileUtils.copyURLToFile(v, file);
                        System.out.println("Created file for: " + pasteName.get(g));
                    }
                    g++;
                } catch (Exception e) {
                    System.out.println("Sorry, the site had blocked you");
                }
            }

            
            //Clear after the pass.
            rawHREF.clear();
            pasteNames.clear();
            linksPartial.clear();
            linksPartial2.clear();
            linksTitle.clear();
            urls.clear();
            pasteName.clear();
            pasteType.clear();
            pasteNamesOrig.clear();
            System.out.println("Now Pausing for " + ptime/60000 + " Mins....");
            Thread.sleep(ptime);


        }

    }

    private static Document scanThis(String cityNext) {

        try {
            doc = Jsoup.connect(cityNext).userAgent("Mozilla").get();


        } catch (Exception e) {
            System.out.println("Sorry, you'll have to wait some time because the site blocked you");
            System.out.println("Waiting for ...." + "31" + " minutes");
            try {
                Thread.sleep(1900000);
            } catch (Exception f) {
                System.out.println("Time delay failed");
            }
            //System.out.println("Error: " + e);
        }

        return doc;

    }

    public static void writeToFile(String input) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter("L:\\a.txt"));
        out.write(input + "\n");
        out.close();
    }

    public static void linkLineSort(ArrayList<String> arr) throws Exception {

        for (int r = 0; r < arr.size(); r++) {
            if (r % 2 == 0) {
                linksPartial.add(arr.get(r));
            } else {
                pasteNames.add(arr.get(r));
            }
        }

    }

    public static void isolateText(int indx1, int indx2) {

        for (int k = 0; k < linksPartial.size(); k++) {

            linksPartial.set(k, linksPartial.get(k).substring(indx1, indx2));

        }
    }

    public static void isolateType(int k) {

        pasteNamesOrig.set(k, pasteNamesOrig.get(k).substring(18));
        String str = pasteNamesOrig.get(k).replaceAll("[\">][\\w+]*", "");
        str = str.substring(0, (str.length() - 3));
        pasteType.add(str);
        //System.out.println(str);

    }

    public static void isolateName(int indx1, int k) {

        pasteName.add(linksPartial2.get(k).substring(indx1, (linksPartial2.get(k).length() - 4)));



    }
}
