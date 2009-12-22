package org.opentox.Resources.Algorithms;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.UnhandledException;
import org.opentox.Applications.OpenToxApplication;
import org.opentox.Resources.AbstractResource;
import org.opentox.Resources.AbstractResource.Directories;
import org.opentox.Resources.AbstractResource.URIs;
import org.opentox.Resources.ErrorRepresentation;
import org.opentox.Resources.ErrorRepresentationFactory;
import org.opentox.Resources.ErrorSource;
import org.opentox.database.ModelsDB;
import org.opentox.ontology.Dataset;
import org.opentox.ontology.Model;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;

/**
 * Trainer for MLR models.
 * @author OpenTox - http://www.opentox.org/
 * @author Sopasakis Pantelis
 * @author Sarimveis Harry
 * @version 1.3.3 (Last update: Dec 20, 2009)
 */
public class MlrTrainer extends AbstractTrainer {

    protected Instances data;

    public MlrTrainer(Form form, ServerResource resource) {
        super(form, resource);
    }


    /**
     * Returns
     * @return
     */
    @Override
    public synchronized Representation train() {
        Representation representation = null;
        int model_id = 0;
        model_id = ModelsDB.getModelsStack() + 1;


        errorRep = (ErrorRepresentation) checkParameters();

        if (errorRep.errorLevel() > 0) {
            representation = errorRep;
        } else {

            /**
             * Retrive the Dataset (RDF), parse it and generate the corresponding
             * weka.core.Instances object.
             */
            Dataset dataset = new Dataset(dataseturi);
            data = dataset.getWekaDatasetForTraining(null, false);


            try {
                data.setClass(data.attribute(targeturi.toString()));
                Preprocessing.removeStringAtts(data);
                LinearRegression linreg = new LinearRegression();
                String[] linRegOptions = {"-S", "1", "-C"};

                /**
                 * Train the model using Weka...
                 */
                try {

                    linreg.setOptions(linRegOptions);
                    linreg.buildClassifier(data);
                    generatePMML(linreg.coefficients(), model_id);

                    List<AlgorithmParameter> paramList = new ArrayList<AlgorithmParameter>();
                    paramList.add(ConstantParameters.TARGET(targeturi.toString()));

                    /**
                     * Store the model as RDF...
                     */
                    Model model = new Model();
                    model.createModel(Integer.toString(model_id),
                            dataseturi.toString(),
                            targeturi.toString(),
                            data,
                            paramList,
                            URIs.mlrAlgorithmURI,
                            new FileOutputStream(Directories.modelRdfDir + "/" + model_id));


                    // return the URI of generated model:
                    if (model.errorRep.errorLevel()==0) {
                        
                        representation = new StringRepresentation(AbstractResource.URIs.modelURI + "/"
                                + ModelsDB.registerNewModel(
                                AbstractResource.URIs.mlrAlgorithmURI) + "\n");
                    } else {
                        errorRep.append(model.errorRep);
                        Throwable throwable = new UnhandledException(new Throwable());
                    }

                } catch (Exception ex) {
                    OpenToxApplication.opentoxLogger.severe("Severe Error while trying to build an MLR model.\n"
                            + "Details :" + ex.getMessage() + "\n");
                    errorRep.append(ex, "Severe Error while trying to build an MLR model.", Status.SERVER_ERROR_INTERNAL);
                }
            } catch (NullPointerException ex) {
                OpenToxApplication.opentoxLogger.severe("Severe Error while trying to build an MLR model.\n"
                        + "Details :" + ex.getMessage() + "\n");
                errorRep.append(ex, "Probably this exception is thrown "
                        + "because the dataset or target uri you provided is not valid or some other internal"
                        + "server error happened!\n", Status.SERVER_ERROR_INTERNAL);
                System.out.println(errorRep.getStatus());
            } catch (Throwable thr) {
                OpenToxApplication.opentoxLogger.severe("Severe Error while trying to build an MLR model.\n"
                        + "Details :" + thr.getMessage() + "\n");
                errorRep.append(thr, "An unexpected exception or error was thrown while"
                        + "training an MLR model. Please contact the system admnistrator for further information!\n",
                        Status.SERVER_ERROR_INTERNAL);
            }
        }
        
        return errorRep.errorLevel()==0 ? representation : errorRep;
    }

    /**
     * Check wether the dataset and target values are valid URIs. The internal
     * status is 202 (Accepted) if the posted data are accepted, or 400
     * (Client Error, Bad Requested) otherwise.
     * @return Returns null if no error is found (the posted parameters are
     * acceptable), otherwise a representation of the error. The internal status
     * is defined accordingly.
     */
    @Override
    public ErrorRepresentation checkParameters() {
        MediaType errorMediaType = MediaType.TEXT_PLAIN;
        Status clientPostedWrongParametersStatus = Status.CLIENT_ERROR_BAD_REQUEST;
        String errorDetails = "";


        try {
            dataseturi = new URI(form.getFirstValue("dataset_uri"));
        } catch (URISyntaxException ex) {
            errorDetails = "[Wrong Posted Parameter ]: The client did"
                    + " not post a valid URI for the dataset";
            errorRep.append(ex, errorDetails, clientPostedWrongParametersStatus);
        }
        try {
            targeturi = new URI(form.getFirstValue("target"));
        } catch (URISyntaxException ex) {
            errorDetails = "[Wrong Posted Parameter ]: The client did"
                    + " not post a valid URI for the target feature";
            errorRep.append(ex, errorDetails, clientPostedWrongParametersStatus);
        }
        return errorRep;
    }

    /**
     * Generates the PMML representation of the model and stores in the hard
     * disk.
     * @param coefficients The vector of the coefficients of the MLR model.
     * @param model_id The id of the generated model.
     */
    private void generatePMML(double[] coefficients, int model_id) {
        StringBuilder pmml = new StringBuilder();
        pmml.append(AbstractResource.xmlIntro);
        pmml.append(AbstractResource.PMMLIntro);
        pmml.append("<Model ID=\"" + model_id + "\" Name=\"MLR Model\">\n");
        pmml.append("<link href=\"" + AbstractResource.URIs.modelURI + "/" + model_id + "\" />\n");
        pmml.append("<AlgorithmID href=\"" + AbstractResource.URIs.mlrAlgorithmURI + "\"/>\n");
        pmml.append("<DatasetID href=\"" + dataseturi.toString() + "\"/>\n");
        pmml.append("<AlgorithmParameters />\n");
        pmml.append("<FeatureDefinitions>\n");
        for (int k = 1; k <= data.numAttributes(); k++) {
            pmml.append("<link href=\"" + data.attribute(k - 1).name() + "\"/>\n");
        }
        pmml.append("<target index=\"" + data.attribute(targeturi.toString()).index() + "\" name=\""
                + targeturi.toString() + "\"/>\n");
        pmml.append("</FeatureDefinitions>\n");
        pmml.append("<User>Guest</User>\n");
        pmml.append("<Timestamp>" + java.util.GregorianCalendar.getInstance().getTime() + "</Timestamp>\n");
        pmml.append("</Model>\n");

        pmml.append("<DataDictionary numberOfFields=\"" + data.numAttributes() + "\" >\n");
        for (int k = 0; k
                <= data.numAttributes() - 1; k++) {
            pmml.append("<DataField name=\"" + data.attribute(k).name()
                    + "\" optype=\"continuous\" dataType=\"double\" />\n");
        }
        pmml.append("</DataDictionary>\n");
        // RegressionModel
        pmml.append("<RegressionModel modelName=\"" + AbstractResource.URIs.modelURI + "/" + model_id + "\""
                + " functionName=\"regression\""
                + " modelType=\"linearRegression\""
                + " algorithmName=\"linearRegression\""
                + " targetFieldName=\"" + data.attribute(data.numAttributes() - 1).name() + "\""
                + ">\n");
        // RegressionModel::MiningSchema
        pmml.append("<MiningSchema>\n");
        for (int k = 0; k <= data.numAttributes() - 1; k++) {
            if (k != data.classIndex()) {
                pmml.append("<MiningField name=\""
                        + data.attribute(k).name() + "\" />\n");
            }
        }
        pmml.append("<MiningField name=\""
                + data.attribute(data.classIndex()).name() + "\" "
                + "usageType=\"predicted\"/>\n");
        pmml.append("</MiningSchema>\n");
        // RegressionModel::RegressionTable
        pmml.append("<RegressionTable intercept=\"" + coefficients[coefficients.length - 1] + "\">\n");

        for (int k = 0; k
                <= data.numAttributes() - 1; k++) {

            if (!(targeturi.toString().equals(data.attribute(k).name()))) {
                pmml.append("<NumericPredictor name=\""
                        + data.attribute(k).name() + "\" "
                        + " exponent=\"1\" "
                        + "coefficient=\"" + coefficients[k] + "\"/>\n");
            }
        }

        pmml.append("</RegressionTable>\n");
        pmml.append("</RegressionModel>\n");
        pmml.append("</PMML>\n\n");
        try {
            FileWriter fwriter = new FileWriter(AbstractResource.Directories.modelPmmlDir
                    + "/" + model_id);
            BufferedWriter writer = new BufferedWriter(fwriter);
            writer.write(pmml.toString());
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            errorRep.append(ex, "(MLR-Trainer ) Iunput/Output Exception while generating a PMML "
                    + "representation for an MLR model. Probably the destination does not exist.",
                    Status.SERVER_ERROR_INTERNAL);
            Logger.getLogger(MlrTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
