/**
 * @author Thomas Mahr (www.tmahr.de)
 * @version $Rev: 4083 $
 * @date $Date: 2016-12-06 21:38:34 +0100 (Di, 06. Dez 2016) $
 */
package de.tmahr.eap.extractor;

import de.tmahr.eap.model.Attribut;
import de.tmahr.eap.model.Paket;
import de.tmahr.eap.model.Element;
import de.tmahr.eap.model.Modell;
import de.tmahr.eap.model.Methode;
import de.tmahr.eap.model.ModellErbauer;
import de.tmahr.eap.model.Stereotyp;
import de.tmahr.eap.model.Stereotypen;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EapImportApi {

    private static final Logger logger = Logger.getLogger(EapImportApi.class.getName());

    public static Modell eapAuslesen(String dateiName) {
        Modell modell = null;

        logger.info("new org.sparx.Repository()");
        org.sparx.Repository sparxRepository = new org.sparx.Repository();

        logger.log(Level.INFO, "org.sparx.Repository().OpenFile({0})", dateiName);
        if (!sparxRepository.OpenFile(dateiName)) {
            logger.log(Level.WARNING, "Kann Datei \"{0}\" nicht \u00f6ffnen.", dateiName);
        } else {
            ModellErbauer modellErbauer = new ModellErbauer();
            repositoryAuslesen(modellErbauer, sparxRepository);
            sparxRepository.CloseFile();
            modell = modellErbauer.erstellen();
        }

        sparxRepository.CloseAddins();
        sparxRepository.Exit();
        sparxRepository.destroy();

        return modell;
    }

    private static void repositoryAuslesen(ModellErbauer modellErbauer, org.sparx.Repository sparxRepository) {
        for (org.sparx.Package sparxPackage : sparxRepository.GetModels()) {
            paketAuslesen(modellErbauer, null, sparxPackage);
        }
    }

    private static void paketAuslesen(ModellErbauer modellErbauer, Paket elternPaket, org.sparx.Package sparxPackage) {
        String packageName = sparxPackage.GetName();
        int packageId = sparxPackage.GetPackageID();
        logger.fine("Read package: " + packageName + ", id=" + packageId);

        Stereotypen stereotypen = erstelleStereotypen(sparxPackage.GetStereotypeEx());
        Paket paket = modellErbauer.erstellePaket(packageName, packageId, elternPaket, stereotypen);
        elementeAuslesen(modellErbauer, paket, sparxPackage);

        for (org.sparx.Package sparxSubpackage : sparxPackage.GetPackages()) {
            paketAuslesen(modellErbauer, paket, sparxSubpackage);
        }
    }

    private static void elementeAuslesen(ModellErbauer modellErbauer, Paket paket, org.sparx.Package sparxPackage) {
        for (org.sparx.Element sparxElement : sparxPackage.GetElements()) {
            String elementName = sparxElement.GetName();
            int elementId = sparxElement.GetElementID();
            String elementTyp = sparxElement.GetType();
            Element element;

            Stereotypen stereotypen = erstelleStereotypen(sparxElement.GetStereotypeEx());
            element = modellErbauer.erstelleElement(elementName, elementId, elementTyp, paket, stereotypen);
            elementeAuslesen(modellErbauer, element, sparxElement);
            verbindungenAuslesen(modellErbauer, sparxElement);
            if(element.typ() == "Interface" || element.typ() == "Class")
            {
                methodenAuslesen(modellErbauer, element, sparxElement);
            }
            if(element.typ() == "Class")
            {
                attributeAuslesen(modellErbauer, element, sparxElement);
            }
        }
    }

    private static void elementeAuslesen(ModellErbauer modellErbauer, Element elternElement, org.sparx.Element sparxElternElement) {
        for (org.sparx.Element sparxElement : sparxElternElement.GetElements()) {
            String elementName = sparxElement.GetName();
            int elementId = sparxElement.GetElementID();
            String elementTyp = sparxElement.GetType();

            Stereotypen stereotypen = erstelleStereotypen(sparxElement.GetStereotypeEx());
            Element element = modellErbauer.erstelleElement(elementName, elementId, elementTyp, elternElement, stereotypen);
            elementeAuslesen(modellErbauer, element, sparxElement);
            verbindungenAuslesen(modellErbauer, sparxElement);
            if(element.typ() == "Interface" || element.typ() == "Class")
            {
                methodenAuslesen(modellErbauer, element, sparxElement);
            }
            if(element.typ() == "Class")
            {
                attributeAuslesen(modellErbauer, element, sparxElement);
            }
        }
    }
    
    private static void methodenAuslesen(ModellErbauer modellErbauer, Element element, org.sparx.Element sparxElement)
    {
        for(org.sparx.Method sparxMethod : sparxElement.GetMethods())
        {
            String returnType = sparxMethod.GetReturnType();
            String methodName = sparxMethod.GetName();
            int methodId = sparxMethod.GetMethodID();
            String visibility = sparxMethod.GetVisibility();
            boolean isQuery = sparxMethod.GetIsQuery();
            boolean isLeaf = sparxMethod.GetIsLeaf();
            boolean isStatic = sparxMethod.GetIsStatic();
            boolean isPure = sparxMethod.GetIsPure();
            boolean isConst = sparxMethod.GetIsConst();
            boolean isAbstract = sparxMethod.GetAbstract();
            logger.fine("Read Method: " + methodName + ", id=" + methodId);
            Stereotypen stereotypen = erstelleStereotypen(sparxMethod.GetStereotypeEx());
            //Erstelle Methode und fuege sie dem Element hinzu
            Methode methode = modellErbauer.erstelleMethode(methodId, returnType, methodName, visibility, isQuery, isLeaf, isStatic, isPure, isConst, isAbstract, element, stereotypen);
            //fuer Methode parameterAuslesen
            parameterAuslesen(modellErbauer, methode, sparxMethod);
        }
    }
    
    private static void parameterAuslesen(ModellErbauer modellErbauer,Methode methode, org.sparx.Method sparxMethode)
    {
        for(org.sparx.Parameter sparxParameter : sparxMethode.GetParameters())
        {
            String parameterName = sparxParameter.GetName();
            String parameterType = sparxParameter.GetType();
            String parameterKind = sparxParameter.GetKind();
            boolean parameterIsConst = sparxParameter.GetIsConst();
            logger.fine("Read Method: " + parameterName);
            Stereotypen stereotypen = erstelleStereotypen(sparxParameter.GetStereotypeEx());
            //Erstelle Parameter und fuege ihn der Methode hinzu
            modellErbauer.erstelleParameter(parameterName, parameterType, parameterKind, parameterIsConst, methode, stereotypen);
        }
    }
    
    private static void attributeAuslesen(ModellErbauer modellErbauer, Element element, org.sparx.Element sparxElement)
    {
        for(org.sparx.Attribute a : sparxElement.GetAttributes())
        {
            int id = a.GetAttributeID();
            String name = a.GetName();
            String typ = a.GetType();
            String visibility = a.GetVisibility();
            String Default = a.GetDefault();
            boolean isStatic = a.GetIsStatic();
            boolean isConst = a.GetIsConst();
            logger.fine("Read Method: " + name + ", id=" + id);
            Stereotypen stereotypen = erstelleStereotypen(a.GetStereotypeEx());
            //Erstelle Attribut und fuege es der Klasse hinzu
            modellErbauer.erstelleAttribut(name, id, typ, visibility, isStatic, isConst, Default, element, stereotypen);
        }
    }

    private static void verbindungenAuslesen(ModellErbauer modellErbauer, org.sparx.Element sparxEelement) {
        for (org.sparx.Connector sparxConnector : sparxEelement.GetConnectors()) {
            verbindungAuslesen(modellErbauer, sparxConnector);
        }
    }

    private static void verbindungAuslesen(ModellErbauer modellErbauer, org.sparx.Connector connector) {
        String connectorType = connector.GetType();
        int clientId = connector.GetClientID();
        int supplierId = connector.GetSupplierID();
        String transitionAction = connector.GetTransitionAction();
        String transitionEvent = connector.GetTransitionEvent();
        String transitionGuard = connector.GetTransitionGuard();

        Stereotypen stereotypes = new Stereotypen();
        stereotypenHinzufuegen(stereotypes, connector.GetStereotypeEx());

        logger.fine("Connector type=" + connectorType + ", clientId=" + clientId + ", supplierId=" + supplierId);
        modellErbauer.erstelleVerbindung(connectorType, clientId, supplierId, stereotypes, transitionAction, transitionEvent, transitionGuard);
    }

    private static void stereotypenHinzufuegen(Stereotypen stereotypeable, String csv) {
        List<String> liste = Arrays.asList(csv.split("\\s*,\\s*"));
        for (String s : liste) {
            if (s.length() > 0) {
                stereotypeable.hinzufuegenStereotyp(new Stereotyp(s));
            }
        }
    }

    private static Stereotypen erstelleStereotypen(String csv) {
        Stereotypen stereotypen = new Stereotypen();
        List<String> liste = Arrays.asList(csv.split("\\s*,\\s*"));
        for (String s : liste) {
            if (s.length() > 0) {
                stereotypen.hinzufuegenStereotyp(new Stereotyp(s));
            }
        }
        return stereotypen;
    }
}
