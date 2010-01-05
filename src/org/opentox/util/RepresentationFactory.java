package org.opentox.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Use FileRepresentation instead.
 * @author OpenTox - http://www.opentox.org
 * @author Sopasakis Pantelis
 * @author Sarimveis Harry
 * @version 1.1 (Last Update: Aug 18, 2009)
 * @since 1.0
 * @deprecated 
 */
@Deprecated
public class RepresentationFactory {

    private static final long serialVersionUID=100121700078001L;

    private String filename;

    public RepresentationFactory(String filename){
        this.filename=filename;
    }

    public StringBuilder getString() throws FileNotFoundException
    {
        File file = new File(filename);
        StringBuilder builder = new StringBuilder();

        if ((file.exists())&&(file.canRead())){
            FileReader fr;
            try {
                fr = new FileReader(file);
                BufferedReader in = new BufferedReader(fr);
                String line ;
             while ((line = in.readLine()) != null)  {
                    builder.append(line);
                    builder.append("\n");
             }
            } catch (IOException ex) {
                Logger.getLogger(RepresentationFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }else if (!(file.exists())){
            throw new FileNotFoundException("File Not Found!");
        }

        return builder;
    }



}
