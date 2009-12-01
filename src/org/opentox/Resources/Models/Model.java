package org.opentox.Resources.Models;

import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.Resources.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentox.Applications.OpenToxApplication;
import org.opentox.Resources.Algorithms.Preprocessing;
import org.opentox.client.opentoxClient;
import org.opentox.database.InHouseDB;
import org.opentox.util.RepresentationFactory;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;

/**
 * GET the representation of a model given its id. The corresponding URIs have
 * the structure: http://opentox.ntua.gr:3000/OpenToxServices/models/classification/{algorithm_id}/{model_id} and
 * http://opentox.ntua.gr:3000/OpenToxServices/models/classification/{algorithm_id}/{model_id}
 * where algorithm_id in case of classification is one of svm, knn, j48 or pls and in case of
 * regression is one of mlr, pls or svm.
 *
 * @author OpenTox - http://www.opentox.org/
 * @author Sopasakis Pantelis
 * @author Sarimveis Harry
 * @version 1.3 (Last update: Aug 27, 2009)
 * @since 1.1
 *
 */
public class Model extends AbstractResource {

    private static final long serialVersionUID = 10012190007003005L;
    private String model_id, algorithm_id, model_type;

    /**
     * Default Class Constructor.Available MediaTypes of Variants: TEXT_XML
     * @param context
     * @param request
     * @param response
     */
    @Override
    public void doInit() throws ResourceException {
        super.doInit();
        Collection<Method> allowedMethods = new ArrayList<Method>();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.POST);
        getAllowedMethods().addAll(allowedMethods);
        super.doInit();
        List<Variant> variants = new ArrayList<Variant>();
        variants.add(new Variant(MediaType.TEXT_PLAIN));
        variants.add(new Variant(MediaType.TEXT_XML));
        /** Sometime we will support HTML representation for models **/
        //variants.add(new Variant(MediaType.TEXT_HTML));
        getVariants().put(Method.GET, variants);
        model_id = Reference.decode(getRequest().getAttributes().get("model_id").toString());
    }

    /**
     *
     * @param variant
     * @return StringRepresentation
     */
    @Override
    public Representation get(Variant variant) {
        File modelXmlFile = new File(Directories.modelXmlDir + "/" + model_id);
        System.out.println("Requested Model id: " + model_id);
        if (modelXmlFile.exists()) {
            RepresentationFactory model = new RepresentationFactory(modelXmlFile.getAbsolutePath());
            try {
                getResponse().setStatus(Status.SUCCESS_OK);
                return new StringRepresentation(model.getString().toString(), MediaType.TEXT_XML);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                return new StringRepresentation("Model not found! Exception Details: " + ex.getMessage());
            } catch (IOException ex) {
                Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                return new StringRepresentation("IO Exception! Details: " + ex.getMessage());
            }
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new StringRepresentation("Model not found!");
        }
    }

    @Override
    public Representation post(Representation entity) {
        Representation result = null;
        /** Get the Instances from the dataset URI posted **/
        Form form = new Form(entity);
        try {
            URI datasetURI = new URI(form.getFirstValue("dataset"));
            Instances testData = opentoxClient.getInstances(datasetURI);
            Preprocessing.removeStringAtts(testData);

            /**** MLR ****/
            if (InHouseDB.isModel(model_id, "mlr")) {
                try {
                    PMMLModel mlrModel = PMMLFactory.getPMMLModel(new File(Directories.modelXmlDir + "/" + model_id));
                    if (mlrModel instanceof PMMLClassifier) {
                        Attribute att = testData.attribute(mlrModel.getMiningSchema().getFieldsAsInstances().classAttribute().name());
                        testData.setClass(att);
                        PMMLClassifier classifier = (PMMLClassifier)mlrModel;                        
                        String predictions = "";
                        for (int i=0;i<testData.numInstances();i++){
                            predictions = predictions + classifier.classifyInstance(testData.instance(i))+"\n";
                        }
                        result = new StringRepresentation(predictions, MediaType.TEXT_PLAIN);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
            }



        } catch (URISyntaxException ex) {
            Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
        }


        return result;
    }

    @Override
    public Representation delete() {
        String responseText = null;
        try {
            if (opentoxClient.IsMimeAvailable(new URI("http://localhost:3000/model/" + model_id),
                    MediaType.TEXT_XML, false)) {
                OpenToxApplication.dbcon.removeModel(model_id);
                File modelFile = new File(Directories.modelXmlDir + "/" + model_id);
                System.out.println(modelFile);
                responseText = "The resource was detected and removed from OT database successfully!";
                if (modelFile.exists()) {
                    boolean success = modelFile.renameTo(new File(Directories.trash, modelFile.getName()));
                    if (success) {
                        OpenToxApplication.opentoxLogger.severe("Model : " + model_id + " moved to trash!");
                    }
                } else {
                    OpenToxApplication.opentoxLogger.severe("Model File not found! Will not apply DELETE!");
                }
            } else {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                responseText = "Model not found on the server!";
            }
        } catch (URISyntaxException ex) {
            responseText = "Model Not Found!";
            OpenToxApplication.opentoxLogger.severe("Model URI : http://localhost:3000/model/" + model_id +
                    "seems to be invalid!");
        }
        return new StringRepresentation(responseText + "\n");
    }
}// End of class

