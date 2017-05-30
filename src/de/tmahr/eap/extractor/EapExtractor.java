/**
 * @author Thomas Mahr (www.tmahr.de)
 * @version $Rev: 4091 $
 * @date $Date: 2016-12-13 17:41:59 +0100 (Di, 13. Dez 2016) $
 */
package de.tmahr.eap.extractor;

import de.tmahr.eap.model.Modell;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EapExtractor
{
    private static final Logger LOGGER = Logger.getLogger(EapExtractor.class.getName());

    private static Modell importiereModellEap(String fileName)
    {
        Modell modell = EapImportApi.eapAuslesen(fileName);
        return modell;
    }

    private static void serialisiereModell(Modell modell, String dateiName)
    {
        try (FileOutputStream fos = new FileOutputStream(dateiName); ObjectOutputStream oos = new ObjectOutputStream(fos))
        {
            oos.writeObject(modell);
        }
        catch (IOException e)
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LOGGER.severe(sw.toString());
        }
    }

    public static void main(String[] args)
    {
        String dateiName = System.getProperty("user.dir") + "\\..\\..\\";
        //dateiName += "DemoEapVerarbeitung01.eap";
        dateiName += "AmpelSystem.eap";

        //dateiName += "DemoEapVerarbeitungAmpel.eap";
        Modell modell = importiereModellEap(dateiName);
        LOGGER.log(Level.INFO, "\n{0}", modell.toString());
        serialisiereModell(modell, dateiName + ".ser");
    }
}
